/*
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2026 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Commercial licensing options are available for organizations wishing
 * to use HexaGlue under terms different from the MPL 2.0.
 * Contact: info@hexaglue.io
 */

package io.hexaglue.core.classification.secondary;

import io.hexaglue.arch.model.core.TypeInfo;
import io.hexaglue.spi.classification.ClassificationContext;
import io.hexaglue.spi.classification.ClassificationException;
import io.hexaglue.spi.classification.HexaglueClassifier;
import io.hexaglue.spi.classification.PrimaryClassificationResult;
import io.hexaglue.spi.classification.SecondaryClassificationResult;
import io.hexaglue.spi.plugin.DiagnosticReporter;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Executes secondary classifiers with timeout protection.
 *
 * <p>This executor provides a safe environment for running user-provided secondary
 * classifiers by:
 * <ul>
 *   <li>Enforcing the timeout specified by {@link HexaglueClassifier#timeout()}</li>
 *   <li>Catching and logging exceptions without failing the classification process</li>
 *   <li>Isolating classifiers in separate threads to prevent interference</li>
 *   <li>Reporting diagnostic information about classifier execution</li>
 * </ul>
 *
 * <h2>Execution Model</h2>
 * <p>Each secondary classifier runs in its own thread. If the classifier:
 * <ul>
 *   <li><b>Completes successfully</b>: Result is returned</li>
 *   <li><b>Times out</b>: Execution is cancelled, warning is logged, primary result used</li>
 *   <li><b>Throws exception</b>: Exception is logged, primary result used</li>
 *   <li><b>Returns null</b>: Signals to use primary result (normal flow)</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>This executor is thread-safe and can be shared across multiple classification
 * operations. It maintains a thread pool for executing classifiers concurrently.
 *
 * <h2>Lifecycle</h2>
 * <p>The executor should be shut down when no longer needed by calling {@link #shutdown()}.
 * This ensures clean termination of the thread pool.
 *
 * @since 3.0.0
 */
public class SecondaryClassifierExecutor {

    private final ExecutorService executorService;
    private final DiagnosticReporter diagnostics;

    /**
     * Creates a new executor with a cached thread pool.
     *
     * @param diagnostics the diagnostic reporter for logging
     */
    public SecondaryClassifierExecutor(DiagnosticReporter diagnostics) {
        this.executorService = Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("hexaglue-secondary-classifier");
            thread.setDaemon(true);
            return thread;
        });
        this.diagnostics = diagnostics;
    }

    /**
     * Executes a secondary classifier with timeout protection.
     *
     * <p>This method:
     * <ol>
     *   <li>Submits the classifier to the thread pool</li>
     *   <li>Waits for up to {@link HexaglueClassifier#timeout()} for completion</li>
     *   <li>Returns the result if successful</li>
     *   <li>Returns empty if timeout, exception, or null result</li>
     * </ol>
     *
     * <p><b>Return value semantics:</b>
     * <ul>
     *   <li>{@code Optional.of(result)} - Secondary classifier produced a result</li>
     *   <li>{@code Optional.empty()} - Use primary result (timeout, exception, or null)</li>
     * </ul>
     *
     * @param classifier    the secondary classifier to execute
     * @param type          the type to classify
     * @param context       the classification context
     * @param primaryResult the primary classification result
     * @return the secondary classification result, or empty to use primary
     */
    public Optional<SecondaryClassificationResult> executeWithTimeout(
            HexaglueClassifier classifier,
            TypeInfo type,
            ClassificationContext context,
            Optional<PrimaryClassificationResult> primaryResult) {

        Duration timeout = classifier.timeout();
        String classifierId = classifier.id();
        String typeName = type.simpleName();

        Future<SecondaryClassificationResult> future =
                executorService.submit(() -> executeClassifier(classifier, type, context, primaryResult));

        try {
            SecondaryClassificationResult result = future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);

            if (result == null) {
                // Null signals to use primary result
                diagnostics.info(
                        String.format("Secondary classifier '%s' delegated to primary for %s", classifierId, typeName));
                return Optional.empty();
            }

            diagnostics.info(String.format(
                    "Secondary classifier '%s' succeeded for %s (kind=%s, certainty=%s)",
                    classifierId, typeName, result.kind(), result.certainty()));

            return Optional.of(result);

        } catch (TimeoutException e) {
            future.cancel(true);
            diagnostics.warn(String.format(
                    "Secondary classifier '%s' timed out after %s for %s", classifierId, timeout, typeName));
            return Optional.empty();

        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            diagnostics.error(String.format(
                    "Secondary classifier '%s' failed for %s: %s", classifierId, typeName, cause.getMessage()));
            return Optional.empty();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            diagnostics.warn(String.format("Secondary classifier '%s' was interrupted for %s", classifierId, typeName));
            return Optional.empty();
        }
    }

    /**
     * Executes the classifier and handles exceptions.
     *
     * @param classifier    the classifier to execute
     * @param type          the type to classify
     * @param context       the classification context
     * @param primaryResult the primary result
     * @return the classification result
     * @throws ClassificationException if classification fails critically
     */
    private SecondaryClassificationResult executeClassifier(
            HexaglueClassifier classifier,
            TypeInfo type,
            ClassificationContext context,
            Optional<PrimaryClassificationResult> primaryResult) {

        try {
            return classifier.classify(type, context, primaryResult);

        } catch (ClassificationException e) {
            // Rethrow to be caught by ExecutionException handler
            throw e;

        } catch (Exception e) {
            // Wrap unexpected exceptions
            throw new ClassificationException(String.format("Unexpected error in classifier '%s'", classifier.id()), e);
        }
    }

    /**
     * Shuts down the executor and waits for termination.
     *
     * <p>This method:
     * <ol>
     *   <li>Initiates an orderly shutdown</li>
     *   <li>Waits up to 5 seconds for pending tasks to complete</li>
     *   <li>Forces shutdown if timeout is exceeded</li>
     * </ol>
     *
     * <p>Should be called when the executor is no longer needed to ensure
     * clean resource cleanup.
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Returns true if the executor has been shut down.
     *
     * @return true if shutdown was initiated
     */
    public boolean isShutdown() {
        return executorService.isShutdown();
    }

    /**
     * Returns true if all tasks have completed after shutdown.
     *
     * @return true if all tasks completed
     */
    public boolean isTerminated() {
        return executorService.isTerminated();
    }
}

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

package io.hexaglue.core.frontend.spoon;

import io.hexaglue.core.frontend.JavaFrontend;
import io.hexaglue.core.frontend.JavaSemanticModel;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.Launcher;
import spoon.compiler.Environment;
import spoon.reflect.CtModel;

/**
 * Spoon-based implementation of {@link JavaFrontend}.
 *
 * <p>Configures Spoon to parse Java source files and produce a
 * {@link JavaSemanticModel} that abstracts away Spoon-specific types.
 */
public final class SpoonFrontend implements JavaFrontend {

    private static final Logger LOG = LoggerFactory.getLogger(SpoonFrontend.class);

    @Override
    public JavaSemanticModel build(JavaAnalysisInput input) {
        LOG.info("Building Spoon model for base package: {}", input.basePackage());

        Launcher launcher = new Launcher();
        configureEnvironment(launcher.getEnvironment(), input.javaVersion(), input.tolerantResolution());

        // Add source roots
        for (Path root : input.sourceRoots()) {
            LOG.debug("Adding source root: {}", root);
            launcher.addInputResource(root.toAbsolutePath().toString());
        }

        // Configure classpath
        if (!input.classpathEntries().isEmpty()) {
            String[] classpath = input.classpathEntries().stream()
                    .map(Path::toAbsolutePath)
                    .map(Path::toString)
                    .toArray(String[]::new);
            launcher.getEnvironment().setSourceClasspath(classpath);
            LOG.debug("Classpath configured with {} entries", classpath.length);
        }

        // Build the model
        CtModel ctModel = launcher.buildModel();
        LOG.info("Spoon model built: {} types", ctModel.getAllTypes().size());

        return new SpoonSemanticModel(ctModel, input.basePackage(), input.includeGenerated());
    }

    private void configureEnvironment(Environment env, int javaVersion, boolean tolerant) {
        env.setNoClasspath(tolerant); // false = strict resolution, true = tolerant (unresolved types accepted)
        env.setAutoImports(false); // Don't rewrite imports
        env.setCommentEnabled(true); // Required for Javadoc extraction
        env.setComplianceLevel(javaVersion);
        env.setIgnoreDuplicateDeclarations(true);
        env.setShouldCompile(false); // We only need the model, not bytecode

        if (tolerant) {
            LOG.info("Spoon configured in tolerant mode (noClasspath=true): unresolved types will be accepted");
        }
    }
}

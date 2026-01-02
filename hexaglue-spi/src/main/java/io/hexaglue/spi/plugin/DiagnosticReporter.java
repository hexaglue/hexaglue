package io.hexaglue.spi.plugin;

/**
 * Reports diagnostics (info, warnings, errors) during plugin execution.
 *
 * <p>Diagnostics are collected and displayed to the user after execution.
 */
public interface DiagnosticReporter {

    /**
     * Reports an informational message.
     *
     * @param message the message
     */
    void info(String message);

    /**
     * Reports a warning.
     *
     * @param message the warning message
     */
    void warn(String message);

    /**
     * Reports an error.
     *
     * <p>Errors do not stop execution but are collected and reported at the end.
     *
     * @param message the error message
     */
    void error(String message);

    /**
     * Reports an error with an associated exception.
     *
     * @param message the error message
     * @param cause the underlying exception
     */
    void error(String message, Throwable cause);
}

package io.hexaglue.core.plugin;

/**
 * A diagnostic message from plugin execution.
 *
 * @param severity the severity level
 * @param message the diagnostic message
 * @param cause the underlying exception, if any
 */
public record PluginDiagnostic(Severity severity, String message, Throwable cause) {

    public enum Severity {
        INFO,
        WARNING,
        ERROR
    }

    public boolean isError() {
        return severity == Severity.ERROR;
    }
}

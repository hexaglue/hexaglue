package io.hexaglue.spi.ir;

/**
 * Reference to a source location for diagnostics.
 *
 * @param filePath the file path (relative or absolute)
 * @param lineStart the starting line number (1-based)
 * @param lineEnd the ending line number (1-based)
 */
public record SourceRef(String filePath, int lineStart, int lineEnd) {

    private static final SourceRef UNKNOWN = new SourceRef("<unknown>", 0, 0);

    /**
     * Returns a source reference for unknown/unavailable source location.
     * Use this for synthetic types or when source information is not available.
     */
    public static SourceRef unknown() {
        return UNKNOWN;
    }

    /**
     * Returns a source reference for synthetic/generated elements.
     *
     * @param description a short description of the synthetic source
     */
    public static SourceRef synthetic(String description) {
        return new SourceRef("<synthetic:" + description + ">", 0, 0);
    }

    /**
     * Returns a source reference for a single line.
     */
    public static SourceRef ofLine(String filePath, int line) {
        return new SourceRef(filePath, line, line);
    }

    /**
     * Returns true if this source reference represents an unknown or synthetic location.
     */
    public boolean isUnknown() {
        return this == UNKNOWN || filePath.startsWith("<");
    }

    /**
     * Returns true if this source reference represents a real source location.
     */
    public boolean isReal() {
        return !isUnknown();
    }
}

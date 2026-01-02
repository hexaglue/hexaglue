package io.hexaglue.core.frontend;

import java.util.Optional;

/**
 * Reference to a source code location.
 *
 * @param filePath the file path (may be relative or absolute)
 * @param lineStart the starting line number (1-based)
 * @param lineEnd the ending line number (1-based)
 */
public record SourceRef(String filePath, int lineStart, int lineEnd) {

    /**
     * Creates a source reference for a single line.
     */
    public static SourceRef ofLine(String filePath, int line) {
        return new SourceRef(filePath, line, line);
    }

    /**
     * Returns the file name without path.
     */
    public String fileName() {
        int lastSlash = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
        return lastSlash < 0 ? filePath : filePath.substring(lastSlash + 1);
    }

    /**
     * Converts to SPI SourceRef.
     */
    public io.hexaglue.spi.ir.SourceRef toSpi() {
        return new io.hexaglue.spi.ir.SourceRef(filePath, lineStart, lineEnd);
    }

    /**
     * Converts an optional to SPI.
     */
    public static io.hexaglue.spi.ir.SourceRef toSpi(Optional<SourceRef> ref) {
        return ref.map(r -> r.toSpi()).orElse(null);
    }
}

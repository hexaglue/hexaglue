package io.hexaglue.core.frontend;

import java.util.Optional;

/**
 * A Java element with source location information.
 */
public interface JavaSourced {

    /**
     * Returns the source location, if available.
     */
    Optional<SourceRef> sourceRef();
}

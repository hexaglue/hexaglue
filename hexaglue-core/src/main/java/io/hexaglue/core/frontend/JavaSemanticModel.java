package io.hexaglue.core.frontend;

import java.util.stream.Stream;

/**
 * Abstraction over the Java source code being analyzed.
 *
 * <p>This interface provides access to all types in the analysis scope,
 * independent of the underlying implementation (Spoon, JavaParser, etc.).
 *
 * <p>Implementations must ensure:
 * <ul>
 *   <li>Types are returned in deterministic order (by qualified name)</li>
 *   <li>Only types within the configured base package are included</li>
 *   <li>Type resolution is complete (classpath properly configured)</li>
 * </ul>
 */
public interface JavaSemanticModel {

    /**
     * Returns all types in the analysis scope.
     *
     * <p>This includes classes, records, interfaces, enums, and annotation types.
     * Types are filtered by the configured base package.
     *
     * @return stream of all types, ordered by qualified name
     */
    Stream<JavaType> types();
}

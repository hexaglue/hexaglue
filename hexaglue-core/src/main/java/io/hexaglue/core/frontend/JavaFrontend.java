package io.hexaglue.core.frontend;

import java.nio.file.Path;
import java.util.List;

/**
 * Factory for creating {@link JavaSemanticModel} instances.
 */
public interface JavaFrontend {

    /**
     * Builds a semantic model from the given input.
     *
     * @param input the analysis input configuration
     * @return the semantic model
     */
    JavaSemanticModel build(JavaAnalysisInput input);

    /**
     * Input configuration for Java analysis.
     *
     * @param sourceRoots directories containing Java source files
     * @param classpathEntries classpath entries for type resolution
     * @param javaVersion the Java version (e.g., 17)
     * @param basePackage the base package to filter types (null for no filter)
     */
    record JavaAnalysisInput(List<Path> sourceRoots, List<Path> classpathEntries, int javaVersion, String basePackage) {

        /**
         * Creates a minimal input for testing.
         */
        public static JavaAnalysisInput minimal(Path sourceRoot, String basePackage) {
            return new JavaAnalysisInput(List.of(sourceRoot), List.of(), 17, basePackage);
        }
    }
}

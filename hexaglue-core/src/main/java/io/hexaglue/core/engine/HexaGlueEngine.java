package io.hexaglue.core.engine;

/**
 * Main entry point for HexaGlue analysis.
 *
 * <p>The engine analyzes Java source code and produces an {@link EngineResult}
 * containing the intermediate representation (IR) and diagnostics.
 *
 * <p>Usage:
 * <pre>{@code
 * HexaGlueEngine engine = HexaGlueEngine.create();
 * EngineConfig config = new EngineConfig(
 *     List.of(Path.of("src/main/java")),
 *     List.of(Path.of("target/classes")),
 *     21,
 *     "com.example",
 *     Map.of()
 * );
 * EngineResult result = engine.analyze(config);
 * }</pre>
 */
public interface HexaGlueEngine {

    /**
     * Analyzes the source code according to the configuration.
     *
     * @param config the analysis configuration
     * @return the analysis result containing IR and diagnostics
     */
    EngineResult analyze(EngineConfig config);

    /**
     * Creates a new engine instance with default settings.
     *
     * @return a new engine instance
     */
    static HexaGlueEngine create() {
        return new DefaultHexaGlueEngine();
    }
}

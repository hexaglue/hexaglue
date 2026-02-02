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
 * EngineConfig config = EngineConfig.minimal(Path.of("src/main/java"), "com.example");
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
        return DefaultHexaGlueEngine.withDefaults();
    }
}

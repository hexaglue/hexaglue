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

package io.hexaglue.plugin.jpa.codegen;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.JavaFile;
import io.hexaglue.plugin.jpa.model.RepositorySpec;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Integration test to verify the complete repository generation pipeline.
 *
 * <p>This test demonstrates what the generated repository code looks like
 * and serves as a visual validation of the code generation output.
 *
 * @since 2.0.0
 */
class RepositoryGenerationIntegrationTest {

    @Test
    void demonstrateRepositoryGeneration() {
        // Given - A repository specification for an Order aggregate
        RepositorySpec spec = new RepositorySpec(
                "com.example.infrastructure.jpa",
                "OrderRepository",
                ClassName.get("com.example.infrastructure.jpa", "OrderEntity"),
                ClassName.get(UUID.class),
                "com.example.domain.Order");

        // When - Generate the repository interface
        JavaFile javaFile = JpaRepositoryCodegen.generateFile(spec);

        // Then - Print the generated code for visual inspection
        System.out.println("=".repeat(80));
        System.out.println("Generated Repository Interface:");
        System.out.println("=".repeat(80));
        System.out.println(javaFile);
        System.out.println("=".repeat(80));
    }
}

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

package io.hexaglue.plugin.livingdoc.generator;

import static io.hexaglue.plugin.livingdoc.V5TestModelBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.ProjectContext;
import io.hexaglue.arch.model.AggregateRoot;
import io.hexaglue.arch.model.TypeId;
import io.hexaglue.arch.model.index.ModuleDescriptor;
import io.hexaglue.arch.model.index.ModuleIndex;
import io.hexaglue.arch.model.index.ModuleRole;
import io.hexaglue.plugin.livingdoc.model.DocumentationModel;
import io.hexaglue.plugin.livingdoc.model.DocumentationModelFactory;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Multi-module tests for {@link OverviewGenerator}.
 *
 * @since 5.0.0
 */
@DisplayName("OverviewGenerator - Multi-Module")
class OverviewGeneratorMultiModuleTest {

    private static final ProjectContext PROJECT = ProjectContext.forTesting("banking", "com.example");

    @Nested
    @DisplayName("Module Topology section")
    class ModuleTopologySection {

        @Test
        @DisplayName("should include Module Topology section when ModuleIndex is present")
        void shouldIncludeModuleTopologySectionWhenModuleIndexPresent() {
            AggregateRoot order = aggregateRoot("com.example.domain.Order");
            ArchitecturalModel model = createModel(PROJECT, order);
            DocumentationModel docModel = DocumentationModelFactory.fromArchModel(model);

            ModuleDescriptor coreModule = ModuleDescriptor.of("banking-core", ModuleRole.DOMAIN, Path.of("/tmp/core"));
            ModuleIndex moduleIndex = ModuleIndex.builder()
                    .addModule(coreModule)
                    .assignType(TypeId.of("com.example.domain.Order"), coreModule)
                    .build();

            OverviewGenerator gen =
                    new OverviewGenerator(docModel, null, List.of(), List.of(), null, null, moduleIndex);
            String output = gen.generate();

            assertThat(output).contains("## Module Topology");
            assertThat(output).contains("banking-core");
            assertThat(output).contains("DOMAIN");
            assertThat(output).contains("modules.md");
        }

        @Test
        @DisplayName("should omit Module Topology section when mono-module")
        void shouldOmitModuleTopologySectionWhenMonoModule() {
            AggregateRoot order = aggregateRoot("com.example.domain.Order");
            ArchitecturalModel model = createModel(PROJECT, order);
            DocumentationModel docModel = DocumentationModelFactory.fromArchModel(model);

            OverviewGenerator gen = new OverviewGenerator(docModel, null, List.of(), List.of(), null, null, null);
            String output = gen.generate();

            assertThat(output).doesNotContain("Module Topology");
            assertThat(output).doesNotContain("modules.md");
        }

        @Test
        @DisplayName("should include Modules link in documentation section when ModuleIndex is present")
        void shouldIncludeModulesLinkWhenModuleIndexPresent() {
            AggregateRoot order = aggregateRoot("com.example.domain.Order");
            ArchitecturalModel model = createModel(PROJECT, order);
            DocumentationModel docModel = DocumentationModelFactory.fromArchModel(model);

            ModuleDescriptor coreModule = ModuleDescriptor.of("banking-core", ModuleRole.DOMAIN, Path.of("/tmp/core"));
            ModuleIndex moduleIndex =
                    ModuleIndex.builder().addModule(coreModule).build();

            OverviewGenerator gen =
                    new OverviewGenerator(docModel, null, List.of(), List.of(), null, null, moduleIndex);
            String output = gen.generate();

            assertThat(output).contains("[Modules](modules.md)");
        }
    }
}

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

package io.hexaglue.plugin.rest;

import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.TypeSpec;
import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.model.DrivingPort;
import io.hexaglue.arch.model.index.DomainIndex;
import io.hexaglue.arch.model.index.PortIndex;
import io.hexaglue.plugin.rest.builder.ControllerSpecBuilder;
import io.hexaglue.plugin.rest.codegen.RequestDtoCodegen;
import io.hexaglue.plugin.rest.codegen.RestControllerCodegen;
import io.hexaglue.plugin.rest.model.ControllerSpec;
import io.hexaglue.plugin.rest.model.RequestDtoSpec;
import io.hexaglue.spi.generation.ArtifactWriter;
import io.hexaglue.spi.generation.GeneratorContext;
import io.hexaglue.spi.generation.GeneratorPlugin;
import io.hexaglue.spi.plugin.DiagnosticReporter;
import io.hexaglue.spi.plugin.PluginConfig;
import java.io.IOException;
import java.util.List;

/**
 * REST plugin for HexaGlue.
 *
 * <p>Generates Spring MVC REST controllers from driving ports using JavaPoet
 * for type-safe code generation. Each driving port with use cases produces
 * a {@code @RestController} class.
 *
 * <p>Phase 1 (MVP) generates controllers with:
 * <ul>
 *   <li>{@code @RestController}, {@code @RequestMapping}, {@code @Tag}</li>
 *   <li>Constructor injection of the driving port</li>
 *   <li>Endpoints using FallbackStrategy (QUERY to GET, COMMAND to POST)</li>
 * </ul>
 *
 * @since 3.1.0
 */
public final class RestPlugin implements GeneratorPlugin {

    /** Plugin identifier. */
    public static final String PLUGIN_ID = "io.hexaglue.plugin.rest";

    /** Indentation for generated code (4 spaces). */
    private static final String INDENT = "    ";

    @Override
    public String id() {
        return PLUGIN_ID;
    }

    @Override
    public void generate(GeneratorContext context) throws Exception {
        PluginConfig pluginConfig = context.config();
        ArtifactWriter writer = context.writer();
        DiagnosticReporter diagnostics = context.diagnostics();
        ArchitecturalModel model = context.model().orElse(null);

        if (model == null) {
            diagnostics.error("ArchitecturalModel is required for REST code generation.");
            return;
        }

        PortIndex portIndex = model.portIndex().orElse(null);
        if (portIndex == null) {
            diagnostics.error("PortIndex is required for REST code generation.");
            return;
        }

        RestConfig config = RestConfig.from(pluginConfig);

        String basePackage = model.project().basePackage();
        String apiPackage = config.apiPackage() != null ? config.apiPackage() : deriveApiPackage(basePackage);

        DomainIndex domainIndex = model.domainIndex().orElse(null);

        List<DrivingPort> drivingPorts =
                portIndex.drivingPorts().filter(DrivingPort::hasUseCases).toList();

        if (drivingPorts.isEmpty()) {
            diagnostics.info("No driving ports with use cases found. Skipping REST generation.");
            return;
        }

        int controllerCount = 0;
        int dtoCount = 0;
        for (DrivingPort port : drivingPorts) {
            ControllerSpec spec = ControllerSpecBuilder.builder()
                    .drivingPort(port)
                    .config(config)
                    .apiPackage(apiPackage)
                    .domainIndex(domainIndex)
                    .build();

            TypeSpec typeSpec = RestControllerCodegen.generate(spec, config);
            String source = toJavaSource(spec.packageName(), typeSpec);
            writeJavaSource(writer, spec.packageName(), spec.className(), source, diagnostics);
            controllerCount++;

            // Generate request DTOs
            for (RequestDtoSpec requestDto : spec.requestDtos()) {
                TypeSpec dtoTypeSpec = RequestDtoCodegen.generate(requestDto);
                String dtoSource = toJavaSource(requestDto.packageName(), dtoTypeSpec);
                writeJavaSource(writer, requestDto.packageName(), requestDto.className(), dtoSource, diagnostics);
                dtoCount++;
            }
        }

        diagnostics.info("REST plugin generated " + controllerCount + " controller(s) and " + dtoCount + " DTO(s).");
    }

    private static String toJavaSource(String packageName, TypeSpec typeSpec) {
        return JavaFile.builder(packageName, typeSpec).indent(INDENT).build().toString();
    }

    private static void writeJavaSource(
            ArtifactWriter writer,
            String packageName,
            String className,
            String source,
            DiagnosticReporter diagnostics) {
        try {
            writer.writeJavaSource(packageName, className, source);
        } catch (IOException e) {
            diagnostics.error("Failed to write " + packageName + "." + className, e);
        }
    }

    private static String deriveApiPackage(String basePackage) {
        return basePackage + ".api";
    }
}

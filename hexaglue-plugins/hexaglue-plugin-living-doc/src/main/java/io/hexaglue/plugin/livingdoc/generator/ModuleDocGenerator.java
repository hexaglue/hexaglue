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

import io.hexaglue.arch.model.TypeId;
import io.hexaglue.arch.model.index.ModuleDescriptor;
import io.hexaglue.arch.model.index.ModuleIndex;
import io.hexaglue.plugin.livingdoc.markdown.MarkdownBuilder;
import io.hexaglue.plugin.livingdoc.markdown.TableBuilder;
import io.hexaglue.plugin.livingdoc.util.PluginVersion;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Generates the module topology documentation for multi-module projects.
 *
 * <p>Produces a {@code modules.md} file with:
 * <ul>
 *   <li>Summary table of all modules with their roles and type counts</li>
 *   <li>Detailed section per module listing all types</li>
 * </ul>
 *
 * @since 5.0.0
 */
public final class ModuleDocGenerator {

    private final ModuleIndex moduleIndex;

    /**
     * Creates a ModuleDocGenerator.
     *
     * @param moduleIndex the module index
     * @throws NullPointerException if moduleIndex is null
     */
    public ModuleDocGenerator(ModuleIndex moduleIndex) {
        this.moduleIndex = Objects.requireNonNull(moduleIndex, "moduleIndex must not be null");
    }

    /**
     * Generates the module topology Markdown content.
     *
     * @return the generated Markdown
     */
    public String generate() {
        MarkdownBuilder md = new MarkdownBuilder()
                .h1("Module Topology")
                .paragraph(PluginVersion.generatorHeader())
                .horizontalRule();

        List<ModuleDescriptor> modules = moduleIndex
                .modules()
                .sorted(Comparator.comparing(ModuleDescriptor::moduleId))
                .toList();

        // Summary table
        md.h2("Summary");
        TableBuilder table = md.table("Module", "Role", "Types", "Base Package");
        for (ModuleDescriptor module : modules) {
            long typeCount = moduleIndex.typesInModule(module.moduleId()).count();
            String basePackage = module.basePackage() != null ? module.basePackage() : "-";
            table.row(module.moduleId(), module.role().name(), String.valueOf(typeCount), basePackage);
        }
        table.end();

        // Detailed section per module
        for (ModuleDescriptor module : modules) {
            md.h2(module.moduleId());
            md.paragraph("**Role:** " + module.role().name());

            List<String> typeNames = moduleIndex
                    .typesInModule(module.moduleId())
                    .sorted(Comparator.comparing(TypeId::qualifiedName))
                    .map(TypeId::simpleName)
                    .toList();

            if (typeNames.isEmpty()) {
                md.paragraph("*No classified types in this module.*");
            } else {
                md.paragraph(typeNames.size() + " types:");
                for (String typeName : typeNames) {
                    md.bulletItem("`" + typeName + "`");
                }
                md.newline();
            }
        }

        return md.build();
    }
}

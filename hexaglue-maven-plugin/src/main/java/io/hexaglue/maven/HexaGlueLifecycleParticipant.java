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

package io.hexaglue.maven;

import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * Maven lifecycle participant that automatically binds HexaGlue goals to Maven phases
 * and configures transitive dependencies required by HexaGlue plugins.
 *
 * <p>When the HexaGlue Maven Plugin is declared with {@code <extensions>true</extensions>},
 * this participant automatically:
 * <ul>
 *   <li>Adds {@code generate} goal bound to {@code generate-sources} phase</li>
 *   <li>Adds {@code audit} goal bound to {@code verify} phase</li>
 *   <li>Injects MapStruct dependencies when the JPA plugin is detected</li>
 * </ul>
 *
 * <p>MapStruct auto-configuration: when {@code hexaglue-plugin-jpa} is declared as a
 * plugin dependency, this participant injects {@code org.mapstruct:mapstruct} (compile)
 * and {@code org.mapstruct:mapstruct-processor} (provided) into the project dependencies.
 * If the project already declares these dependencies, they are not overwritten.
 * If {@code maven-compiler-plugin} has {@code annotationProcessorPaths} configured,
 * the processor is also added there.
 *
 * @since 3.0.0
 */
@Named("hexaglue")
@Singleton
public class HexaGlueLifecycleParticipant extends AbstractMavenLifecycleParticipant {

    private static final String GROUP_ID = "io.hexaglue";
    private static final String ARTIFACT_ID = "hexaglue-maven-plugin";

    private static final String GENERATE_GOAL = "generate";
    private static final String GENERATE_PHASE = "generate-sources";
    private static final String GENERATE_EXECUTION_ID = "default-hexaglue-generate";

    private static final String AUDIT_GOAL = "audit";
    private static final String AUDIT_PHASE = "verify";
    private static final String AUDIT_EXECUTION_ID = "default-hexaglue-audit";

    private static final String REACTOR_GENERATE_GOAL = "reactor-generate";
    private static final String REACTOR_GENERATE_EXECUTION_ID = "default-hexaglue-reactor-generate";

    private static final String REACTOR_AUDIT_GOAL = "reactor-audit";
    private static final String REACTOR_AUDIT_EXECUTION_ID = "default-hexaglue-reactor-audit";

    private static final String JPA_PLUGIN_GROUP_ID = "io.hexaglue.plugins";
    private static final String JPA_PLUGIN_ARTIFACT_ID = "hexaglue-plugin-jpa";

    private static final String MAPSTRUCT_GROUP_ID = "org.mapstruct";
    private static final String MAPSTRUCT_ARTIFACT_ID = "mapstruct";
    private static final String MAPSTRUCT_PROCESSOR_ARTIFACT_ID = "mapstruct-processor";
    private static final String MAPSTRUCT_VERSION = "1.6.3";

    private static final String COMPILER_PLUGIN_GROUP_ID = "org.apache.maven.plugins";
    private static final String COMPILER_PLUGIN_ARTIFACT_ID = "maven-compiler-plugin";

    @Override
    public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
        MavenProject topLevel = session.getProjects().get(0);
        boolean isMultiModule = topLevel.getModules() != null && !topLevel.getModules().isEmpty();

        if (isMultiModule) {
            // Multi-module: inject reactor goals on the parent project only
            injectReactorExecutionsIfNeeded(topLevel);

            // Inject MapStruct into child modules that have the JPA plugin
            for (MavenProject project : session.getProjects()) {
                Plugin hexagluePlugin = findHexaGluePlugin(project);
                if (hexagluePlugin != null && hasJpaPlugin(hexagluePlugin)) {
                    injectMapStructDependencies(project);
                    injectMapStructAnnotationProcessor(project);
                }
            }
        } else {
            // Mono-module: existing behavior
            for (MavenProject project : session.getProjects()) {
                injectExecutionsIfNeeded(project);
            }
        }
    }

    /**
     * Injects reactor-level goals on the parent project for multi-module builds.
     *
     * <p>Instead of injecting {@code generate} and {@code audit} on each child module,
     * this injects {@code reactor-generate} and {@code reactor-audit} on the parent POM,
     * so HexaGlue runs once with a unified view of all modules.</p>
     *
     * @param parentProject the top-level reactor project
     * @since 5.0.0
     */
    private void injectReactorExecutionsIfNeeded(MavenProject parentProject) {
        Plugin hexagluePlugin = findHexaGluePlugin(parentProject);
        if (hexagluePlugin == null) {
            return;
        }

        injectGoalIfNeeded(
                hexagluePlugin, REACTOR_GENERATE_GOAL, GENERATE_PHASE, REACTOR_GENERATE_EXECUTION_ID);
        injectGoalIfNeeded(
                hexagluePlugin, REACTOR_AUDIT_GOAL, AUDIT_PHASE, REACTOR_AUDIT_EXECUTION_ID);
    }

    private void injectExecutionsIfNeeded(MavenProject project) {
        Plugin hexagluePlugin = findHexaGluePlugin(project);
        if (hexagluePlugin == null) {
            return;
        }

        injectGoalIfNeeded(hexagluePlugin, GENERATE_GOAL, GENERATE_PHASE, GENERATE_EXECUTION_ID);
        injectGoalIfNeeded(hexagluePlugin, AUDIT_GOAL, AUDIT_PHASE, AUDIT_EXECUTION_ID);

        if (hasJpaPlugin(hexagluePlugin)) {
            injectMapStructDependencies(project);
            injectMapStructAnnotationProcessor(project);
        }
    }

    private void injectGoalIfNeeded(Plugin plugin, String goal, String phase, String executionId) {
        // Check if there's already an execution with this goal
        boolean hasExecution =
                plugin.getExecutions().stream().anyMatch(exec -> exec.getGoals().contains(goal));

        if (hasExecution) {
            // User has explicitly configured this execution, don't interfere
            return;
        }

        // Add default execution
        PluginExecution execution = new PluginExecution();
        execution.setId(executionId);
        execution.setPhase(phase);
        execution.addGoal(goal);

        // Inherit configuration from plugin level
        if (plugin.getConfiguration() != null) {
            execution.setConfiguration(plugin.getConfiguration());
        }

        plugin.addExecution(execution);
    }

    private Plugin findHexaGluePlugin(MavenProject project) {
        return project.getBuildPlugins().stream()
                .filter(p -> GROUP_ID.equals(p.getGroupId()) && ARTIFACT_ID.equals(p.getArtifactId()))
                .findFirst()
                .orElse(null);
    }

    private boolean hasJpaPlugin(Plugin hexagluePlugin) {
        return hexagluePlugin.getDependencies().stream()
                .anyMatch(d ->
                        JPA_PLUGIN_GROUP_ID.equals(d.getGroupId()) && JPA_PLUGIN_ARTIFACT_ID.equals(d.getArtifactId()));
    }

    /**
     * Injects MapStruct compile and processor dependencies into the project
     * if not already present.
     */
    private void injectMapStructDependencies(MavenProject project) {
        if (!hasProjectDependency(project, MAPSTRUCT_GROUP_ID, MAPSTRUCT_ARTIFACT_ID)) {
            project.getDependencies().add(createDependency(MAPSTRUCT_GROUP_ID, MAPSTRUCT_ARTIFACT_ID, null));
        }
        if (!hasProjectDependency(project, MAPSTRUCT_GROUP_ID, MAPSTRUCT_PROCESSOR_ARTIFACT_ID)) {
            project.getDependencies()
                    .add(createDependency(MAPSTRUCT_GROUP_ID, MAPSTRUCT_PROCESSOR_ARTIFACT_ID, "provided"));
        }
    }

    /**
     * If {@code maven-compiler-plugin} has {@code annotationProcessorPaths} configured,
     * adds the MapStruct processor to the list (unless already present).
     */
    private void injectMapStructAnnotationProcessor(MavenProject project) {
        Plugin compilerPlugin = findPlugin(project, COMPILER_PLUGIN_GROUP_ID, COMPILER_PLUGIN_ARTIFACT_ID);
        if (compilerPlugin == null) {
            return;
        }

        Object configObj = compilerPlugin.getConfiguration();
        if (!(configObj instanceof Xpp3Dom config)) {
            return;
        }

        Xpp3Dom paths = config.getChild("annotationProcessorPaths");
        if (paths == null) {
            return;
        }

        if (hasAnnotationProcessorPath(paths, MAPSTRUCT_GROUP_ID, MAPSTRUCT_PROCESSOR_ARTIFACT_ID)) {
            return;
        }

        Xpp3Dom processorPath = new Xpp3Dom("path");
        addChild(processorPath, "groupId", MAPSTRUCT_GROUP_ID);
        addChild(processorPath, "artifactId", MAPSTRUCT_PROCESSOR_ARTIFACT_ID);
        addChild(processorPath, "version", MAPSTRUCT_VERSION);
        paths.addChild(processorPath);
    }

    private boolean hasProjectDependency(MavenProject project, String groupId, String artifactId) {
        return project.getDependencies().stream()
                .anyMatch(d -> groupId.equals(d.getGroupId()) && artifactId.equals(d.getArtifactId()));
    }

    private Dependency createDependency(String groupId, String artifactId, String scope) {
        Dependency dep = new Dependency();
        dep.setGroupId(groupId);
        dep.setArtifactId(artifactId);
        dep.setVersion(MAPSTRUCT_VERSION);
        if (scope != null) {
            dep.setScope(scope);
        }
        return dep;
    }

    private Plugin findPlugin(MavenProject project, String groupId, String artifactId) {
        return project.getBuild().getPlugins().stream()
                .filter(p -> groupId.equals(p.getGroupId()) && artifactId.equals(p.getArtifactId()))
                .findFirst()
                .orElse(null);
    }

    private boolean hasAnnotationProcessorPath(Xpp3Dom paths, String groupId, String artifactId) {
        for (Xpp3Dom child : paths.getChildren()) {
            Xpp3Dom gid = child.getChild("groupId");
            Xpp3Dom aid = child.getChild("artifactId");
            if (gid != null && aid != null && groupId.equals(gid.getValue()) && artifactId.equals(aid.getValue())) {
                return true;
            }
        }
        return false;
    }

    private void addChild(Xpp3Dom parent, String name, String value) {
        Xpp3Dom child = new Xpp3Dom(name);
        child.setValue(value);
        parent.addChild(child);
    }
}

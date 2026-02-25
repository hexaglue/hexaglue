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

import io.hexaglue.core.engine.MultiModuleOutputResolver;
import java.nio.file.Path;
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
 *   <li>Injects delombok preprocessing when Lombok is detected (since 6.0.0)</li>
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

    private static final String LOMBOK_GROUP_ID = "org.projectlombok";
    private static final String LOMBOK_ARTIFACT_ID = "lombok";

    private static final String EXEC_PLUGIN_GROUP_ID = "org.codehaus.mojo";
    private static final String EXEC_PLUGIN_ARTIFACT_ID = "exec-maven-plugin";
    private static final String EXEC_PLUGIN_VERSION = "3.6.3";
    private static final String DELOMBOK_EXECUTION_ID = "hexaglue-delombok";

    private static final String DEPENDENCY_PLUGIN_GROUP_ID = "org.apache.maven.plugins";
    private static final String DEPENDENCY_PLUGIN_ARTIFACT_ID = "maven-dependency-plugin";
    private static final String DEPENDENCY_PLUGIN_VERSION = "3.10.0";
    private static final String DEPENDENCY_PROPS_EXECUTION_ID = "hexaglue-dependency-properties";

    @Override
    public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
        MavenProject topLevel = session.getProjects().get(0);
        boolean isMultiModule =
                topLevel.getModules() != null && !topLevel.getModules().isEmpty();

        if (isMultiModule) {
            // Multi-module: inject reactor goals on the parent project only
            injectReactorExecutionsIfNeeded(topLevel);

            // Register generated-sources roots early so IDEs discover them on import
            registerMultiModuleSourceRoots(session, topLevel);

            // Inject MapStruct and delombok into child modules as needed
            for (MavenProject project : session.getProjects()) {
                Plugin hexagluePlugin = findHexaGluePlugin(project);
                if (hexagluePlugin != null && hasJpaPlugin(hexagluePlugin)) {
                    injectMapStructDependencies(project);
                    injectMapStructAnnotationProcessor(project);
                }
                injectDelombokIfNeeded(project);
            }
        } else {
            // Mono-module: existing behavior
            for (MavenProject project : session.getProjects()) {
                injectExecutionsIfNeeded(project);
                injectDelombokIfNeeded(project);
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

        injectGoalIfNeeded(hexagluePlugin, REACTOR_GENERATE_GOAL, GENERATE_PHASE, REACTOR_GENERATE_EXECUTION_ID);
        injectGoalIfNeeded(hexagluePlugin, REACTOR_AUDIT_GOAL, AUDIT_PHASE, REACTOR_AUDIT_EXECUTION_ID);
    }

    /**
     * Registers generated-source roots for each {@code jar}-packaged child module.
     *
     * <p>Source roots point to the path resolved by {@link MultiModuleOutputResolver}
     * (under the reactor root's {@code target/}) so that child module {@code clean} phases
     * never erase generated code. Calling {@code addCompileSourceRoot} early (in
     * {@code afterProjectsRead}) ensures IDEs discover the roots during project import.</p>
     *
     * @since 5.0.0
     */
    private void registerMultiModuleSourceRoots(MavenSession session, MavenProject topLevel) {
        MultiModuleOutputResolver outputResolver =
                new MultiModuleOutputResolver(topLevel.getBasedir().toPath());
        for (MavenProject project : session.getProjects()) {
            if (!"jar".equals(project.getPackaging())) {
                continue;
            }
            String moduleId = project.getArtifactId();
            Path sourceRoot = outputResolver.resolveSourcesDirectory(moduleId);
            project.addCompileSourceRoot(sourceRoot.toAbsolutePath().toString());
        }
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

    /**
     * Detects whether the project has Lombok as a dependency (any scope).
     *
     * @since 6.0.0
     */
    private boolean hasLombok(MavenProject project) {
        return project.getDependencies().stream()
                .anyMatch(d -> LOMBOK_GROUP_ID.equals(d.getGroupId()) && LOMBOK_ARTIFACT_ID.equals(d.getArtifactId()));
    }

    /**
     * Injects a delombok execution via {@code exec-maven-plugin} to expand Lombok
     * annotations before Spoon analysis.
     *
     * <p>Uses {@code exec-maven-plugin} to invoke {@code java -jar lombok.jar delombok}
     * directly, avoiding dependency on the unmaintained {@code lombok-maven-plugin}.
     * This guarantees compatibility with any Lombok version declared by the project.
     *
     * <p>The delombok output is written to {@code target/hexaglue/delombok-sources}.
     * This directory replaces the original source roots when passed to SpoonFrontend,
     * so Spoon sees standard Java code with explicit getters, setters, etc.
     *
     * <p>To resolve the Lombok JAR path at execution time, a
     * {@code maven-dependency-plugin:properties} execution is also injected at the
     * {@code validate} phase. This exposes {@code ${org.projectlombok:lombok:jar}}
     * as a Maven property, which is referenced in the exec plugin arguments.
     *
     * <p>The delombok execution is bound to the {@code initialize} phase to guarantee it runs
     * before HexaGlue's generate goal at {@code generate-sources}.
     *
     * @since 6.0.0
     */
    private void injectDelombokIfNeeded(MavenProject project) {
        if (!hasLombok(project)) {
            return;
        }

        // Skip if user already manages delombok themselves via lombok-maven-plugin
        if (findPlugin(project, LOMBOK_GROUP_ID, "lombok-maven-plugin") != null) {
            return;
        }

        // Skip if delombok execution already injected
        Plugin existingExecPlugin = findPlugin(project, EXEC_PLUGIN_GROUP_ID, EXEC_PLUGIN_ARTIFACT_ID);
        if (existingExecPlugin != null && hasExecution(existingExecPlugin, DELOMBOK_EXECUTION_ID)) {
            return;
        }

        // Inject maven-dependency-plugin:properties to expose ${org.projectlombok:lombok:jar}
        injectDependencyPropertiesIfNeeded(project);

        String sourceDir = project.getBuild().getSourceDirectory();
        String outputDir = project.getBuild().getDirectory() + "/" + MojoSourceRootsResolver.DELOMBOK_OUTPUT_SUBDIR;

        Plugin execPlugin = existingExecPlugin != null
                ? existingExecPlugin
                : createPlugin(EXEC_PLUGIN_GROUP_ID, EXEC_PLUGIN_ARTIFACT_ID, EXEC_PLUGIN_VERSION);

        PluginExecution execution = new PluginExecution();
        execution.setId(DELOMBOK_EXECUTION_ID);
        execution.setPhase("initialize");
        execution.addGoal("exec");

        Xpp3Dom config = new Xpp3Dom("configuration");
        addChild(config, "executable", "java");

        Xpp3Dom arguments = new Xpp3Dom("arguments");
        addChild(arguments, "argument", "-jar");
        addChild(arguments, "argument", "${org.projectlombok:lombok:jar}");
        addChild(arguments, "argument", "delombok");
        addChild(arguments, "argument", sourceDir);
        addChild(arguments, "argument", "-d");
        addChild(arguments, "argument", outputDir);
        config.addChild(arguments);

        execution.setConfiguration(config);
        execPlugin.addExecution(execution);

        if (existingExecPlugin == null) {
            project.getBuild().addPlugin(execPlugin);
        }
    }

    /**
     * Injects a {@code maven-dependency-plugin:properties} execution at the {@code validate}
     * phase so that artifact paths are exposed as Maven properties (e.g.,
     * {@code ${org.projectlombok:lombok:jar}}).
     *
     * @since 6.0.0
     */
    private void injectDependencyPropertiesIfNeeded(MavenProject project) {
        Plugin depPlugin = findPlugin(project, DEPENDENCY_PLUGIN_GROUP_ID, DEPENDENCY_PLUGIN_ARTIFACT_ID);

        if (depPlugin != null && hasExecution(depPlugin, DEPENDENCY_PROPS_EXECUTION_ID)) {
            return;
        }

        Plugin plugin = depPlugin != null
                ? depPlugin
                : createPlugin(DEPENDENCY_PLUGIN_GROUP_ID, DEPENDENCY_PLUGIN_ARTIFACT_ID, DEPENDENCY_PLUGIN_VERSION);

        PluginExecution execution = new PluginExecution();
        execution.setId(DEPENDENCY_PROPS_EXECUTION_ID);
        execution.setPhase("validate");
        execution.addGoal("properties");

        plugin.addExecution(execution);

        if (depPlugin == null) {
            project.getBuild().addPlugin(plugin);
        }
    }

    private boolean hasExecution(Plugin plugin, String executionId) {
        return plugin.getExecutions().stream().anyMatch(exec -> executionId.equals(exec.getId()));
    }

    private Plugin createPlugin(String groupId, String artifactId, String version) {
        Plugin plugin = new Plugin();
        plugin.setGroupId(groupId);
        plugin.setArtifactId(artifactId);
        plugin.setVersion(version);
        return plugin;
    }
}

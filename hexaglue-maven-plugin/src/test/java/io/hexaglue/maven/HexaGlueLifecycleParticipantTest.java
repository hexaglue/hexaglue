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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link HexaGlueLifecycleParticipant} MapStruct auto-injection.
 *
 * <p>Verifies that when the JPA plugin is detected in hexaglue-maven-plugin dependencies,
 * MapStruct dependencies and annotation processor configuration are automatically injected
 * into the user's project.
 *
 * @since 5.0.0
 */
class HexaGlueLifecycleParticipantTest {

    private HexaGlueLifecycleParticipant participant;

    @BeforeEach
    void setUp() {
        participant = new HexaGlueLifecycleParticipant();
    }

    @Nested
    @DisplayName("When JPA plugin is present")
    class WhenJpaPluginPresent {

        @Test
        @DisplayName("should inject mapstruct dependency into project")
        void shouldInjectMapstructDependency() throws Exception {
            MavenProject project = createProjectWithJpaPlugin();
            MavenSession session = createSession(project);

            participant.afterProjectsRead(session);

            assertThat(project.getDependencies())
                    .filteredOn(d -> "org.mapstruct".equals(d.getGroupId()) && "mapstruct".equals(d.getArtifactId()))
                    .hasSize(1)
                    .first()
                    .satisfies(d -> {
                        assertThat(d.getVersion()).isEqualTo("1.6.3");
                        assertThat(d.getScope()).isNull();
                    });
        }

        @Test
        @DisplayName("should inject mapstruct-processor dependency as provided")
        void shouldInjectMapstructProcessorDependency() throws Exception {
            MavenProject project = createProjectWithJpaPlugin();
            MavenSession session = createSession(project);

            participant.afterProjectsRead(session);

            assertThat(project.getDependencies())
                    .filteredOn(d ->
                            "org.mapstruct".equals(d.getGroupId()) && "mapstruct-processor".equals(d.getArtifactId()))
                    .hasSize(1)
                    .first()
                    .satisfies(d -> {
                        assertThat(d.getVersion()).isEqualTo("1.6.3");
                        assertThat(d.getScope()).isEqualTo("provided");
                    });
        }

        @Test
        @DisplayName("should not duplicate mapstruct if already declared by user")
        void shouldNotDuplicateMapstructIfAlreadyPresent() throws Exception {
            MavenProject project = createProjectWithJpaPlugin();
            Dependency existingMapstruct = createDependency("org.mapstruct", "mapstruct", "1.5.5.Final", null);
            project.getDependencies().add(existingMapstruct);
            MavenSession session = createSession(project);

            participant.afterProjectsRead(session);

            long mapstructCount = project.getDependencies().stream()
                    .filter(d -> "org.mapstruct".equals(d.getGroupId()) && "mapstruct".equals(d.getArtifactId()))
                    .count();
            assertThat(mapstructCount).isEqualTo(1);
            // Should keep user's version
            assertThat(project.getDependencies().stream()
                            .filter(d ->
                                    "org.mapstruct".equals(d.getGroupId()) && "mapstruct".equals(d.getArtifactId()))
                            .findFirst()
                            .orElseThrow()
                            .getVersion())
                    .isEqualTo("1.5.5.Final");
        }

        @Test
        @DisplayName("should not duplicate mapstruct-processor if already declared by user")
        void shouldNotDuplicateMapstructProcessorIfAlreadyPresent() throws Exception {
            MavenProject project = createProjectWithJpaPlugin();
            Dependency existingProcessor =
                    createDependency("org.mapstruct", "mapstruct-processor", "1.5.5.Final", "provided");
            project.getDependencies().add(existingProcessor);
            MavenSession session = createSession(project);

            participant.afterProjectsRead(session);

            long processorCount = project.getDependencies().stream()
                    .filter(d ->
                            "org.mapstruct".equals(d.getGroupId()) && "mapstruct-processor".equals(d.getArtifactId()))
                    .count();
            assertThat(processorCount).isEqualTo(1);
        }

        @Test
        @DisplayName("should inject mapstruct-processor into existing annotationProcessorPaths")
        void shouldInjectIntoExistingAnnotationProcessorPaths() throws Exception {
            MavenProject project = createProjectWithJpaPlugin();
            addCompilerPluginWithAnnotationProcessorPaths(project, "org.projectlombok", "lombok", "1.18.30");
            MavenSession session = createSession(project);

            participant.afterProjectsRead(session);

            Plugin compilerPlugin = findPlugin(project, "org.apache.maven.plugins", "maven-compiler-plugin");
            assertThat(compilerPlugin).isNotNull();

            Xpp3Dom config = (Xpp3Dom) compilerPlugin.getConfiguration();
            Xpp3Dom paths = config.getChild("annotationProcessorPaths");
            assertThat(paths.getChildCount()).isEqualTo(2);

            // Lombok should still be there
            assertThat(hasAnnotationProcessorPath(paths, "org.projectlombok", "lombok"))
                    .isTrue();
            // MapStruct processor should be added
            assertThat(hasAnnotationProcessorPath(paths, "org.mapstruct", "mapstruct-processor"))
                    .isTrue();
        }

        @Test
        @DisplayName("should not duplicate mapstruct-processor in annotationProcessorPaths if already present")
        void shouldNotDuplicateMapstructProcessorInAnnotationProcessorPaths() throws Exception {
            MavenProject project = createProjectWithJpaPlugin();
            addCompilerPluginWithAnnotationProcessorPaths(project, "org.mapstruct", "mapstruct-processor", "1.6.3");
            MavenSession session = createSession(project);

            participant.afterProjectsRead(session);

            Plugin compilerPlugin = findPlugin(project, "org.apache.maven.plugins", "maven-compiler-plugin");
            Xpp3Dom config = (Xpp3Dom) compilerPlugin.getConfiguration();
            Xpp3Dom paths = config.getChild("annotationProcessorPaths");
            assertThat(paths.getChildCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should not touch compiler plugin without annotationProcessorPaths")
        void shouldNotTouchCompilerPluginWithoutAnnotationProcessorPaths() throws Exception {
            MavenProject project = createProjectWithJpaPlugin();
            // Add a compiler plugin with only source/target config
            Plugin compilerPlugin = createPlugin("org.apache.maven.plugins", "maven-compiler-plugin");
            Xpp3Dom config = new Xpp3Dom("configuration");
            Xpp3Dom source = new Xpp3Dom("source");
            source.setValue("17");
            config.addChild(source);
            compilerPlugin.setConfiguration(config);
            project.getBuild().getPlugins().add(compilerPlugin);
            MavenSession session = createSession(project);

            participant.afterProjectsRead(session);

            Plugin updatedCompiler = findPlugin(project, "org.apache.maven.plugins", "maven-compiler-plugin");
            Xpp3Dom updatedConfig = (Xpp3Dom) updatedCompiler.getConfiguration();
            assertThat(updatedConfig.getChild("annotationProcessorPaths")).isNull();
        }
    }

    @Nested
    @DisplayName("When JPA plugin is absent")
    class WhenJpaPluginAbsent {

        @Test
        @DisplayName("should not inject any dependencies")
        void shouldNotInjectDependencies() throws Exception {
            MavenProject project = createProjectWithoutJpaPlugin();
            int initialDependencyCount = project.getDependencies().size();
            MavenSession session = createSession(project);

            participant.afterProjectsRead(session);

            assertThat(project.getDependencies()).hasSize(initialDependencyCount);
        }

        @Test
        @DisplayName("should not modify compiler plugin")
        void shouldNotModifyCompilerPlugin() throws Exception {
            MavenProject project = createProjectWithoutJpaPlugin();
            addCompilerPluginWithAnnotationProcessorPaths(project, "org.projectlombok", "lombok", "1.18.30");
            MavenSession session = createSession(project);

            participant.afterProjectsRead(session);

            Plugin compilerPlugin = findPlugin(project, "org.apache.maven.plugins", "maven-compiler-plugin");
            Xpp3Dom config = (Xpp3Dom) compilerPlugin.getConfiguration();
            Xpp3Dom paths = config.getChild("annotationProcessorPaths");
            assertThat(paths.getChildCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("When hexaglue-maven-plugin is not present")
    class WhenHexaGluePluginAbsent {

        @Test
        @DisplayName("should do nothing")
        void shouldDoNothing() throws Exception {
            MavenProject project = new MavenProject();
            project.setBuild(new Build());
            MavenSession session = createSession(project);

            participant.afterProjectsRead(session);

            assertThat(project.getDependencies()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Multi-module support")
    class MultiModuleSupport {

        @Test
        @DisplayName("should inject into project with JPA plugin and skip others")
        void shouldInjectSelectivelyInMultiModule() throws Exception {
            MavenProject projectWithJpa = createProjectWithJpaPlugin();
            MavenProject projectWithoutJpa = createProjectWithoutJpaPlugin();
            int initialDepsWithoutJpa = projectWithoutJpa.getDependencies().size();

            MavenSession session = createSession(projectWithJpa, projectWithoutJpa);

            participant.afterProjectsRead(session);

            // Project with JPA should get mapstruct injected
            assertThat(projectWithJpa.getDependencies())
                    .filteredOn(d -> "org.mapstruct".equals(d.getGroupId()))
                    .isNotEmpty();

            // Project without JPA should remain unchanged
            assertThat(projectWithoutJpa.getDependencies()).hasSize(initialDepsWithoutJpa);
        }
    }

    // --- Helper methods ---

    private MavenProject createProjectWithJpaPlugin() {
        MavenProject project = new MavenProject();
        project.setDependencies(new ArrayList<>());
        Build build = new Build();

        Plugin hexagluePlugin = createPlugin("io.hexaglue", "hexaglue-maven-plugin");
        Dependency jpaDep = createDependency("io.hexaglue.plugins", "hexaglue-plugin-jpa", "2.0.0-SNAPSHOT", null);
        hexagluePlugin.setDependencies(List.of(jpaDep));

        build.setPlugins(new ArrayList<>(List.of(hexagluePlugin)));
        project.setBuild(build);
        return project;
    }

    private MavenProject createProjectWithoutJpaPlugin() {
        MavenProject project = new MavenProject();
        project.setDependencies(new ArrayList<>());
        Build build = new Build();

        Plugin hexagluePlugin = createPlugin("io.hexaglue", "hexaglue-maven-plugin");
        Dependency livingDocDep =
                createDependency("io.hexaglue.plugins", "hexaglue-plugin-living-doc", "2.0.0-SNAPSHOT", null);
        hexagluePlugin.setDependencies(List.of(livingDocDep));

        build.setPlugins(new ArrayList<>(List.of(hexagluePlugin)));
        project.setBuild(build);
        return project;
    }

    private Plugin createPlugin(String groupId, String artifactId) {
        Plugin plugin = new Plugin();
        plugin.setGroupId(groupId);
        plugin.setArtifactId(artifactId);
        return plugin;
    }

    private Dependency createDependency(String groupId, String artifactId, String version, String scope) {
        Dependency dep = new Dependency();
        dep.setGroupId(groupId);
        dep.setArtifactId(artifactId);
        dep.setVersion(version);
        if (scope != null) {
            dep.setScope(scope);
        }
        return dep;
    }

    private void addCompilerPluginWithAnnotationProcessorPaths(
            MavenProject project, String groupId, String artifactId, String version) {
        Plugin compilerPlugin = createPlugin("org.apache.maven.plugins", "maven-compiler-plugin");

        Xpp3Dom config = new Xpp3Dom("configuration");
        Xpp3Dom paths = new Xpp3Dom("annotationProcessorPaths");
        Xpp3Dom path = new Xpp3Dom("path");

        Xpp3Dom gid = new Xpp3Dom("groupId");
        gid.setValue(groupId);
        path.addChild(gid);

        Xpp3Dom aid = new Xpp3Dom("artifactId");
        aid.setValue(artifactId);
        path.addChild(aid);

        Xpp3Dom ver = new Xpp3Dom("version");
        ver.setValue(version);
        path.addChild(ver);

        paths.addChild(path);
        config.addChild(paths);
        compilerPlugin.setConfiguration(config);

        project.getBuild().getPlugins().add(compilerPlugin);
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

    @SuppressWarnings("deprecation") // No non-deprecated constructor available in Maven 3.9.x
    private MavenSession createSession(MavenProject... projects) {
        return new MavenSession(
                null, new DefaultMavenExecutionRequest(), new DefaultMavenExecutionResult(), List.of(projects));
    }
}

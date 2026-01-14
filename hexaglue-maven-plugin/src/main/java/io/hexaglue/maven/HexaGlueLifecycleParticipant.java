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
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.project.MavenProject;

/**
 * Maven lifecycle participant that automatically binds hexaglue:generate to generate-sources.
 *
 * <p>When the HexaGlue Maven Plugin is declared with {@code <extensions>true</extensions>},
 * this participant automatically adds a default execution that binds the {@code generate}
 * goal to the {@code generate-sources} phase.
 *
 * <p>This eliminates the need for users to explicitly declare an {@code <executions>}
 * block in their pom.xml:
 *
 * <pre>{@code
 * <plugin>
 *     <groupId>io.hexaglue</groupId>
 *     <artifactId>hexaglue-maven-plugin</artifactId>
 *     <extensions>true</extensions>
 *     <configuration>
 *         <basePackage>com.example</basePackage>
 *     </configuration>
 * </plugin>
 * }</pre>
 *
 * @since 3.0.0
 */
@Named("hexaglue")
@Singleton
public class HexaGlueLifecycleParticipant extends AbstractMavenLifecycleParticipant {

    private static final String GROUP_ID = "io.hexaglue";
    private static final String ARTIFACT_ID = "hexaglue-maven-plugin";
    private static final String GOAL = "generate";
    private static final String PHASE = "generate-sources";
    private static final String EXECUTION_ID = "default-hexaglue-generate";

    @Override
    public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
        for (MavenProject project : session.getProjects()) {
            injectExecutionIfNeeded(project);
        }
    }

    private void injectExecutionIfNeeded(MavenProject project) {
        Plugin hexagluePlugin = findHexaGluePlugin(project);
        if (hexagluePlugin == null) {
            return;
        }

        // Check if there's already an execution with the generate goal
        boolean hasGenerateExecution = hexagluePlugin.getExecutions().stream()
                .anyMatch(exec -> exec.getGoals().contains(GOAL));

        if (hasGenerateExecution) {
            // User has explicitly configured executions, don't interfere
            return;
        }

        // Add default execution
        PluginExecution execution = new PluginExecution();
        execution.setId(EXECUTION_ID);
        execution.setPhase(PHASE);
        execution.addGoal(GOAL);

        // Inherit configuration from plugin level
        if (hexagluePlugin.getConfiguration() != null) {
            execution.setConfiguration(hexagluePlugin.getConfiguration());
        }

        hexagluePlugin.addExecution(execution);
    }

    private Plugin findHexaGluePlugin(MavenProject project) {
        return project.getBuildPlugins().stream()
                .filter(p -> GROUP_ID.equals(p.getGroupId()) && ARTIFACT_ID.equals(p.getArtifactId()))
                .findFirst()
                .orElse(null);
    }
}

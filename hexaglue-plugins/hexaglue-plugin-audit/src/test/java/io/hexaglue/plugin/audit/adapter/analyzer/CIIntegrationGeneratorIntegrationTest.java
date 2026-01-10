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

package io.hexaglue.plugin.audit.adapter.analyzer;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.plugin.audit.domain.model.CIConfiguration;
import io.hexaglue.plugin.audit.domain.model.CIPlatform;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link CIIntegrationGenerator}.
 *
 * <p>These tests validate the complete workflow of generating CI configurations
 * and demonstrate typical usage patterns for the generator.
 */
class CIIntegrationGeneratorIntegrationTest {

    private final CIIntegrationGenerator generator = new CIIntegrationGenerator();

    @Test
    void shouldGenerateCompleteGitHubActionsWorkflow() {
        // Given: A project requiring 85% health score
        int requiredHealthScore = 85;

        // When: Generating GitHub Actions configuration
        CIConfiguration config = generator.generate(CIPlatform.GITHUB_ACTIONS, requiredHealthScore);

        // Then: Configuration is ready to use
        assertThat(config.platform()).isEqualTo(CIPlatform.GITHUB_ACTIONS);
        assertThat(config.filename()).isEqualTo(".github/workflows/hexaglue-audit.yml");

        // Verify complete workflow structure
        String content = config.configContent();

        // Workflow metadata
        assertThat(content).contains("name: HexaGlue Architecture Audit");
        assertThat(content).contains("on:");
        assertThat(content).contains("push:");
        assertThat(content).contains("pull_request:");

        // Job configuration
        assertThat(content).contains("jobs:");
        assertThat(content).contains("audit:");
        assertThat(content).contains("runs-on: ubuntu-latest");

        // Build steps
        assertThat(content).contains("Checkout code");
        assertThat(content).contains("Set up JDK 17");
        assertThat(content).contains("Run HexaGlue Audit");
        assertThat(content).contains("Check Health Score");
        assertThat(content).contains("Upload Audit Report");
        assertThat(content).contains("Comment PR with Audit Summary");

        // Tool setup
        assertThat(content).contains("actions/checkout@v4");
        assertThat(content).contains("actions/setup-java@v4");
        assertThat(content).contains("java-version: '17'");
        assertThat(content).contains("distribution: 'temurin'");

        // Maven execution
        assertThat(content).contains("mvn clean compile hexaglue:audit");
        assertThat(content).contains("-Dhexaglue.report.format=json");

        // Health score validation
        assertThat(content).contains("jq -r '.healthScore'");
        assertThat(content).contains("if [ \"$SCORE\" -lt 85 ]");

        // Artifact upload
        assertThat(content).contains("actions/upload-artifact@v4");
        assertThat(content).contains("name: hexaglue-audit-report");
        assertThat(content).contains("target/hexaglue/audit-report.*");

        // PR comment
        assertThat(content).contains("github.event_name == 'pull_request'");
        assertThat(content).contains("actions/github-script@v7");

        // Print configuration for manual verification
        System.out.println("=== GitHub Actions Configuration ===");
        System.out.println(config.summary());
        System.out.println("\n" + content);
    }

    @Test
    void shouldGenerateCompleteGitLabCIPipeline() {
        // Given: A project requiring 75% health score
        int requiredHealthScore = 75;

        // When: Generating GitLab CI configuration
        CIConfiguration config = generator.generate(CIPlatform.GITLAB_CI, requiredHealthScore);

        // Then: Configuration is ready to use
        assertThat(config.platform()).isEqualTo(CIPlatform.GITLAB_CI);
        assertThat(config.filename()).isEqualTo(".gitlab-ci-hexaglue.yml");

        String content = config.configContent();

        // Pipeline structure
        assertThat(content).contains("stages:");
        assertThat(content).contains("- audit");

        // Job configuration
        assertThat(content).contains("hexaglue-audit:");
        assertThat(content).contains("stage: audit");
        assertThat(content).contains("image: maven:3.9-eclipse-temurin-17");

        // Build execution
        assertThat(content).contains("script:");
        assertThat(content).contains("mvn clean compile hexaglue:audit");

        // Health score validation
        assertThat(content).contains("if [ \"$SCORE\" -lt 75 ]");

        // Artifacts
        assertThat(content).contains("artifacts:");
        assertThat(content).contains("when: always");
        assertThat(content).contains("target/hexaglue/audit-report.*");

        // Rules
        assertThat(content).contains("rules:");
        assertThat(content).contains("CI_PIPELINE_SOURCE == \"merge_request_event\"");

        // Print configuration for manual verification
        System.out.println("=== GitLab CI Configuration ===");
        System.out.println(config.summary());
        System.out.println("\n" + content);
    }

    @Test
    void shouldGenerateCompleteJenkinsPipeline() {
        // Given: A project requiring 90% health score
        int requiredHealthScore = 90;

        // When: Generating Jenkins configuration
        CIConfiguration config = generator.generate(CIPlatform.JENKINS, requiredHealthScore);

        // Then: Configuration is ready to use
        assertThat(config.platform()).isEqualTo(CIPlatform.JENKINS);
        assertThat(config.filename()).isEqualTo("Jenkinsfile.hexaglue");

        String content = config.configContent();

        // Pipeline structure
        assertThat(content).contains("pipeline {");
        assertThat(content).contains("agent any");

        // Tools
        assertThat(content).contains("tools {");
        assertThat(content).contains("maven 'Maven 3.9'");
        assertThat(content).contains("jdk 'JDK 17'");

        // Stages
        assertThat(content).contains("stages {");
        assertThat(content).contains("stage('Checkout')");
        assertThat(content).contains("stage('Build and Audit')");
        assertThat(content).contains("stage('Validate Health Score')");

        // Build execution
        assertThat(content).contains("mvn clean compile hexaglue:audit");

        // Health score validation
        assertThat(content).contains("readJSON file:");
        assertThat(content).contains("if (healthScore < 90)");

        // Post actions
        assertThat(content).contains("post {");
        assertThat(content).contains("always {");
        assertThat(content).contains("archiveArtifacts");

        // Print configuration for manual verification
        System.out.println("=== Jenkins Pipeline Configuration ===");
        System.out.println(config.summary());
        System.out.println("\n" + content);
    }

    @Test
    void shouldGenerateConfigurationsForAllPlatforms() {
        // Given: A team requiring 80% health score across all CI platforms
        int minHealthScore = 80;

        // When: Generating configurations for all platforms
        List<CIConfiguration> configurations = generator.generateAll(minHealthScore);

        // Then: All platforms have configurations
        assertThat(configurations).hasSize(3);

        // Verify each platform
        assertThat(configurations)
                .extracting(CIConfiguration::platform)
                .containsExactlyInAnyOrder(CIPlatform.GITHUB_ACTIONS, CIPlatform.GITLAB_CI, CIPlatform.JENKINS);

        // All configurations include the threshold
        configurations.forEach(config -> {
            assertThat(config.configContent()).contains("80");
        });

        // Print summary of all configurations
        System.out.println("\n=== All CI Platform Configurations ===");
        configurations.forEach(config -> {
            System.out.println("\n" + config.summary());
            System.out.println("Content preview (first 200 chars):");
            String preview = config.configContent()
                    .substring(0, Math.min(200, config.configContent().length()));
            System.out.println(preview + "...");
        });
    }

    @Test
    void shouldGenerateConfigurationsWithDifferentThresholds() {
        // Given: Different projects with different quality requirements
        int strictThreshold = 95;
        int standardThreshold = 75;
        int lenientThreshold = 50;

        // When: Generating configurations with different thresholds
        CIConfiguration strictConfig = generator.generate(CIPlatform.GITHUB_ACTIONS, strictThreshold);
        CIConfiguration standardConfig = generator.generate(CIPlatform.GITHUB_ACTIONS, standardThreshold);
        CIConfiguration lenientConfig = generator.generate(CIPlatform.GITHUB_ACTIONS, lenientThreshold);

        // Then: Each configuration has the correct threshold
        assertThat(strictConfig.configContent()).contains("95");
        assertThat(standardConfig.configContent()).contains("75");
        assertThat(lenientConfig.configContent()).contains("50");

        // Verify the threshold is used in the validation step
        assertThat(strictConfig.configContent()).contains("if [ \"$SCORE\" -lt 95 ]");
        assertThat(standardConfig.configContent()).contains("if [ \"$SCORE\" -lt 75 ]");
        assertThat(lenientConfig.configContent()).contains("if [ \"$SCORE\" -lt 50 ]");

        // Print comparison
        System.out.println("\n=== Threshold Comparison ===");
        System.out.println("Strict (95%):   Threshold check includes '95'");
        System.out.println("Standard (75%): Threshold check includes '75'");
        System.out.println("Lenient (50%):  Threshold check includes '50'");
    }

    @Test
    void shouldGenerateReadyToUseConfigurations() {
        // Given: A real-world scenario - team wants to set up CI for architecture audits
        int minHealthScore = 80;

        // When: Generating configurations
        List<CIConfiguration> configs = generator.generateAll(minHealthScore);

        // Then: Each configuration can be saved to the specified filename
        System.out.println("\n=== Ready-to-Use CI Configurations ===");
        System.out.println("\nTo integrate HexaGlue audits into your CI/CD pipeline:");
        System.out.println("1. Choose your CI platform");
        System.out.println("2. Copy the configuration to the specified file");
        System.out.println("3. Commit and push\n");

        configs.forEach(config -> {
            System.out.printf("Platform: %s%n", config.platform().displayName());
            System.out.printf("  Save to:     %s%n", config.filename());
            System.out.printf("  Description: %s%n", config.description());
            System.out.printf(
                    "  Size:        %d bytes%n", config.configContent().length());
            System.out.println();

            // Verify configuration is complete and non-empty
            assertThat(config.configContent()).hasSizeGreaterThan(500); // Meaningful configuration
            assertThat(config.filename()).isNotBlank();
            assertThat(config.description()).isNotBlank();
        });
    }
}

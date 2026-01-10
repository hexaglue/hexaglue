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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.hexaglue.plugin.audit.domain.model.CIConfiguration;
import io.hexaglue.plugin.audit.domain.model.CIPlatform;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for {@link CIIntegrationGenerator}.
 */
class CIIntegrationGeneratorTest {

    private final CIIntegrationGenerator generator = new CIIntegrationGenerator();

    @Test
    void shouldRejectNullPlatform() {
        // When/Then
        assertThatThrownBy(() -> generator.generate(null, 75))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("platform required");
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, -10, 101, 200})
    void shouldRejectInvalidHealthScore(int invalidScore) {
        // When/Then
        assertThatThrownBy(() -> generator.generate(CIPlatform.GITHUB_ACTIONS, invalidScore))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minHealthScore must be between 0 and 100");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 50, 75, 100})
    void shouldAcceptValidHealthScore(int validScore) {
        // When
        CIConfiguration config = generator.generate(CIPlatform.GITHUB_ACTIONS, validScore);

        // Then
        assertThat(config).isNotNull();
        assertThat(config.configContent()).contains(String.valueOf(validScore));
    }

    @ParameterizedTest
    @EnumSource(CIPlatform.class)
    void shouldGenerateConfiguration_forAllPlatforms(CIPlatform platform) {
        // When
        CIConfiguration config = generator.generate(platform, 75);

        // Then
        assertThat(config).isNotNull();
        assertThat(config.platform()).isEqualTo(platform);
        assertThat(config.configContent()).isNotBlank();
        assertThat(config.filename()).isNotBlank();
        assertThat(config.description()).isNotBlank();
    }

    @Test
    void shouldGenerateGitHubActionsConfiguration() {
        // When
        CIConfiguration config = generator.generate(CIPlatform.GITHUB_ACTIONS, 80);

        // Then
        assertThat(config.platform()).isEqualTo(CIPlatform.GITHUB_ACTIONS);
        assertThat(config.filename()).isEqualTo(".github/workflows/hexaglue-audit.yml");

        String content = config.configContent();
        assertThat(content).contains("name: HexaGlue Architecture Audit");
        assertThat(content).contains("on:");
        assertThat(content).contains("jobs:");
        assertThat(content).contains("audit:");
        assertThat(content).contains("runs-on: ubuntu-latest");
        assertThat(content).contains("uses: actions/checkout@v4");
        assertThat(content).contains("uses: actions/setup-java@v4");
        assertThat(content).contains("java-version: '17'");
        assertThat(content).contains("distribution: 'temurin'");
        assertThat(content).contains("mvn clean compile hexaglue:audit");
        assertThat(content).contains("-Dhexaglue.report.format=json");
        assertThat(content).contains("target/hexaglue/audit-report.json");
        assertThat(content).contains("jq -r '.healthScore'");
        assertThat(content).contains("80"); // min health score
        assertThat(content).contains("uses: actions/upload-artifact@v4");
        assertThat(content).contains("name: hexaglue-audit-report");
        assertThat(content).contains("github.event_name == 'pull_request'");
        assertThat(content).contains("actions/github-script@v7");
    }

    @Test
    void shouldGenerateGitHubActionsConfiguration_withCorrectThreshold() {
        // When
        CIConfiguration config = generator.generate(CIPlatform.GITHUB_ACTIONS, 90);

        // Then
        String content = config.configContent();
        assertThat(content).contains("if [ \"$SCORE\" -lt 90 ]");
        assertThat(content).contains("threshold: 90");
    }

    @Test
    void shouldGenerateGitLabCIConfiguration() {
        // When
        CIConfiguration config = generator.generate(CIPlatform.GITLAB_CI, 75);

        // Then
        assertThat(config.platform()).isEqualTo(CIPlatform.GITLAB_CI);
        assertThat(config.filename()).isEqualTo(".gitlab-ci-hexaglue.yml");

        String content = config.configContent();
        assertThat(content).contains("stages:");
        assertThat(content).contains("- audit");
        assertThat(content).contains("hexaglue-audit:");
        assertThat(content).contains("stage: audit");
        assertThat(content).contains("image: maven:3.9-eclipse-temurin-17");
        assertThat(content).contains("MAVEN_OPTS");
        assertThat(content).contains("cache:");
        assertThat(content).contains(".m2/repository");
        assertThat(content).contains("script:");
        assertThat(content).contains("mvn clean compile hexaglue:audit");
        assertThat(content).contains("-Dhexaglue.report.format=json");
        assertThat(content).contains("target/hexaglue/audit-report.json");
        assertThat(content).contains("75"); // min health score
        assertThat(content).contains("artifacts:");
        assertThat(content).contains("when: always");
        assertThat(content).contains("expire_in: 30 days");
        assertThat(content).contains("rules:");
        assertThat(content).contains("CI_PIPELINE_SOURCE == \"merge_request_event\"");
        assertThat(content).contains("CI_DEFAULT_BRANCH");
    }

    @Test
    void shouldGenerateGitLabCIConfiguration_withCorrectThreshold() {
        // When
        CIConfiguration config = generator.generate(CIPlatform.GITLAB_CI, 85);

        // Then
        String content = config.configContent();
        assertThat(content).contains("if [ \"$SCORE\" -lt 85 ]");
        assertThat(content).contains("threshold 85");
    }

    @Test
    void shouldGenerateJenkinsConfiguration() {
        // When
        CIConfiguration config = generator.generate(CIPlatform.JENKINS, 70);

        // Then
        assertThat(config.platform()).isEqualTo(CIPlatform.JENKINS);
        assertThat(config.filename()).isEqualTo("Jenkinsfile.hexaglue");

        String content = config.configContent();
        assertThat(content).contains("pipeline {");
        assertThat(content).contains("agent any");
        assertThat(content).contains("tools {");
        assertThat(content).contains("maven 'Maven 3.9'");
        assertThat(content).contains("jdk 'JDK 17'");
        assertThat(content).contains("stages {");
        assertThat(content).contains("stage('Checkout')");
        assertThat(content).contains("stage('Build and Audit')");
        assertThat(content).contains("stage('Validate Health Score')");
        assertThat(content).contains("checkout scm");
        assertThat(content).contains("mvn clean compile hexaglue:audit");
        assertThat(content).contains("-Dhexaglue.report.format=json");
        assertThat(content).contains("target/hexaglue/audit-report.json");
        assertThat(content).contains("readJSON file:");
        assertThat(content).contains("healthScore");
        assertThat(content).contains("70"); // min health score
        assertThat(content).contains("post {");
        assertThat(content).contains("always {");
        assertThat(content).contains("archiveArtifacts");
        assertThat(content).contains("target/hexaglue/audit-report.*");
    }

    @Test
    void shouldGenerateJenkinsConfiguration_withCorrectThreshold() {
        // When
        CIConfiguration config = generator.generate(CIPlatform.JENKINS, 95);

        // Then
        String content = config.configContent();
        assertThat(content).contains("if (healthScore < 95)");
        assertThat(content).contains("threshold 95");
    }

    @Test
    void shouldIncludeReportPath_inAllConfigurations() {
        // Given
        String expectedPath = "target/hexaglue/audit-report.json";

        // When/Then
        for (CIPlatform platform : CIPlatform.values()) {
            CIConfiguration config = generator.generate(platform, 75);
            assertThat(config.configContent()).as("Report path for " + platform).contains(expectedPath);
        }
    }

    @Test
    void shouldIncludeMavenCommand_inAllConfigurations() {
        // Given
        String mavenCommand = "mvn clean compile hexaglue:audit";
        String reportFormat = "-Dhexaglue.report.format=json";

        // When/Then
        for (CIPlatform platform : CIPlatform.values()) {
            CIConfiguration config = generator.generate(platform, 75);
            assertThat(config.configContent())
                    .as("Maven command for " + platform)
                    .contains(mavenCommand)
                    .contains(reportFormat);
        }
    }

    @Test
    void shouldIncludeHealthScoreCheck_inAllConfigurations() {
        // Given
        int minScore = 85;

        // When/Then
        for (CIPlatform platform : CIPlatform.values()) {
            CIConfiguration config = generator.generate(platform, minScore);
            assertThat(config.configContent())
                    .as("Health score check for " + platform)
                    .contains(String.valueOf(minScore));
        }
    }

    @Test
    void shouldGenerateAllPlatforms() {
        // When
        List<CIConfiguration> configs = generator.generateAll(80);

        // Then
        assertThat(configs).hasSize(CIPlatform.values().length);

        // Verify all platforms are present
        assertThat(configs).extracting(CIConfiguration::platform).containsExactlyInAnyOrder(CIPlatform.values());

        // Verify all configurations have content
        assertThat(configs).allMatch(config -> !config.configContent().isBlank());
        assertThat(configs).allMatch(config -> !config.filename().isBlank());
        assertThat(configs).allMatch(config -> !config.description().isBlank());
    }

    @Test
    void shouldGenerateAllPlatforms_withSameThreshold() {
        // Given
        int minScore = 90;

        // When
        List<CIConfiguration> configs = generator.generateAll(minScore);

        // Then
        assertThat(configs).allMatch(config -> config.configContent().contains(String.valueOf(minScore)));
    }

    @ParameterizedTest
    @ValueSource(ints = {-5, 105})
    void shouldRejectInvalidHealthScore_inGenerateAll(int invalidScore) {
        // When/Then
        assertThatThrownBy(() -> generator.generateAll(invalidScore))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minHealthScore must be between 0 and 100");
    }

    @Test
    void shouldGenerateValidYamlStructure_forGitHubActions() {
        // When
        CIConfiguration config = generator.generate(CIPlatform.GITHUB_ACTIONS, 75);

        // Then - Check basic YAML structure
        String content = config.configContent();
        assertThat(content).contains("name:");
        assertThat(content).contains("on:");
        assertThat(content).contains("jobs:");
        assertThat(content).contains("steps:");
        assertThat(content).contains("- name:");
        assertThat(content).contains("uses:");
        assertThat(content).contains("run:");
        assertThat(content).contains("with:");

        // Verify indentation is consistent (YAML requires proper indentation)
        assertThat(content).containsPattern("(?m)^  ");
        assertThat(content).containsPattern("(?m)^    ");
    }

    @Test
    void shouldGenerateValidYamlStructure_forGitLabCI() {
        // When
        CIConfiguration config = generator.generate(CIPlatform.GITLAB_CI, 75);

        // Then - Check basic YAML structure
        String content = config.configContent();
        assertThat(content).contains("stages:");
        assertThat(content).contains("stage:");
        assertThat(content).contains("image:");
        assertThat(content).contains("script:");
        assertThat(content).contains("artifacts:");
        assertThat(content).contains("rules:");

        // Verify indentation is consistent
        assertThat(content).containsPattern("(?m)^  ");
        assertThat(content).containsPattern("(?m)^    ");
    }

    @Test
    void shouldGenerateValidGroovyStructure_forJenkins() {
        // When
        CIConfiguration config = generator.generate(CIPlatform.JENKINS, 75);

        // Then - Check basic Groovy/Jenkinsfile structure
        String content = config.configContent();
        assertThat(content).contains("pipeline {");
        assertThat(content).contains("agent");
        assertThat(content).contains("tools {");
        assertThat(content).contains("stages {");
        assertThat(content).contains("stage(");
        assertThat(content).contains("steps {");
        assertThat(content).contains("script {");
        assertThat(content).contains("post {");

        // Verify closing braces
        long openBraces = content.chars().filter(ch -> ch == '{').count();
        long closeBraces = content.chars().filter(ch -> ch == '}').count();
        assertThat(openBraces).isEqualTo(closeBraces);
    }

    @Test
    void shouldUseCorrectJavaVersion_inAllConfigurations() {
        // When/Then
        for (CIPlatform platform : CIPlatform.values()) {
            CIConfiguration config = generator.generate(platform, 75);
            assertThat(config.configContent())
                    .as("Java version for " + platform)
                    .containsAnyOf("java-version: '17'", "JDK 17", "temurin-17");
        }
    }

    @Test
    void shouldIncludeArtifactStorage_inAllConfigurations() {
        // When/Then
        for (CIPlatform platform : CIPlatform.values()) {
            CIConfiguration config = generator.generate(platform, 75);
            String content = config.configContent();

            assertThat(content)
                    .as("Artifact storage for " + platform)
                    .containsAnyOf("upload-artifact", "artifacts:", "archiveArtifacts");
        }
    }

    @Test
    void shouldGenerateConfiguration_withZeroThreshold() {
        // When
        CIConfiguration config = generator.generate(CIPlatform.GITHUB_ACTIONS, 0);

        // Then
        assertThat(config.configContent()).contains("0");
    }

    @Test
    void shouldGenerateConfiguration_withMaximumThreshold() {
        // When
        CIConfiguration config = generator.generate(CIPlatform.GITHUB_ACTIONS, 100);

        // Then
        assertThat(config.configContent()).contains("100");
    }

    @Test
    void shouldGenerateUniqueFilenames_forDifferentPlatforms() {
        // When
        List<CIConfiguration> configs = generator.generateAll(75);

        // Then
        List<String> filenames = configs.stream().map(CIConfiguration::filename).toList();

        assertThat(filenames).doesNotHaveDuplicates();
    }

    @Test
    void shouldGenerateDescriptiveMessages_inAllConfigurations() {
        // When/Then
        for (CIPlatform platform : CIPlatform.values()) {
            CIConfiguration config = generator.generate(platform, 75);

            assertThat(config.description())
                    .as("Description for " + platform)
                    .isNotBlank()
                    .contains("HexaGlue")
                    .contains("audit");
        }
    }

    @Test
    void shouldIncludeFailureHandling_inAllConfigurations() {
        // Given
        int minScore = 80;

        // When/Then
        for (CIPlatform platform : CIPlatform.values()) {
            CIConfiguration config = generator.generate(platform, minScore);
            String content = config.configContent();

            // All configurations should handle failure scenarios
            assertThat(content).as("Failure handling for " + platform).containsAnyOf("exit 1", "error(", "FAILED");
        }
    }

    @Test
    void shouldIncludeSuccessMessage_inAllConfigurations() {
        // When/Then
        for (CIPlatform platform : CIPlatform.values()) {
            CIConfiguration config = generator.generate(platform, 75);
            String content = config.configContent();

            assertThat(content).as("Success message for " + platform).containsAnyOf("SUCCESS", "success", "passed");
        }
    }

    @Test
    void shouldValidateSummary() {
        // When
        CIConfiguration config = generator.generate(CIPlatform.GITHUB_ACTIONS, 75);

        // Then
        String summary = config.summary();
        assertThat(summary).contains("GitHub Actions");
        assertThat(summary).contains(config.filename());
        assertThat(summary).contains(config.description());
    }
}

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

import io.hexaglue.plugin.audit.domain.model.CIConfiguration;
import io.hexaglue.plugin.audit.domain.model.CIPlatform;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Generates ready-to-use CI/CD integration configurations for HexaGlue audits.
 *
 * <p>This generator produces platform-specific configuration files (YAML or Groovy)
 * that integrate HexaGlue architecture audits into continuous integration pipelines.
 * Each generated configuration includes:
 * <ul>
 *   <li>Java environment setup (JDK 17)</li>
 *   <li>Maven build and HexaGlue audit execution</li>
 *   <li>JSON report generation for machine-readable results</li>
 *   <li>Health score threshold validation</li>
 *   <li>Artifact storage for audit reports</li>
 *   <li>Optional PR/MR comment integration</li>
 * </ul>
 *
 * <p><strong>Supported platforms:</strong>
 * <ul>
 *   <li>{@link CIPlatform#GITHUB_ACTIONS GitHub Actions} - YAML workflow with artifacts and PR comments</li>
 *   <li>{@link CIPlatform#GITLAB_CI GitLab CI} - YAML pipeline with artifacts and MR notes</li>
 *   <li>{@link CIPlatform#JENKINS Jenkins} - Declarative pipeline with post-build actions</li>
 * </ul>
 *
 * <p><strong>Health score validation:</strong>
 * All configurations validate that the audit health score meets or exceeds the
 * specified minimum threshold. Builds fail if the score is below the threshold,
 * preventing merging of code with unacceptable architectural violations.
 *
 * <p><strong>Example usage:</strong>
 * <pre>{@code
 * CIIntegrationGenerator generator = new CIIntegrationGenerator();
 *
 * // Generate configuration for a specific platform
 * CIConfiguration config = generator.generate(CIPlatform.GITHUB_ACTIONS, 75);
 * Files.writeString(Path.of(config.filename()), config.configContent());
 *
 * // Generate configurations for all platforms
 * List<CIConfiguration> allConfigs = generator.generateAll(80);
 * allConfigs.forEach(cfg -> System.out.println(cfg.summary()));
 * }</pre>
 *
 * @since 1.0.0
 */
public class CIIntegrationGenerator {

    private static final String REPORT_PATH = "target/hexaglue/audit-report.json";

    /**
     * Generates a CI/CD configuration for the specified platform.
     *
     * <p>The generated configuration includes all necessary steps to run the
     * HexaGlue audit and validate the results against the minimum health score
     * threshold.
     *
     * @param platform       the CI/CD platform to generate configuration for
     * @param minHealthScore the minimum acceptable health score (0-100)
     * @return the generated CI configuration
     * @throws NullPointerException     if platform is null
     * @throws IllegalArgumentException if minHealthScore is not in range [0, 100]
     */
    public CIConfiguration generate(CIPlatform platform, int minHealthScore) {
        Objects.requireNonNull(platform, "platform required");
        validateHealthScore(minHealthScore);

        return switch (platform) {
            case GITHUB_ACTIONS -> generateGitHubActions(minHealthScore);
            case GITLAB_CI -> generateGitLabCI(minHealthScore);
            case JENKINS -> generateJenkins(minHealthScore);
        };
    }

    /**
     * Generates CI/CD configurations for all supported platforms.
     *
     * <p>This convenience method generates configurations for every platform
     * defined in {@link CIPlatform}, using the same minimum health score
     * threshold for all.
     *
     * @param minHealthScore the minimum acceptable health score (0-100)
     * @return list of CI configurations, one per platform
     * @throws IllegalArgumentException if minHealthScore is not in range [0, 100]
     */
    public List<CIConfiguration> generateAll(int minHealthScore) {
        validateHealthScore(minHealthScore);

        return Arrays.stream(CIPlatform.values())
                .map(platform -> generate(platform, minHealthScore))
                .toList();
    }

    /**
     * Generates a GitHub Actions workflow configuration.
     *
     * <p>The workflow:
     * <ul>
     *   <li>Triggers on push and pull_request events</li>
     *   <li>Sets up Java 17 (Temurin distribution)</li>
     *   <li>Runs Maven build and HexaGlue audit with JSON report</li>
     *   <li>Validates health score against threshold</li>
     *   <li>Uploads audit report as workflow artifact</li>
     *   <li>Posts PR comment with audit summary (optional, requires GitHub token)</li>
     * </ul>
     *
     * @param minHealthScore minimum acceptable health score
     * @return GitHub Actions workflow configuration
     */
    private CIConfiguration generateGitHubActions(int minHealthScore) {
        String content = String.format(
                """
                name: HexaGlue Architecture Audit

                on:
                  push:
                    branches: [ main, develop ]
                  pull_request:
                    branches: [ main, develop ]

                jobs:
                  audit:
                    name: Architecture Audit
                    runs-on: ubuntu-latest

                    steps:
                      - name: Checkout code
                        uses: actions/checkout@v4

                      - name: Set up JDK 17
                        uses: actions/setup-java@v4
                        with:
                          java-version: '17'
                          distribution: 'temurin'
                          cache: 'maven'

                      - name: Run HexaGlue Audit
                        run: |
                          mvn clean compile hexaglue:audit -Dhexaglue.report.format=json

                      - name: Check Health Score
                        run: |
                          if [ ! -f "%s" ]; then
                            echo "Error: Audit report not found at %s"
                            exit 1
                          fi

                          SCORE=$(jq -r '.healthScore' %s)
                          echo "Health Score: $SCORE"

                          if [ "$SCORE" -lt %d ]; then
                            echo "FAILED: Health score $SCORE is below threshold %d"
                            exit 1
                          fi

                          echo "SUCCESS: Health score $SCORE meets threshold %d"

                      - name: Upload Audit Report
                        uses: actions/upload-artifact@v4
                        if: always()
                        with:
                          name: hexaglue-audit-report
                          path: target/hexaglue/audit-report.*
                          retention-days: 30

                      - name: Comment PR with Audit Summary
                        if: github.event_name == 'pull_request'
                        uses: actions/github-script@v7
                        with:
                          script: |
                            const fs = require('fs');
                            const report = JSON.parse(fs.readFileSync('%s', 'utf8'));

                            const comment = `## HexaGlue Architecture Audit

                            **Health Score:** ${report.healthScore}/100 (threshold: %d)
                            **Status:** ${report.healthScore >= %d ? '✅ PASSED' : '❌ FAILED'}

                            **Violations:**
                            - Critical: ${report.summary.violationsBySeverity.CRITICAL || 0}
                            - Major: ${report.summary.violationsBySeverity.MAJOR || 0}
                            - Minor: ${report.summary.violationsBySeverity.MINOR || 0}

                            [View detailed report in artifacts](${{ github.server_url }}/${{ github.repository }}/actions/runs/${{ github.run_id }})`;

                            github.rest.issues.createComment({
                              issue_number: context.issue.number,
                              owner: context.repo.owner,
                              repo: context.repo.repo,
                              body: comment
                            });
                """,
                REPORT_PATH,
                REPORT_PATH,
                REPORT_PATH,
                minHealthScore,
                minHealthScore,
                minHealthScore,
                REPORT_PATH,
                minHealthScore,
                minHealthScore);

        return new CIConfiguration(
                CIPlatform.GITHUB_ACTIONS,
                content,
                CIPlatform.GITHUB_ACTIONS.defaultFilename(),
                "GitHub Actions workflow that runs HexaGlue audit on push and pull requests, "
                        + "validates health score, and uploads reports as artifacts");
    }

    /**
     * Generates a GitLab CI/CD pipeline configuration.
     *
     * <p>The pipeline:
     * <ul>
     *   <li>Defines 'audit' stage</li>
     *   <li>Uses Maven Docker image with JDK 17</li>
     *   <li>Runs Maven build and HexaGlue audit with JSON report</li>
     *   <li>Validates health score against threshold</li>
     *   <li>Stores audit reports as GitLab artifacts</li>
     *   <li>Reports can be viewed in pipeline artifacts</li>
     * </ul>
     *
     * @param minHealthScore minimum acceptable health score
     * @return GitLab CI pipeline configuration
     */
    private CIConfiguration generateGitLabCI(int minHealthScore) {
        String content = String.format(
                """
                stages:
                  - audit

                hexaglue-audit:
                  stage: audit
                  image: maven:3.9-eclipse-temurin-17

                  variables:
                    MAVEN_OPTS: "-Dmaven.repo.local=$CI_PROJECT_DIR/.m2/repository"

                  cache:
                    paths:
                      - .m2/repository

                  script:
                    - echo "Running HexaGlue Architecture Audit"
                    - mvn clean compile hexaglue:audit -Dhexaglue.report.format=json

                    - |
                      if [ ! -f "%s" ]; then
                        echo "Error: Audit report not found at %s"
                        exit 1
                      fi

                    - |
                      SCORE=$(grep -oP '"healthScore"\\s*:\\s*\\K[0-9]+' %s)
                      echo "Health Score: $SCORE"

                      if [ "$SCORE" -lt %d ]; then
                        echo "FAILED: Health score $SCORE is below threshold %d"
                        exit 1
                      fi

                      echo "SUCCESS: Health score $SCORE meets threshold %d"

                  artifacts:
                    when: always
                    paths:
                      - target/hexaglue/audit-report.*
                    reports:
                      junit: target/surefire-reports/TEST-*.xml
                    expire_in: 30 days

                  rules:
                    - if: '$CI_PIPELINE_SOURCE == "merge_request_event"'
                    - if: '$CI_COMMIT_BRANCH == $CI_DEFAULT_BRANCH'
                    - if: '$CI_COMMIT_BRANCH == "develop"'
                """, REPORT_PATH, REPORT_PATH, REPORT_PATH, minHealthScore, minHealthScore, minHealthScore);

        return new CIConfiguration(
                CIPlatform.GITLAB_CI,
                content,
                CIPlatform.GITLAB_CI.defaultFilename(),
                "GitLab CI pipeline that runs HexaGlue audit on merge requests and main branches, "
                        + "validates health score, and stores reports as artifacts");
    }

    /**
     * Generates a Jenkins declarative pipeline configuration.
     *
     * <p>The pipeline:
     * <ul>
     *   <li>Uses any available agent</li>
     *   <li>Configures Maven and JDK 17 tools</li>
     *   <li>Runs Maven build and HexaGlue audit with JSON report</li>
     *   <li>Validates health score against threshold</li>
     *   <li>Archives audit reports as build artifacts</li>
     *   <li>Marks build unstable/failed based on health score</li>
     * </ul>
     *
     * @param minHealthScore minimum acceptable health score
     * @return Jenkins declarative pipeline configuration
     */
    private CIConfiguration generateJenkins(int minHealthScore) {
        String content = String.format(
                """
                pipeline {
                    agent any

                    tools {
                        maven 'Maven 3.9'
                        jdk 'JDK 17'
                    }

                    stages {
                        stage('Checkout') {
                            steps {
                                checkout scm
                            }
                        }

                        stage('Build and Audit') {
                            steps {
                                script {
                                    echo 'Running HexaGlue Architecture Audit'
                                    sh 'mvn clean compile hexaglue:audit -Dhexaglue.report.format=json'
                                }
                            }
                        }

                        stage('Validate Health Score') {
                            steps {
                                script {
                                    if (!fileExists('%s')) {
                                        error("Audit report not found at %s")
                                    }

                                    def report = readJSON file: '%s'
                                    def healthScore = report.healthScore

                                    echo "Health Score: ${healthScore}"

                                    if (healthScore < %d) {
                                        error("FAILED: Health score ${healthScore} is below threshold %d")
                                    }

                                    echo "SUCCESS: Health score ${healthScore} meets threshold %d"
                                }
                            }
                        }
                    }

                    post {
                        always {
                            archiveArtifacts artifacts: 'target/hexaglue/audit-report.*',
                                             allowEmptyArchive: true,
                                             fingerprint: true
                        }
                        success {
                            echo 'HexaGlue audit passed successfully'
                        }
                        failure {
                            echo 'HexaGlue audit failed - check the report for details'
                        }
                    }
                }
                """, REPORT_PATH, REPORT_PATH, REPORT_PATH, minHealthScore, minHealthScore, minHealthScore);

        return new CIConfiguration(
                CIPlatform.JENKINS,
                content,
                CIPlatform.JENKINS.defaultFilename(),
                "Jenkins declarative pipeline that runs HexaGlue audit, validates health score, "
                        + "and archives reports as build artifacts");
    }

    /**
     * Validates that the health score is within the valid range [0, 100].
     *
     * @param minHealthScore the health score to validate
     * @throws IllegalArgumentException if the score is out of range
     */
    private void validateHealthScore(int minHealthScore) {
        if (minHealthScore < 0 || minHealthScore > 100) {
            throw new IllegalArgumentException("minHealthScore must be between 0 and 100, got: " + minHealthScore);
        }
    }
}

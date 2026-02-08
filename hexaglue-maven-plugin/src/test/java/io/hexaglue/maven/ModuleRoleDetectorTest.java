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

import io.hexaglue.arch.model.index.ModuleRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ModuleRoleDetector}.
 *
 * @since 5.0.0
 */
@DisplayName("ModuleRoleDetector")
class ModuleRoleDetectorTest {

    @Nested
    @DisplayName("DOMAIN role detection")
    class DomainRole {

        @Test
        @DisplayName("should detect -core suffix")
        void shouldDetectCoreSuffix() {
            assertThat(ModuleRoleDetector.detect("banking-core")).contains(ModuleRole.DOMAIN);
        }

        @Test
        @DisplayName("should detect -domain suffix")
        void shouldDetectDomainSuffix() {
            assertThat(ModuleRoleDetector.detect("banking-domain")).contains(ModuleRole.DOMAIN);
        }

        @Test
        @DisplayName("should detect -model suffix")
        void shouldDetectModelSuffix() {
            assertThat(ModuleRoleDetector.detect("banking-model")).contains(ModuleRole.DOMAIN);
        }

        @Test
        @DisplayName("should detect exact artifactId without prefix")
        void shouldDetectExactArtifactId() {
            assertThat(ModuleRoleDetector.detect("core")).contains(ModuleRole.DOMAIN);
            assertThat(ModuleRoleDetector.detect("domain")).contains(ModuleRole.DOMAIN);
        }
    }

    @Nested
    @DisplayName("INFRASTRUCTURE role detection")
    class InfrastructureRole {

        @Test
        @DisplayName("should detect -persistence suffix")
        void shouldDetectPersistenceSuffix() {
            assertThat(ModuleRoleDetector.detect("banking-persistence")).contains(ModuleRole.INFRASTRUCTURE);
        }

        @Test
        @DisplayName("should detect -infrastructure suffix")
        void shouldDetectInfrastructureSuffix() {
            assertThat(ModuleRoleDetector.detect("banking-infrastructure")).contains(ModuleRole.INFRASTRUCTURE);
        }

        @Test
        @DisplayName("should detect -infra suffix")
        void shouldDetectInfraSuffix() {
            assertThat(ModuleRoleDetector.detect("banking-infra")).contains(ModuleRole.INFRASTRUCTURE);
        }

        @Test
        @DisplayName("should detect -db suffix")
        void shouldDetectDbSuffix() {
            assertThat(ModuleRoleDetector.detect("banking-db")).contains(ModuleRole.INFRASTRUCTURE);
        }

        @Test
        @DisplayName("should detect -jpa suffix")
        void shouldDetectJpaSuffix() {
            assertThat(ModuleRoleDetector.detect("banking-jpa")).contains(ModuleRole.INFRASTRUCTURE);
        }

        @Test
        @DisplayName("should prefer longest suffix: -infrastructure over -infra")
        void shouldPreferLongestSuffix() {
            // Both -infrastructure and -infra match INFRASTRUCTURE, but -infrastructure is longer
            assertThat(ModuleRoleDetector.detect("my-infrastructure")).contains(ModuleRole.INFRASTRUCTURE);
        }
    }

    @Nested
    @DisplayName("APPLICATION role detection")
    class ApplicationRole {

        @Test
        @DisplayName("should detect -service suffix")
        void shouldDetectServiceSuffix() {
            assertThat(ModuleRoleDetector.detect("banking-service")).contains(ModuleRole.APPLICATION);
        }

        @Test
        @DisplayName("should detect -application suffix")
        void shouldDetectApplicationSuffix() {
            assertThat(ModuleRoleDetector.detect("banking-application")).contains(ModuleRole.APPLICATION);
        }

        @Test
        @DisplayName("should detect -usecases suffix")
        void shouldDetectUsecasesSuffix() {
            assertThat(ModuleRoleDetector.detect("banking-usecases")).contains(ModuleRole.APPLICATION);
        }
    }

    @Nested
    @DisplayName("API role detection")
    class ApiRole {

        @Test
        @DisplayName("should detect -api suffix")
        void shouldDetectApiSuffix() {
            assertThat(ModuleRoleDetector.detect("banking-api")).contains(ModuleRole.API);
        }

        @Test
        @DisplayName("should detect -rest suffix")
        void shouldDetectRestSuffix() {
            assertThat(ModuleRoleDetector.detect("banking-rest")).contains(ModuleRole.API);
        }

        @Test
        @DisplayName("should detect -web suffix")
        void shouldDetectWebSuffix() {
            assertThat(ModuleRoleDetector.detect("banking-web")).contains(ModuleRole.API);
        }

        @Test
        @DisplayName("should detect -gateway suffix")
        void shouldDetectGatewaySuffix() {
            assertThat(ModuleRoleDetector.detect("banking-gateway")).contains(ModuleRole.API);
        }
    }

    @Nested
    @DisplayName("ASSEMBLY role detection")
    class AssemblyRole {

        @Test
        @DisplayName("should detect -app suffix")
        void shouldDetectAppSuffix() {
            assertThat(ModuleRoleDetector.detect("banking-app")).contains(ModuleRole.ASSEMBLY);
        }

        @Test
        @DisplayName("should detect -boot suffix")
        void shouldDetectBootSuffix() {
            assertThat(ModuleRoleDetector.detect("banking-boot")).contains(ModuleRole.ASSEMBLY);
        }

        @Test
        @DisplayName("should detect -bootstrap suffix")
        void shouldDetectBootstrapSuffix() {
            assertThat(ModuleRoleDetector.detect("banking-bootstrap")).contains(ModuleRole.ASSEMBLY);
        }

        @Test
        @DisplayName("should detect -starter suffix")
        void shouldDetectStarterSuffix() {
            assertThat(ModuleRoleDetector.detect("banking-starter")).contains(ModuleRole.ASSEMBLY);
        }
    }

    @Nested
    @DisplayName("SHARED role detection")
    class SharedRole {

        @Test
        @DisplayName("should detect -shared suffix")
        void shouldDetectSharedSuffix() {
            assertThat(ModuleRoleDetector.detect("banking-shared")).contains(ModuleRole.SHARED);
        }

        @Test
        @DisplayName("should detect -common suffix")
        void shouldDetectCommonSuffix() {
            assertThat(ModuleRoleDetector.detect("banking-common")).contains(ModuleRole.SHARED);
        }

        @Test
        @DisplayName("should detect -util suffix")
        void shouldDetectUtilSuffix() {
            assertThat(ModuleRoleDetector.detect("banking-util")).contains(ModuleRole.SHARED);
        }

        @Test
        @DisplayName("should detect -utils suffix")
        void shouldDetectUtilsSuffix() {
            assertThat(ModuleRoleDetector.detect("banking-utils")).contains(ModuleRole.SHARED);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("should return empty for null artifactId")
        void shouldReturnEmptyForNull() {
            assertThat(ModuleRoleDetector.detect(null)).isEmpty();
        }

        @Test
        @DisplayName("should return empty for blank artifactId")
        void shouldReturnEmptyForBlank() {
            assertThat(ModuleRoleDetector.detect("")).isEmpty();
            assertThat(ModuleRoleDetector.detect("   ")).isEmpty();
        }

        @Test
        @DisplayName("should return empty for unrecognized artifactId")
        void shouldReturnEmptyForUnrecognized() {
            assertThat(ModuleRoleDetector.detect("banking")).isEmpty();
            assertThat(ModuleRoleDetector.detect("my-project")).isEmpty();
        }

        @Test
        @DisplayName("should be case-insensitive")
        void shouldBeCaseInsensitive() {
            assertThat(ModuleRoleDetector.detect("banking-CORE")).contains(ModuleRole.DOMAIN);
            assertThat(ModuleRoleDetector.detect("banking-Core")).contains(ModuleRole.DOMAIN);
            assertThat(ModuleRoleDetector.detect("BANKING-INFRASTRUCTURE")).contains(ModuleRole.INFRASTRUCTURE);
        }

        @Test
        @DisplayName("should match last suffix for double-suffix artifactId")
        void shouldMatchLastSuffixForDoubleSuffix() {
            // -core-service: last meaningful suffix is -service → APPLICATION
            assertThat(ModuleRoleDetector.detect("banking-core-service")).contains(ModuleRole.APPLICATION);
        }

        @Test
        @DisplayName("should match exact single-word artifactId")
        void shouldMatchExactSingleWordArtifactId() {
            assertThat(ModuleRoleDetector.detect("persistence")).contains(ModuleRole.INFRASTRUCTURE);
            assertThat(ModuleRoleDetector.detect("api")).contains(ModuleRole.API);
        }
    }
}

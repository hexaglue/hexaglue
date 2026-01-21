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

package io.hexaglue.arch.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tests for {@link MethodRole}.
 *
 * @since 5.0.0
 */
@DisplayName("MethodRole")
class MethodRoleTest {

    @Nested
    @DisplayName("Mutation Roles")
    class MutationRoles {

        @ParameterizedTest
        @EnumSource(
                value = MethodRole.class,
                names = {"SETTER", "COMMAND", "BUSINESS"})
        @DisplayName("should identify mutation roles")
        void shouldIdentifyMutationRoles(MethodRole role) {
            assertThat(role.isMutation()).isTrue();
        }

        @ParameterizedTest
        @EnumSource(
                value = MethodRole.class,
                names = {"GETTER", "FACTORY", "VALIDATION", "LIFECYCLE", "OBJECT_METHOD", "QUERY"})
        @DisplayName("should identify non-mutation roles")
        void shouldIdentifyNonMutationRoles(MethodRole role) {
            assertThat(role.isMutation()).isFalse();
        }
    }

    @Nested
    @DisplayName("Accessor Roles")
    class AccessorRoles {

        @ParameterizedTest
        @EnumSource(
                value = MethodRole.class,
                names = {"GETTER", "QUERY"})
        @DisplayName("should identify accessor roles")
        void shouldIdentifyAccessorRoles(MethodRole role) {
            assertThat(role.isAccessor()).isTrue();
        }

        @ParameterizedTest
        @EnumSource(
                value = MethodRole.class,
                names = {"SETTER", "FACTORY", "BUSINESS", "VALIDATION", "LIFECYCLE", "OBJECT_METHOD", "COMMAND"})
        @DisplayName("should identify non-accessor roles")
        void shouldIdentifyNonAccessorRoles(MethodRole role) {
            assertThat(role.isAccessor()).isFalse();
        }
    }

    @Nested
    @DisplayName("Infrastructure Roles")
    class InfrastructureRoles {

        @ParameterizedTest
        @EnumSource(
                value = MethodRole.class,
                names = {"OBJECT_METHOD", "LIFECYCLE", "FACTORY"})
        @DisplayName("should identify infrastructure roles")
        void shouldIdentifyInfrastructureRoles(MethodRole role) {
            assertThat(role.isInfrastructure()).isTrue();
        }

        @ParameterizedTest
        @EnumSource(
                value = MethodRole.class,
                names = {"GETTER", "SETTER", "BUSINESS", "VALIDATION", "COMMAND", "QUERY"})
        @DisplayName("should identify non-infrastructure roles")
        void shouldIdentifyNonInfrastructureRoles(MethodRole role) {
            assertThat(role.isInfrastructure()).isFalse();
        }
    }

    @Nested
    @DisplayName("Domain Operation Roles")
    class DomainOperationRoles {

        @ParameterizedTest
        @EnumSource(
                value = MethodRole.class,
                names = {"BUSINESS", "COMMAND", "QUERY", "VALIDATION"})
        @DisplayName("should identify domain operation roles")
        void shouldIdentifyDomainOperationRoles(MethodRole role) {
            assertThat(role.isDomainOperation()).isTrue();
        }

        @ParameterizedTest
        @EnumSource(
                value = MethodRole.class,
                names = {"GETTER", "SETTER", "FACTORY", "LIFECYCLE", "OBJECT_METHOD"})
        @DisplayName("should identify non-domain operation roles")
        void shouldIdentifyNonDomainOperationRoles(MethodRole role) {
            assertThat(role.isDomainOperation()).isFalse();
        }
    }

    @Nested
    @DisplayName("Completeness")
    class Completeness {

        @Test
        @DisplayName("should have exactly 9 values")
        void shouldHaveExactNumberOfValues() {
            // GETTER, SETTER, FACTORY, BUSINESS, VALIDATION, LIFECYCLE, OBJECT_METHOD, COMMAND, QUERY
            assertThat(MethodRole.values()).hasSize(9);
        }
    }

    @Nested
    @DisplayName("Role-Specific Tests")
    class RoleSpecificTests {

        @Test
        @DisplayName("GETTER should be accessor but not mutation")
        void getterShouldBeAccessorButNotMutation() {
            assertThat(MethodRole.GETTER.isAccessor()).isTrue();
            assertThat(MethodRole.GETTER.isMutation()).isFalse();
            assertThat(MethodRole.GETTER.isInfrastructure()).isFalse();
            assertThat(MethodRole.GETTER.isDomainOperation()).isFalse();
        }

        @Test
        @DisplayName("SETTER should be mutation but not accessor")
        void setterShouldBeMutationButNotAccessor() {
            assertThat(MethodRole.SETTER.isMutation()).isTrue();
            assertThat(MethodRole.SETTER.isAccessor()).isFalse();
            assertThat(MethodRole.SETTER.isInfrastructure()).isFalse();
            assertThat(MethodRole.SETTER.isDomainOperation()).isFalse();
        }

        @Test
        @DisplayName("FACTORY should be infrastructure")
        void factoryShouldBeInfrastructure() {
            assertThat(MethodRole.FACTORY.isInfrastructure()).isTrue();
            assertThat(MethodRole.FACTORY.isMutation()).isFalse();
            assertThat(MethodRole.FACTORY.isAccessor()).isFalse();
            assertThat(MethodRole.FACTORY.isDomainOperation()).isFalse();
        }

        @Test
        @DisplayName("BUSINESS should be domain operation and mutation")
        void businessShouldBeDomainOperationAndMutation() {
            assertThat(MethodRole.BUSINESS.isDomainOperation()).isTrue();
            assertThat(MethodRole.BUSINESS.isMutation()).isTrue();
            assertThat(MethodRole.BUSINESS.isAccessor()).isFalse();
            assertThat(MethodRole.BUSINESS.isInfrastructure()).isFalse();
        }

        @Test
        @DisplayName("VALIDATION should be domain operation but not mutation")
        void validationShouldBeDomainOperationButNotMutation() {
            assertThat(MethodRole.VALIDATION.isDomainOperation()).isTrue();
            assertThat(MethodRole.VALIDATION.isMutation()).isFalse();
            assertThat(MethodRole.VALIDATION.isAccessor()).isFalse();
            assertThat(MethodRole.VALIDATION.isInfrastructure()).isFalse();
        }

        @Test
        @DisplayName("LIFECYCLE should be infrastructure")
        void lifecycleShouldBeInfrastructure() {
            assertThat(MethodRole.LIFECYCLE.isInfrastructure()).isTrue();
            assertThat(MethodRole.LIFECYCLE.isDomainOperation()).isFalse();
        }

        @Test
        @DisplayName("OBJECT_METHOD should be infrastructure")
        void objectMethodShouldBeInfrastructure() {
            assertThat(MethodRole.OBJECT_METHOD.isInfrastructure()).isTrue();
            assertThat(MethodRole.OBJECT_METHOD.isDomainOperation()).isFalse();
        }

        @Test
        @DisplayName("COMMAND should be domain operation and mutation")
        void commandShouldBeDomainOperationAndMutation() {
            assertThat(MethodRole.COMMAND.isDomainOperation()).isTrue();
            assertThat(MethodRole.COMMAND.isMutation()).isTrue();
            assertThat(MethodRole.COMMAND.isAccessor()).isFalse();
            assertThat(MethodRole.COMMAND.isInfrastructure()).isFalse();
        }

        @Test
        @DisplayName("QUERY should be domain operation and accessor")
        void queryShouldBeDomainOperationAndAccessor() {
            assertThat(MethodRole.QUERY.isDomainOperation()).isTrue();
            assertThat(MethodRole.QUERY.isAccessor()).isTrue();
            assertThat(MethodRole.QUERY.isMutation()).isFalse();
            assertThat(MethodRole.QUERY.isInfrastructure()).isFalse();
        }
    }
}

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

package io.hexaglue.core.classification.semantic;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.core.graph.model.NodeId;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link InterfaceFacts} predicate methods.
 *
 * @since 5.0.0
 */
class InterfaceFactsTest {

    private static final NodeId INTERFACE_ID = NodeId.type("com.example.ports.out.SomePort");

    @Nested
    class DrivingPortCandidate {

        @Test
        void shouldMatchWhenImplementedByCoreAndNotUsedByCore() {
            InterfaceFacts facts = InterfaceFacts.drivingPort(INTERFACE_ID, 1, true);
            assertThat(facts.isDrivingPortCandidate()).isTrue();
        }

        @Test
        void shouldNotMatchWhenUsedByCore() {
            var facts = new InterfaceFacts(INTERFACE_ID, 1, false, false, false, true, true, true);
            assertThat(facts.isDrivingPortCandidate()).isFalse();
        }
    }

    @Nested
    class DrivenPortCandidate {

        @Test
        void shouldMatchWithMissingImplAndAnnotation() {
            InterfaceFacts facts = InterfaceFacts.drivenPortMissingImpl(INTERFACE_ID, true);
            assertThat(facts.isDrivenPortCandidate()).isTrue();
        }

        @Test
        void shouldMatchWithInternalImplAndAnnotation() {
            InterfaceFacts facts = InterfaceFacts.drivenPortInternalImpl(INTERFACE_ID, 1, true);
            assertThat(facts.isDrivenPortCandidate()).isTrue();
        }

        @Test
        void shouldNotMatchWithoutAnnotation() {
            InterfaceFacts facts = InterfaceFacts.drivenPortMissingImpl(INTERFACE_ID, false);
            assertThat(facts.isDrivenPortCandidate()).isFalse();
        }
    }

    @Nested
    class DrivenPortWithExternalImpl {

        @Test
        void shouldMatchWhenUsedByCoreWithExternalInfraImpl() {
            // usedByCore=true, implementedByCore=false, missingImpl=false, internalImplOnly=false
            // This is the PaymentGateway scenario: app service depends on it,
            // adapter exists in infrastructure (not domain anchor)
            var facts = new InterfaceFacts(INTERFACE_ID, 1, false, false, true, true, false, true);
            assertThat(facts.isDrivenPortWithExternalImpl()).isTrue();
        }

        @Test
        void shouldNotMatchWhenNotUsedByCore() {
            // usedByCore=false — no app service depends on it
            var facts = new InterfaceFacts(INTERFACE_ID, 1, false, false, true, false, false, true);
            assertThat(facts.isDrivenPortWithExternalImpl()).isFalse();
        }

        @Test
        void shouldNotMatchWhenMissingImpl() {
            // missingImpl=true — covered by isDrivenPortCandidate instead
            var facts = new InterfaceFacts(INTERFACE_ID, 0, true, false, false, true, false, true);
            assertThat(facts.isDrivenPortWithExternalImpl()).isFalse();
        }

        @Test
        void shouldNotMatchWhenInternalImplOnly() {
            // internalImplOnly=true — covered by isDrivenPortCandidateWithoutAnnotationCheck instead
            var facts = new InterfaceFacts(INTERFACE_ID, 1, false, true, false, true, false, true);
            assertThat(facts.isDrivenPortWithExternalImpl()).isFalse();
        }

        @Test
        void shouldNotMatchWhenImplementedByCore() {
            // implementedByCore=true — this is a driving port scenario
            var facts = new InterfaceFacts(INTERFACE_ID, 1, false, false, false, true, true, true);
            assertThat(facts.isDrivenPortWithExternalImpl()).isFalse();
        }
    }

    @Nested
    class DrivenPortByInfrastructureImpl {

        @Test
        void shouldMatchWhenAllImplsAreInfraAnchors() {
            // infraImplOnly=true, implementedByCore=false
            // This is the NotificationSender scenario: no app service depends on it,
            // but only infrastructure adapters implement it
            InterfaceFacts facts = InterfaceFacts.drivenPortInfraImpl(INTERFACE_ID, 1, true);
            assertThat(facts.isDrivenPortByInfrastructureImpl()).isTrue();
        }

        @Test
        void shouldNotMatchWhenImplementedByCore() {
            // infraImplOnly=true but implementedByCore=true — not a pure driven port
            var facts = new InterfaceFacts(INTERFACE_ID, 1, false, false, true, false, true, true);
            assertThat(facts.isDrivenPortByInfrastructureImpl()).isFalse();
        }

        @Test
        void shouldNotMatchWhenNotInfraImplOnly() {
            // infraImplOnly=false — mixed implementations
            var facts = new InterfaceFacts(INTERFACE_ID, 1, false, false, false, false, false, true);
            assertThat(facts.isDrivenPortByInfrastructureImpl()).isFalse();
        }

        @Test
        void shouldMatchEvenWhenNotUsedByCore() {
            // Event-driven ports may not be directly injected
            var facts = new InterfaceFacts(INTERFACE_ID, 1, false, false, true, false, false, true);
            assertThat(facts.isDrivenPortByInfrastructureImpl()).isTrue();
        }
    }

    @Nested
    class FactoryMethods {

        @Test
        void drivingPortShouldSetCorrectFlags() {
            InterfaceFacts facts = InterfaceFacts.drivingPort(INTERFACE_ID, 2, true);
            assertThat(facts.implementedByCore()).isTrue();
            assertThat(facts.usedByCore()).isFalse();
            assertThat(facts.infraImplOnly()).isFalse();
            assertThat(facts.internalImplOnly()).isFalse();
            assertThat(facts.missingImpl()).isFalse();
            assertThat(facts.implsProdCount()).isEqualTo(2);
        }

        @Test
        void drivenPortMissingImplShouldSetCorrectFlags() {
            InterfaceFacts facts = InterfaceFacts.drivenPortMissingImpl(INTERFACE_ID, true);
            assertThat(facts.usedByCore()).isTrue();
            assertThat(facts.missingImpl()).isTrue();
            assertThat(facts.infraImplOnly()).isFalse();
            assertThat(facts.implementedByCore()).isFalse();
        }

        @Test
        void drivenPortInternalImplShouldSetCorrectFlags() {
            InterfaceFacts facts = InterfaceFacts.drivenPortInternalImpl(INTERFACE_ID, 1, true);
            assertThat(facts.usedByCore()).isTrue();
            assertThat(facts.internalImplOnly()).isTrue();
            assertThat(facts.infraImplOnly()).isFalse();
            assertThat(facts.implementedByCore()).isFalse();
        }

        @Test
        void drivenPortInfraImplShouldSetCorrectFlags() {
            InterfaceFacts facts = InterfaceFacts.drivenPortInfraImpl(INTERFACE_ID, 1, true);
            assertThat(facts.infraImplOnly()).isTrue();
            assertThat(facts.internalImplOnly()).isFalse();
            assertThat(facts.implementedByCore()).isFalse();
            assertThat(facts.implsProdCount()).isEqualTo(1);
        }
    }

    @Nested
    class Undecided {

        @Test
        void shouldBeUndecidedWhenNoConditionMatches() {
            // No strong signal in any direction
            var facts = new InterfaceFacts(INTERFACE_ID, 0, true, false, false, false, false, false);
            assertThat(facts.isUndecided()).isTrue();
        }

        @Test
        void shouldNotBeUndecidedWhenDrivenPortWithExternalImpl() {
            var facts = new InterfaceFacts(INTERFACE_ID, 1, false, false, false, true, false, true);
            assertThat(facts.isUndecided()).isFalse();
        }

        @Test
        void shouldNotBeUndecidedWhenDrivenPortByInfraImpl() {
            var facts = new InterfaceFacts(INTERFACE_ID, 1, false, false, true, false, false, true);
            assertThat(facts.isUndecided()).isFalse();
        }
    }
}

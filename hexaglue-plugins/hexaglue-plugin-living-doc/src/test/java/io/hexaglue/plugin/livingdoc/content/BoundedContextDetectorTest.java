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

package io.hexaglue.plugin.livingdoc.content;

import static io.hexaglue.plugin.livingdoc.V5TestModelBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.ProjectContext;
import io.hexaglue.arch.model.AggregateRoot;
import io.hexaglue.arch.model.DrivenPortType;
import io.hexaglue.arch.model.Entity;
import io.hexaglue.arch.model.TypeId;
import io.hexaglue.arch.model.ValueObject;
import io.hexaglue.plugin.livingdoc.model.BoundedContextDoc;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for BoundedContextDetector.
 *
 * @since 5.0.0
 */
@DisplayName("BoundedContextDetector")
class BoundedContextDetectorTest {

    @Nested
    @DisplayName("Multi-context detection")
    class MultiContextDetection {

        @Test
        @DisplayName("should detect multiple bounded contexts from different packages")
        void detectsMultipleContexts() {
            AggregateRoot order = aggregateRoot("com.example.order.domain.Order");
            Entity lineItem = entity("com.example.order.domain.LineItem");
            AggregateRoot product = aggregateRoot("com.example.inventory.domain.Product");
            ValueObject sku = valueObject("com.example.inventory.domain.Sku", List.of("code"));

            ArchitecturalModel model =
                    createModel(ProjectContext.forTesting("app", "com.example"), order, lineItem, product, sku);

            BoundedContextDetector detector = new BoundedContextDetector(model);
            List<BoundedContextDoc> contexts = detector.detectAll();

            assertThat(contexts).hasSize(2);

            BoundedContextDoc inventoryCtx = contexts.stream()
                    .filter(c -> c.name().equals("inventory"))
                    .findFirst()
                    .orElseThrow();
            assertThat(inventoryCtx.aggregateCount()).isEqualTo(1);
            assertThat(inventoryCtx.valueObjectCount()).isEqualTo(1);
            assertThat(inventoryCtx.totalTypeCount()).isEqualTo(2);

            BoundedContextDoc orderCtx = contexts.stream()
                    .filter(c -> c.name().equals("order"))
                    .findFirst()
                    .orElseThrow();
            assertThat(orderCtx.aggregateCount()).isEqualTo(1);
            assertThat(orderCtx.entityCount()).isEqualTo(1);
            assertThat(orderCtx.totalTypeCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("should sort contexts by name")
        void sortsByName() {
            AggregateRoot z = aggregateRoot("com.example.zebra.domain.Z");
            AggregateRoot a = aggregateRoot("com.example.alpha.domain.A");

            ArchitecturalModel model = createModel(ProjectContext.forTesting("app", "com.example"), z, a);

            BoundedContextDetector detector = new BoundedContextDetector(model);
            List<BoundedContextDoc> contexts = detector.detectAll();

            assertThat(contexts).extracting(BoundedContextDoc::name).containsExactly("alpha", "zebra");
        }
    }

    @Nested
    @DisplayName("Single context detection")
    class SingleContextDetection {

        @Test
        @DisplayName("should detect single context when all types share the same package root")
        void detectsSingleContext() {
            AggregateRoot order = aggregateRoot("com.example.order.domain.Order");
            Entity lineItem = entity("com.example.order.domain.LineItem");

            ArchitecturalModel model = createModel(ProjectContext.forTesting("app", "com.example"), order, lineItem);

            BoundedContextDetector detector = new BoundedContextDetector(model);
            List<BoundedContextDoc> contexts = detector.detectAll();

            assertThat(contexts).hasSize(1);
            assertThat(contexts.get(0).name()).isEqualTo("order");
            assertThat(contexts.get(0).totalTypeCount()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Short package handling")
    class ShortPackageHandling {

        @Test
        @DisplayName("should handle packages with fewer than 3 segments")
        void handlesShortPackages() {
            AggregateRoot order = aggregateRoot("com.order.Order");

            ArchitecturalModel model = createModel(ProjectContext.forTesting("app", "com"), order);

            BoundedContextDetector detector = new BoundedContextDetector(model);
            List<BoundedContextDoc> contexts = detector.detectAll();

            assertThat(contexts).hasSize(1);
            // For 2-segment package, uses last segment
            assertThat(contexts.get(0).name()).isEqualTo("order");
        }
    }

    @Nested
    @DisplayName("Empty model")
    class EmptyModel {

        @Test
        @DisplayName("should return empty list for model with no types")
        void returnsEmptyForNoTypes() {
            ArchitecturalModel model = createModel(ProjectContext.forTesting("app", "com.example"));

            BoundedContextDetector detector = new BoundedContextDetector(model);
            List<BoundedContextDoc> contexts = detector.detectAll();

            assertThat(contexts).isEmpty();
        }
    }

    @Nested
    @DisplayName("contextOf lookup")
    class ContextOfLookup {

        @Test
        @DisplayName("should return context name for known type")
        void returnsContextForKnownType() {
            AggregateRoot order = aggregateRoot("com.example.order.domain.Order");

            ArchitecturalModel model = createModel(ProjectContext.forTesting("app", "com.example"), order);

            BoundedContextDetector detector = new BoundedContextDetector(model);

            assertThat(detector.contextOf(TypeId.of("com.example.order.domain.Order")))
                    .isPresent()
                    .hasValue("order");
        }

        @Test
        @DisplayName("should return empty for unknown type")
        void returnsEmptyForUnknownType() {
            AggregateRoot order = aggregateRoot("com.example.order.domain.Order");

            ArchitecturalModel model = createModel(ProjectContext.forTesting("app", "com.example"), order);

            BoundedContextDetector detector = new BoundedContextDetector(model);

            assertThat(detector.contextOf(TypeId.of("com.example.unknown.UnknownType")))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("Port counting")
    class PortCounting {

        @Test
        @DisplayName("should count ports in bounded context")
        void countsPortsInContext() {
            AggregateRoot order = aggregateRoot("com.example.order.domain.Order");
            var drivingPort = drivingPort("com.example.order.port.OrderUseCase");
            var drivenPort = drivenPort("com.example.order.port.OrderRepository", DrivenPortType.REPOSITORY);

            ArchitecturalModel model =
                    createModel(ProjectContext.forTesting("app", "com.example"), order, drivingPort, drivenPort);

            BoundedContextDetector detector = new BoundedContextDetector(model);
            List<BoundedContextDoc> contexts = detector.detectAll();

            assertThat(contexts).hasSize(1);
            assertThat(contexts.get(0).portCount()).isEqualTo(2);
            assertThat(contexts.get(0).totalTypeCount()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("extractContextName static method")
    class ExtractContextName {

        @Test
        @DisplayName("should extract third segment as context name")
        void extractsThirdSegment() {
            assertThat(BoundedContextDetector.extractContextName(TypeId.of("com.example.order.domain.Order")))
                    .isEqualTo("order");
        }

        @Test
        @DisplayName("should use last segment for short packages")
        void usesLastSegmentForShort() {
            assertThat(BoundedContextDetector.extractContextName(TypeId.of("com.order.Order")))
                    .isEqualTo("order");
        }

        @Test
        @DisplayName("should use single segment for single-segment packages")
        void usesSingleSegment() {
            assertThat(BoundedContextDetector.extractContextName(TypeId.of("order.Order")))
                    .isEqualTo("order");
        }
    }
}

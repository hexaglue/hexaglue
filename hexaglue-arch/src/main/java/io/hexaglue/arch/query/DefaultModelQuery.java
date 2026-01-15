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

package io.hexaglue.arch.query;

import io.hexaglue.arch.ArchElement;
import io.hexaglue.arch.ElementRef;
import io.hexaglue.arch.ElementRegistry;
import io.hexaglue.arch.RelationshipStore;
import io.hexaglue.arch.adapters.DrivenAdapter;
import io.hexaglue.arch.adapters.DrivingAdapter;
import io.hexaglue.arch.domain.Aggregate;
import io.hexaglue.arch.domain.DomainService;
import io.hexaglue.arch.ports.ApplicationService;
import io.hexaglue.arch.ports.DrivenPort;
import io.hexaglue.arch.ports.DrivingPort;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Default implementation of {@link ModelQuery}.
 *
 * <p>Provides fluent access to all architectural elements in the model.
 * All queries are immutable and reusable.</p>
 *
 * @since 4.0.0
 */
public final class DefaultModelQuery implements ModelQuery {

    private final ElementRegistry registry;
    private final RelationshipStore relationships;

    /**
     * Creates a new model query.
     *
     * @param registry the element registry
     * @param relationships the relationship store
     */
    public DefaultModelQuery(ElementRegistry registry, RelationshipStore relationships) {
        this.registry = Objects.requireNonNull(registry, "registry must not be null");
        this.relationships = Objects.requireNonNull(relationships, "relationships must not be null");
    }

    @Override
    public AggregateQuery aggregates() {
        return new DefaultAggregateQuery(() -> registry.all(Aggregate.class), relationships);
    }

    @Override
    public PortQuery drivingPorts() {
        return new DefaultPortQuery(() -> registry.all(DrivingPort.class).map(p -> p));
    }

    @Override
    public PortQuery drivenPorts() {
        return new DefaultPortQuery(() -> registry.all(DrivenPort.class).map(p -> p));
    }

    @Override
    public PortQuery allPorts() {
        return new DefaultPortQuery(() -> Stream.concat(
                registry.all(DrivingPort.class).map(p -> (ArchElement) p),
                registry.all(DrivenPort.class).map(p -> (ArchElement) p)));
    }

    @Override
    public ServiceQuery<ApplicationService> applicationServices() {
        return new DefaultServiceQuery<>(() -> registry.all(ApplicationService.class));
    }

    @Override
    public ServiceQuery<DomainService> domainServices() {
        return new DefaultServiceQuery<>(() -> registry.all(DomainService.class));
    }

    @Override
    public AdapterQuery<DrivingAdapter> drivingAdapters() {
        return new DefaultAdapterQuery<>(() -> registry.all(DrivingAdapter.class));
    }

    @Override
    public AdapterQuery<DrivenAdapter> drivenAdapters() {
        return new DefaultAdapterQuery<>(() -> registry.all(DrivenAdapter.class));
    }

    @Override
    public ElementQuery<ArchElement> elements() {
        return new DefaultElementQuery<>(registry::all);
    }

    @Override
    public <T extends ArchElement> ElementQuery<T> elements(Class<T> type) {
        Objects.requireNonNull(type, "type must not be null");
        return new DefaultElementQuery<>(() -> registry.all(type));
    }

    @Override
    public <T extends ArchElement> Optional<T> find(ElementRef<T> ref) {
        Objects.requireNonNull(ref, "ref must not be null");
        return ref.resolveOpt(registry);
    }

    @Override
    public <T extends ArchElement> T get(ElementRef<T> ref) {
        Objects.requireNonNull(ref, "ref must not be null");
        return ref.resolveOrThrow(registry);
    }
}

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
import io.hexaglue.arch.ConfidenceLevel;
import io.hexaglue.arch.ports.DrivenPort;
import io.hexaglue.arch.ports.DrivingPort;
import io.hexaglue.arch.ports.PortClassification;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Default implementation of {@link PortQuery}.
 *
 * <p>This implementation is immutable. Each filter method returns a new
 * instance with the added filter.</p>
 *
 * @since 4.0.0
 */
public final class DefaultPortQuery extends DefaultElementQuery<ArchElement> implements PortQuery {

    /**
     * Creates a new port query.
     *
     * @param source the port source supplier
     */
    public DefaultPortQuery(Supplier<Stream<ArchElement>> source) {
        super(source);
    }

    private DefaultPortQuery(Supplier<Stream<ArchElement>> source, Predicate<ArchElement> filter) {
        super(source, filter);
    }

    @Override
    protected DefaultPortQuery withFilter(Predicate<ArchElement> additionalFilter) {
        return new DefaultPortQuery(source(), filter().and(additionalFilter));
    }

    @Override
    public PortQuery repositories() {
        return withFilter(this::isRepository);
    }

    @Override
    public PortQuery gateways() {
        return withFilter(this::isGateway);
    }

    @Override
    public PortQuery useCases() {
        return withFilter(this::isUseCase);
    }

    @Override
    public PortQuery filter(Predicate<ArchElement> predicate) {
        Objects.requireNonNull(predicate, "predicate must not be null");
        return withFilter(predicate);
    }

    @Override
    public PortQuery inPackage(String packageName) {
        Objects.requireNonNull(packageName, "packageName must not be null");
        return withFilter(e -> e.packageName().equals(packageName));
    }

    @Override
    public PortQuery withConfidence(ConfidenceLevel minLevel) {
        Objects.requireNonNull(minLevel, "minLevel must not be null");
        return withFilter(e -> {
            ConfidenceLevel elementLevel = e.classificationTrace().confidence();
            return elementLevel.compareTo(minLevel) >= 0;
        });
    }

    private boolean isRepository(ArchElement element) {
        if (element instanceof DrivenPort port) {
            return port.classification() == PortClassification.REPOSITORY;
        }
        return false;
    }

    private boolean isGateway(ArchElement element) {
        if (element instanceof DrivenPort port) {
            return port.classification() == PortClassification.GATEWAY;
        }
        return false;
    }

    private boolean isUseCase(ArchElement element) {
        if (element instanceof DrivingPort port) {
            return port.classification() == PortClassification.USE_CASE;
        }
        return false;
    }
}

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
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Default implementation of {@link ServiceQuery}.
 *
 * <p>This implementation is immutable. Each filter method returns a new
 * instance with the added filter.</p>
 *
 * @param <T> the service element type
 * @since 4.0.0
 */
public final class DefaultServiceQuery<T extends ArchElement> extends DefaultElementQuery<T>
        implements ServiceQuery<T> {

    /**
     * Creates a new service query.
     *
     * @param source the service source supplier
     */
    public DefaultServiceQuery(Supplier<Stream<T>> source) {
        super(source);
    }

    private DefaultServiceQuery(Supplier<Stream<T>> source, Predicate<T> filter) {
        super(source, filter);
    }

    @Override
    protected DefaultServiceQuery<T> withFilter(Predicate<T> additionalFilter) {
        return new DefaultServiceQuery<>(source(), filter().and(additionalFilter));
    }

    @Override
    public ServiceQuery<T> filter(Predicate<T> predicate) {
        Objects.requireNonNull(predicate, "predicate must not be null");
        return withFilter(predicate);
    }

    @Override
    public ServiceQuery<T> inPackage(String packageName) {
        Objects.requireNonNull(packageName, "packageName must not be null");
        return withFilter(e -> e.packageName().equals(packageName));
    }

    @Override
    public ServiceQuery<T> withConfidence(ConfidenceLevel minLevel) {
        Objects.requireNonNull(minLevel, "minLevel must not be null");
        return withFilter(e -> {
            ConfidenceLevel elementLevel = e.classificationTrace().confidence();
            return elementLevel.compareTo(minLevel) >= 0;
        });
    }
}

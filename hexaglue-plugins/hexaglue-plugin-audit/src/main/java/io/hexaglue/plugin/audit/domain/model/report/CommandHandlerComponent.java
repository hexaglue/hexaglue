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

package io.hexaglue.plugin.audit.domain.model.report;

import java.util.Objects;
import java.util.Optional;

/**
 * Details about a command handler component.
 *
 * <p>A command handler processes commands that modify state. In CQRS architecture,
 * command handlers are responsible for handling write operations.
 *
 * @param name simple name of the command handler
 * @param packageName fully qualified package name
 * @param handledCommand name of the command type handled (if detected)
 * @param targetAggregate name of the target aggregate (if detected)
 * @since 5.0.0
 */
public record CommandHandlerComponent(String name, String packageName, String handledCommand, String targetAggregate) {

    /**
     * Creates a command handler component with validation.
     */
    public CommandHandlerComponent {
        Objects.requireNonNull(name, "name is required");
        Objects.requireNonNull(packageName, "packageName is required");
    }

    /**
     * Returns the handled command as optional.
     *
     * @return optional handled command name
     */
    public Optional<String> handledCommandOpt() {
        return Optional.ofNullable(handledCommand);
    }

    /**
     * Returns the target aggregate as optional.
     *
     * @return optional target aggregate name
     */
    public Optional<String> targetAggregateOpt() {
        return Optional.ofNullable(targetAggregate);
    }

    /**
     * Creates a command handler component with all fields.
     *
     * @param name simple name
     * @param packageName package name
     * @param handledCommand command type handled (may be null)
     * @param targetAggregate target aggregate name (may be null)
     * @return the command handler component
     */
    public static CommandHandlerComponent of(
            String name, String packageName, String handledCommand, String targetAggregate) {
        return new CommandHandlerComponent(name, packageName, handledCommand, targetAggregate);
    }

    /**
     * Creates a command handler component without command/aggregate info.
     *
     * @param name simple name
     * @param packageName package name
     * @return the command handler component
     */
    public static CommandHandlerComponent of(String name, String packageName) {
        return new CommandHandlerComponent(name, packageName, null, null);
    }
}

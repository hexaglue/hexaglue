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

import io.hexaglue.arch.ClassificationTrace;
import java.util.Objects;

/**
 * Represents a command handler in the application layer.
 *
 * <p>A command handler processes commands that modify state. In CQRS (Command Query
 * Responsibility Segregation) architecture, command handlers are responsible for
 * handling write operations.</p>
 *
 * <h2>Characteristics</h2>
 * <ul>
 *   <li>Write operations - modifies system state</li>
 *   <li>Single responsibility - handles one command type</li>
 *   <li>Side effects - persists changes, publishes events</li>
 * </ul>
 *
 * <h2>Typical Pattern</h2>
 * <pre>{@code
 * public class PlaceOrderHandler implements CommandHandler<PlaceOrderCommand> {
 *     public void handle(PlaceOrderCommand command) {
 *         // Load aggregate
 *         // Execute domain operation
 *         // Persist changes
 *         // Publish events
 *     }
 * }
 * }</pre>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * CommandHandler handler = CommandHandler.of(
 *     TypeId.of("com.example.PlaceOrderHandler"),
 *     structure,
 *     trace
 * );
 * }</pre>
 *
 * @param id the unique identifier for this type
 * @param structure the structural description of this type
 * @param classification the classification trace explaining why this type was classified
 * @since 4.1.0
 */
public record CommandHandler(TypeId id, TypeStructure structure, ClassificationTrace classification)
        implements ApplicationType {

    /**
     * Creates a new CommandHandler.
     *
     * @param id the type id, must not be null
     * @param structure the type structure, must not be null
     * @param classification the classification trace, must not be null
     * @throws NullPointerException if any argument is null
     */
    public CommandHandler {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(structure, "structure must not be null");
        Objects.requireNonNull(classification, "classification must not be null");
    }

    @Override
    public ArchKind kind() {
        return ArchKind.COMMAND_HANDLER;
    }

    /**
     * Creates a CommandHandler with the given parameters.
     *
     * @param id the type id
     * @param structure the type structure
     * @param classification the classification trace
     * @return a new CommandHandler
     * @throws NullPointerException if any argument is null
     */
    public static CommandHandler of(TypeId id, TypeStructure structure, ClassificationTrace classification) {
        return new CommandHandler(id, structure, classification);
    }
}

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

package io.hexaglue.core.classification.discriminator;

import io.hexaglue.spi.classification.CertaintyLevel;
import io.hexaglue.spi.classification.ClassificationEvidence;
import io.hexaglue.spi.classification.ClassificationStrategy;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import spoon.reflect.declaration.CtRecord;
import spoon.reflect.declaration.CtRecordComponent;

/**
 * Discriminator for detecting value objects implemented as Java records.
 *
 * <p>Java records are immutable by design and commonly used to implement value objects
 * in modern DDD. This discriminator detects records that should be classified as
 * VALUE_OBJECT based on structural characteristics.
 *
 * <p>Classification rules:
 * <ol>
 *   <li>Type must be a Java record</li>
 *   <li>Must NOT have an "id" component (would indicate identity)</li>
 *   <li>Must NOT be an ID wrapper (detected by {@link IdWrapperDiscriminator})</li>
 * </ol>
 *
 * <p>Records meeting these criteria are classified with:
 * <ul>
 *   <li>Certainty: {@link CertaintyLevel#CERTAIN_BY_STRUCTURE}</li>
 *   <li>Strategy: {@link ClassificationStrategy#RECORD}</li>
 * </ul>
 *
 * @since 3.0.0
 */
public final class RecordValueObjectDiscriminator {

    private final IdWrapperDiscriminator idWrapperDiscriminator;

    /**
     * Creates a new discriminator with the given ID wrapper detector.
     *
     * @param idWrapperDiscriminator the ID wrapper discriminator
     */
    public RecordValueObjectDiscriminator(IdWrapperDiscriminator idWrapperDiscriminator) {
        this.idWrapperDiscriminator = Objects.requireNonNull(idWrapperDiscriminator, "idWrapperDiscriminator required");
    }

    /**
     * Attempts to classify the given record as a value object.
     *
     * @param record the record to classify
     * @return optional containing classification if the record is a value object
     */
    public Optional<ValueObjectClassification> detect(CtRecord record) {
        Objects.requireNonNull(record, "record required");

        List<ClassificationEvidence> evidences = new ArrayList<>();

        // Evidence: is a record
        evidences.add(ClassificationEvidence.positive(
                "IS_RECORD", 50, "Type is a Java record, which are immutable by design"));

        // Check if it's an ID wrapper (disqualifies as value object)
        if (idWrapperDiscriminator.isIdWrapper(record)) {
            return Optional.empty(); // ID wrappers are classified as IDENTIFIER
        }

        // Check for identity component (disqualifies as value object)
        if (hasIdComponent(record)) {
            evidences.add(ClassificationEvidence.negative(
                    "HAS_ID_COMPONENT", -100, "Record has an 'id' component, indicating identity"));
            return Optional.empty(); // Has identity, not a value object
        }

        // Evidence: no identity
        evidences.add(ClassificationEvidence.positive("NO_IDENTITY", 30, "Record has no 'id' component"));

        String typeName = record.getQualifiedName();
        String reasoning = String.format(
                "Java record '%s' without identity is classified as VALUE_OBJECT", record.getSimpleName());

        return Optional.of(new ValueObjectClassification(
                typeName, CertaintyLevel.CERTAIN_BY_STRUCTURE, ClassificationStrategy.RECORD, reasoning, evidences));
    }

    /**
     * Checks if the record has an identity component.
     *
     * <p>A record has identity if any component is named "id" or "identifier".
     *
     * @param record the record to check
     * @return true if the record has an id component
     */
    private boolean hasIdComponent(CtRecord record) {
        for (CtRecordComponent component : record.getRecordComponents()) {
            String componentName = component.getSimpleName().toLowerCase();
            if (componentName.equals("id") || componentName.equals("identifier")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Classification result for a value object.
     *
     * @param typeName   the fully qualified type name
     * @param certainty  the certainty level of the classification
     * @param strategy   the classification strategy used
     * @param reasoning  human-readable explanation
     * @param evidences  list of supporting evidence
     */
    public record ValueObjectClassification(
            String typeName,
            CertaintyLevel certainty,
            ClassificationStrategy strategy,
            String reasoning,
            List<ClassificationEvidence> evidences) {

        public ValueObjectClassification {
            Objects.requireNonNull(typeName, "typeName required");
            Objects.requireNonNull(certainty, "certainty required");
            Objects.requireNonNull(strategy, "strategy required");
            Objects.requireNonNull(reasoning, "reasoning required");
            evidences = evidences != null ? List.copyOf(evidences) : List.of();
        }
    }
}

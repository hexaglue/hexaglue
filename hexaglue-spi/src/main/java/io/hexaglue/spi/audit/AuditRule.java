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

package io.hexaglue.spi.audit;

import io.hexaglue.arch.model.audit.CodeUnit;
import io.hexaglue.arch.model.audit.RuleViolation;
import io.hexaglue.arch.model.audit.Severity;
import java.util.List;

/**
 * An audit rule that checks code units for violations.
 *
 * <p>Audit rules encapsulate specific checks for architecture compliance,
 * code quality, documentation, or other concerns. Each rule has a unique
 * identifier, a human-readable name, and a default severity level.
 *
 * <p>Example implementation:
 * <pre>{@code
 * public class LayerDependencyRule implements AuditRule {
 *     @Override
 *     public String id() {
 *         return "hexaglue.arch.layer-dependency";
 *     }
 *
 *     @Override
 *     public String name() {
 *         return "Layer Dependency Check";
 *     }
 *
 *     @Override
 *     public Severity defaultSeverity() {
 *         return Severity.ERROR;
 *     }
 *
 *     @Override
 *     public List<RuleViolation> check(CodeUnit unit) {
 *         // Check if unit violates layer dependencies
 *         // Return violations if found
 *         return List.of();
 *     }
 * }
 * }</pre>
 *
 * @since 3.0.0
 */
public interface AuditRule {

    /**
     * Returns the unique identifier for this rule.
     *
     * <p>Convention: use reverse domain notation with rule category,
     * e.g., "hexaglue.arch.layer-dependency" or "hexaglue.doc.missing-javadoc"
     *
     * @return the rule identifier
     */
    String id();

    /**
     * Returns the human-readable name of this rule.
     *
     * @return the rule name
     */
    String name();

    /**
     * Returns the default severity for violations of this rule.
     *
     * @return the default severity
     */
    Severity defaultSeverity();

    /**
     * Checks a code unit and returns violations.
     *
     * @param unit the code unit to check
     * @return list of violations (empty if none)
     */
    List<RuleViolation> check(CodeUnit unit);
}

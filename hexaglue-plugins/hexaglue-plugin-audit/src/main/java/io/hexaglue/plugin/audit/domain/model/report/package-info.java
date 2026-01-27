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

/**
 * Report data model for the JSON pivot format (v2.0).
 *
 * <p>This package contains all the records that make up the audit report
 * data structure. The main entry point is {@link io.hexaglue.plugin.audit.domain.model.report.ReportData}.
 *
 * <h2>Report Structure</h2>
 * The report is organized into 5 sections:
 * <ol>
 *   <li><strong>Verdict</strong> - Score, grade, status, KPIs, immediate action</li>
 *   <li><strong>Architecture</strong> - Inventory, components, diagrams info, relationships</li>
 *   <li><strong>Issues</strong> - Violations grouped by theme with impact and suggestions</li>
 *   <li><strong>Remediation</strong> - Prioritized actions to fix issues</li>
 *   <li><strong>Appendix</strong> - Score breakdown, metrics, constraints, package metrics</li>
 * </ol>
 *
 * <h2>Design Principles</h2>
 * <ul>
 *   <li>All records are immutable with defensive copies of collections</li>
 *   <li>Null safety enforced via Objects.requireNonNull in compact constructors</li>
 *   <li>Diagrams (Mermaid) are stored separately in {@link io.hexaglue.plugin.audit.domain.model.report.DiagramSet}</li>
 *   <li>ReportData is the JSON pivot format; DiagramSet is shared between HTML/Markdown</li>
 * </ul>
 *
 * <h2>Key Classes</h2>
 * <ul>
 *   <li>{@link io.hexaglue.plugin.audit.domain.model.report.ReportData} - Root data structure</li>
 *   <li>{@link io.hexaglue.plugin.audit.domain.model.report.DiagramSet} - Mermaid diagrams container</li>
 *   <li>{@link io.hexaglue.plugin.audit.domain.model.report.Verdict} - Audit verdict with score</li>
 *   <li>{@link io.hexaglue.plugin.audit.domain.model.report.IssueEntry} - Individual violation with impact and suggestion</li>
 *   <li>{@link io.hexaglue.plugin.audit.domain.model.report.Suggestion} - Remediation suggestion with steps and code example</li>
 * </ul>
 *
 * @since 5.0.0
 * @see io.hexaglue.plugin.audit.domain.model.report.ReportData
 * @see io.hexaglue.plugin.audit.domain.model.report.DiagramSet
 */
package io.hexaglue.plugin.audit.domain.model.report;

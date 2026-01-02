package io.hexaglue.core.classification.port.criteria;

import io.hexaglue.core.classification.ConfidenceLevel;
import io.hexaglue.core.classification.Evidence;
import io.hexaglue.core.classification.MatchResult;
import io.hexaglue.core.classification.port.PortClassificationCriteria;
import io.hexaglue.core.classification.port.PortDirection;
import io.hexaglue.core.classification.port.PortKind;
import io.hexaglue.core.frontend.JavaForm;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.core.graph.query.GraphQuery;
import io.hexaglue.core.graph.style.PackageOrganizationStyle;
import java.util.List;

/**
 * Matches interfaces in packages indicating inbound/driving ports.
 *
 * <p>Package patterns:
 * <ul>
 *   <li>Package ends with ".in"</li>
 *   <li>Package ends with ".ports.in"</li>
 *   <li>Package ends with ".port.in"</li>
 *   <li>Package contains ".inbound."</li>
 *   <li>Package contains ".primary."</li>
 * </ul>
 *
 * <p>Confidence and priority are style-aware:
 * <ul>
 *   <li>HEXAGONAL style: priority 80, HIGH confidence (patterns are canonical)</li>
 *   <li>Other styles: priority 60, MEDIUM confidence (patterns are heuristic)</li>
 * </ul>
 *
 * <p>Direction: DRIVING
 */
public final class PackageInCriteria implements PortClassificationCriteria {

    @Override
    public String name() {
        return "package-in";
    }

    @Override
    public int priority() {
        return 60;
    }

    @Override
    public PortKind targetKind() {
        return PortKind.USE_CASE;
    }

    @Override
    public PortDirection targetDirection() {
        return PortDirection.DRIVING;
    }

    @Override
    public MatchResult evaluate(TypeNode node, GraphQuery query) {
        // Must be an interface
        if (node.form() != JavaForm.INTERFACE) {
            return MatchResult.noMatch();
        }

        String packageName = node.packageName();
        String reason = getPackageReason(packageName);

        if (reason == null) {
            return MatchResult.noMatch();
        }

        // Style-aware confidence: HEXAGONAL style gets HIGH confidence because
        // .ports.in/.ports.out patterns are canonical for hexagonal architecture
        PackageOrganizationStyle style = query.packageOrganizationStyle();
        ConfidenceLevel confidence =
                (style == PackageOrganizationStyle.HEXAGONAL) ? ConfidenceLevel.HIGH : ConfidenceLevel.MEDIUM;

        String styleInfo = (style == PackageOrganizationStyle.HEXAGONAL) ? ", hexagonal style detected" : "";

        return MatchResult.match(
                confidence,
                "Interface in package '%s' (%s%s)".formatted(packageName, reason, styleInfo),
                List.of(Evidence.fromPackage(packageName, reason)));
    }

    private String getPackageReason(String packageName) {
        if (packageName.endsWith(".in")) {
            return "ends with '.in'";
        }
        if (packageName.endsWith(".ports.in") || packageName.endsWith(".port.in")) {
            return "ends with '.ports.in' or '.port.in'";
        }
        if (packageName.contains(".inbound") || packageName.endsWith(".inbound")) {
            return "contains '.inbound'";
        }
        if (packageName.contains(".primary") || packageName.endsWith(".primary")) {
            return "contains '.primary'";
        }
        return null;
    }
}

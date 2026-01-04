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

package io.hexaglue.core;

import io.hexaglue.spi.ir.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Simple JSON serializer for IrSnapshot.
 * Produces a deterministic, human-readable JSON format for golden file testing.
 *
 * <p>Note: Excludes volatile fields (timestamp, sourceRef line numbers) for stable comparisons.
 */
public final class IrJsonSerializer {

    private IrJsonSerializer() {}

    public static String toJson(IrSnapshot snapshot) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"domain\": ").append(domainToJson(snapshot.domain())).append(",\n");
        sb.append("  \"ports\": ").append(portsToJson(snapshot.ports())).append("\n");
        sb.append("}");
        return sb.toString();
    }

    private static String domainToJson(DomainModel domain) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("    \"types\": [\n");
        List<DomainType> sortedTypes = domain.types().stream()
                .sorted((a, b) -> a.qualifiedName().compareTo(b.qualifiedName()))
                .toList();
        for (int i = 0; i < sortedTypes.size(); i++) {
            sb.append(domainTypeToJson(sortedTypes.get(i)));
            if (i < sortedTypes.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("    ]\n");
        sb.append("  }");
        return sb.toString();
    }

    private static String domainTypeToJson(DomainType type) {
        StringBuilder sb = new StringBuilder();
        sb.append("      {\n");
        sb.append("        \"qualifiedName\": \"").append(type.qualifiedName()).append("\",\n");
        sb.append("        \"simpleName\": \"").append(type.simpleName()).append("\",\n");
        sb.append("        \"kind\": \"").append(type.kind()).append("\",\n");
        sb.append("        \"confidence\": \"").append(type.confidence()).append("\",\n");
        sb.append("        \"construct\": \"").append(type.construct()).append("\"");
        if (type.identity().isPresent()) {
            sb.append(",\n        \"identity\": ")
                    .append(identityToJson(type.identity().get()));
        }
        if (!type.properties().isEmpty()) {
            sb.append(",\n        \"properties\": ").append(propertiesToJson(type.properties()));
        }
        sb.append("\n      }");
        return sb.toString();
    }

    private static String identityToJson(Identity identity) {
        return String.format(
                "{\"fieldName\": \"%s\", \"typeName\": \"%s\"}",
                identity.fieldName(), identity.type().qualifiedName());
    }

    private static String propertiesToJson(List<DomainProperty> properties) {
        return "["
                + properties.stream()
                        .map(p -> String.format(
                                "{\"name\": \"%s\", \"typeName\": \"%s\", \"cardinality\": \"%s\"}",
                                p.name(), p.type().unwrapElement().qualifiedName(), p.cardinality()))
                        .collect(Collectors.joining(", "))
                + "]";
    }

    private static String portsToJson(PortModel ports) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("    \"ports\": [\n");
        List<Port> sortedPorts = ports.ports().stream()
                .sorted((a, b) -> a.qualifiedName().compareTo(b.qualifiedName()))
                .toList();
        for (int i = 0; i < sortedPorts.size(); i++) {
            sb.append(portToJson(sortedPorts.get(i)));
            if (i < sortedPorts.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("    ]\n");
        sb.append("  }");
        return sb.toString();
    }

    private static String portToJson(Port port) {
        StringBuilder sb = new StringBuilder();
        sb.append("      {\n");
        sb.append("        \"qualifiedName\": \"").append(port.qualifiedName()).append("\",\n");
        sb.append("        \"simpleName\": \"").append(port.simpleName()).append("\",\n");
        sb.append("        \"kind\": \"").append(port.kind()).append("\",\n");
        sb.append("        \"direction\": \"").append(port.direction()).append("\",\n");
        sb.append("        \"confidence\": \"").append(port.confidence()).append("\"");
        if (!port.methods().isEmpty()) {
            sb.append(",\n        \"methods\": [");
            sb.append(port.methods().stream().map(m -> "\"" + m.name() + "\"").collect(Collectors.joining(", ")));
            sb.append("]");
        }
        sb.append("\n      }");
        return sb.toString();
    }
}

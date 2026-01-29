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

package io.hexaglue.plugin.livingdoc.model;

import java.util.List;

/**
 * Documentation model for port methods.
 *
 * @param name the method name
 * @param returnType the return type qualified name
 * @param parameters list of parameter type qualified names
 * @param documentation the method Javadoc description, or null if absent
 * @since 5.0.0 - added documentation field
 */
public record MethodDoc(String name, String returnType, List<String> parameters, String documentation) {}

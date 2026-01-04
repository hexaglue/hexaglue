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

package io.hexaglue.core.frontend;

import java.util.List;
import java.util.Optional;

/**
 * A parameter of a method or constructor.
 */
public interface JavaParameter extends JavaAnnotated, JavaSourced {

    /**
     * Returns the parameter name.
     */
    String name();

    /**
     * Returns the parameter type.
     */
    TypeRef type();

    /**
     * Simple implementation for cases where only name and type are known.
     */
    record Simple(String name, TypeRef type) implements JavaParameter {
        @Override
        public List<JavaAnnotation> annotations() {
            return List.of();
        }

        @Override
        public Optional<SourceRef> sourceRef() {
            return Optional.empty();
        }
    }
}

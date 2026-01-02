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

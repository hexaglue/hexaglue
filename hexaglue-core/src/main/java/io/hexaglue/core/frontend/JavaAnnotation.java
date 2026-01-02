package io.hexaglue.core.frontend;

import java.util.Map;
import java.util.Optional;

/**
 * An annotation present on a Java element.
 *
 * @param annotationType the annotation type reference
 * @param values the annotation attribute values (name -> value)
 */
public record JavaAnnotation(TypeRef annotationType, Map<String, Object> values) {

    /**
     * Returns the fully qualified name of the annotation type.
     */
    public String qualifiedName() {
        return annotationType.rawQualifiedName();
    }

    /**
     * Returns the simple name of the annotation type.
     */
    public String simpleName() {
        return annotationType.simpleName();
    }

    /**
     * Returns a string attribute value.
     */
    public Optional<String> getString(String name) {
        Object v = values.get(name);
        return v instanceof String s ? Optional.of(s) : Optional.empty();
    }

    /**
     * Returns a boolean attribute value.
     */
    public Optional<Boolean> getBoolean(String name) {
        Object v = values.get(name);
        return v instanceof Boolean b ? Optional.of(b) : Optional.empty();
    }

    /**
     * Returns the "value" attribute as a string.
     */
    public Optional<String> value() {
        return getString("value");
    }
}

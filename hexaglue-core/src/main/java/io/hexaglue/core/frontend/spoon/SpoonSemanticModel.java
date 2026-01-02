package io.hexaglue.core.frontend.spoon;

import io.hexaglue.core.frontend.JavaSemanticModel;
import io.hexaglue.core.frontend.JavaType;
import io.hexaglue.core.frontend.spoon.adapters.SpoonTypeAdapter;
import java.util.Comparator;
import java.util.stream.Stream;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtType;

/**
 * Spoon implementation of {@link JavaSemanticModel}.
 *
 * <p>Wraps a Spoon {@link CtModel} and provides access to types
 * filtered by the configured base package.
 */
final class SpoonSemanticModel implements JavaSemanticModel {

    private final CtModel model;
    private final String basePackage;

    SpoonSemanticModel(CtModel model, String basePackage) {
        this.model = model;
        this.basePackage = basePackage;
    }

    @Override
    public Stream<JavaType> types() {
        return model.getAllTypes().stream()
                .filter(this::isInScope)
                .sorted(Comparator.comparing(CtType::getQualifiedName))
                .map(SpoonTypeAdapter::adapt);
    }

    private boolean isInScope(CtType<?> type) {
        if (basePackage == null || basePackage.isBlank()) {
            return true;
        }
        String pkg = type.getPackage() == null ? "" : type.getPackage().getQualifiedName();
        return pkg.equals(basePackage) || pkg.startsWith(basePackage + ".");
    }
}

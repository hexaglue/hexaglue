package io.hexaglue.core.frontend.spoon.adapters;

import io.hexaglue.core.frontend.SourceRef;
import java.util.Optional;
import spoon.reflect.cu.SourcePosition;

/**
 * Adapts Spoon's {@link SourcePosition} to {@link SourceRef}.
 */
public final class SpoonSourceRefAdapter {

    private SpoonSourceRefAdapter() {}

    public static Optional<SourceRef> adapt(SourcePosition pos) {
        if (pos == null || !pos.isValidPosition() || pos.getFile() == null) {
            return Optional.empty();
        }
        return Optional.of(
                new SourceRef(pos.getFile().getPath(), pos.getLine(), Math.max(pos.getLine(), pos.getEndLine())));
    }
}

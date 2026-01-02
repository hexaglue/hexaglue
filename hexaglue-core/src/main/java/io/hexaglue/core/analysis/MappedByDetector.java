package io.hexaglue.core.analysis;

import io.hexaglue.core.frontend.TypeRef;
import io.hexaglue.core.graph.model.FieldNode;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.core.graph.query.GraphQuery;
import java.util.List;
import java.util.Optional;

/**
 * Detects the mappedBy field for bidirectional relationships.
 *
 * <p>In JPA, bidirectional relationships have an owning side and an inverse side.
 * The inverse side declares {@code mappedBy} to indicate which field on the
 * owning side defines the relationship.
 *
 * <p>Example:
 * <pre>{@code
 * // Order has items collection
 * public class Order {
 *     private List<LineItem> items;  // owning side or inverse side?
 * }
 *
 * // LineItem references back to Order
 * public class LineItem {
 *     private Order order;  // This is the mappedBy target
 * }
 *
 * // Detector finds: Order.items mappedBy "order"
 * }</pre>
 */
public final class MappedByDetector {

    /**
     * Detects the mappedBy field name for a bidirectional relationship.
     *
     * <p>Given a collection field on the owner type, this method looks for
     * a back-reference field on the element type that points to the owner.
     *
     * @param ownerType the type containing the collection field
     * @param collectionField the collection field (e.g., items: List&lt;LineItem&gt;)
     * @param elementType the element type of the collection
     * @param query the graph query interface
     * @return the name of the back-reference field, if found
     */
    public Optional<String> detectMappedBy(
            TypeNode ownerType, FieldNode collectionField, TypeRef elementType, GraphQuery query) {

        // Find the target type in the graph
        Optional<TypeNode> targetType = query.type(elementType.rawQualifiedName());
        if (targetType.isEmpty()) {
            return Optional.empty();
        }

        // Look for a field in the target type that references the owner type
        List<FieldNode> targetFields = query.fieldsOf(targetType.get());

        for (FieldNode targetField : targetFields) {
            String fieldTypeName = targetField.type().rawQualifiedName();

            // Check if this field references the owner type
            if (fieldTypeName.equals(ownerType.qualifiedName())) {
                return Optional.of(targetField.simpleName());
            }
        }

        return Optional.empty();
    }

    /**
     * Detects if a simple field (non-collection) has a back-reference.
     *
     * <p>This is useful for ONE_TO_ONE bidirectional relationships.
     *
     * @param ownerType the type containing the field
     * @param field the field to check
     * @param query the graph query interface
     * @return the name of the back-reference field, if found
     */
    public Optional<String> detectMappedByForSimpleField(TypeNode ownerType, FieldNode field, GraphQuery query) {

        TypeRef fieldType = field.type();
        Optional<TypeNode> targetType = query.type(fieldType.rawQualifiedName());

        if (targetType.isEmpty()) {
            return Optional.empty();
        }

        // Look for a field in the target type that references the owner type
        List<FieldNode> targetFields = query.fieldsOf(targetType.get());

        for (FieldNode targetField : targetFields) {
            String targetFieldTypeName = targetField.type().rawQualifiedName();

            // Check if this field references the owner type
            if (targetFieldTypeName.equals(ownerType.qualifiedName())) {
                return Optional.of(targetField.simpleName());
            }
        }

        return Optional.empty();
    }
}

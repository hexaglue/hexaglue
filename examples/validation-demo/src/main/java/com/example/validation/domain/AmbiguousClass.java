package com.example.validation.domain;

/**
 * A class demonstrating what happens to types NOT classified as domain types.
 *
 * <p>Classification: NOT ANALYZED (not part of domain model)
 *
 * <p>This class is NOT included in the validation report because:
 * <ul>
 *   <li>No jMolecules annotation (@AggregateRoot, @Entity, @ValueObject, etc.)</li>
 *   <li>No explicit classification in hexaglue.yaml</li>
 *   <li>No semantic signals (not embedded in aggregates, no repository relationship)</li>
 * </ul>
 *
 * <p><b>IMPORTANT:</b> This is the expected behavior - HexaGlue only classifies
 * types that are part of the domain model. Types without domain signals are simply
 * not analyzed. UNCLASSIFIED is for types that ARE part of the domain model but
 * cannot be classified deterministically.
 *
 * <p><b>TO INCLUDE IN ANALYSIS:</b>
 *
 * <p>Option 1: Add jMolecules annotation
 * <pre>
 * {@literal @}ValueObject
 * public class AmbiguousClass { ... }
 * </pre>
 *
 * <p>Option 2: Add to hexaglue.yaml explicit section
 * <pre>
 * classification:
 *   explicit:
 *     com.example.validation.domain.AmbiguousClass: VALUE_OBJECT
 * </pre>
 *
 * <p>Option 3: Embed in an aggregate root
 * <pre>
 * {@literal @}AggregateRoot
 * public class Order {
 *     private AmbiguousClass metadata; // Now analyzed
 * }
 * </pre>
 */
public class AmbiguousClass {

    private String data;
    private int count;

    public AmbiguousClass() {
        // Default constructor
    }

    public AmbiguousClass(String data, int count) {
        this.data = data;
        this.count = count;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String process() {
        return data.repeat(count);
    }
}

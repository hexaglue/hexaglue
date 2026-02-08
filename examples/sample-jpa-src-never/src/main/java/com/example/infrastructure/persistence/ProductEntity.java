package com.example.infrastructure.persistence;

import jakarta.persistence.*;

// Pre-existing file - should NOT be overwritten with never policy
@Entity
@Table(name = "product")
public class ProductEntity {
    @Id
    private java.util.UUID id;

    private String name;

    // MARKER: This line proves the file was NOT overwritten by HexaGlue
    private String neverOverwriteMarker;
}

package com.example.infrastructure.persistence;

import jakarta.persistence.*;

// Manually edited entity - should be protected by IF_UNCHANGED policy
@Entity
@Table(name = "product")
public class ProductEntity {
    @Id
    private java.util.UUID id;

    private String name;

    // CUSTOM: manually added field that HexaGlue wouldn't generate
    private String customField;
}

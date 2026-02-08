package com.example.infrastructure.persistence;

/**
 * Stale file from a previous generation - should be deleted by StaleFileCleaner.
 * This file simulates a generated JPA entity whose source domain type was removed.
 * It has no annotations to avoid being re-classified by HexaGlue.
 */
public class OldProductEntity {
    private java.util.UUID id;
    private String legacyField;
}

/*
 * Verify script for test-param-jpa-infra-package integration test.
 * Validates custom infrastructurePackage parameter.
 */

def buildLog = new File(basedir, "build.log")
assert buildLog.exists() : "build.log should exist"

def logContent = buildLog.text

// =============================================================================
// BUILD VERIFICATION
// =============================================================================

assert logContent.contains("BUILD SUCCESS") : "Build should succeed"
assert !logContent.contains("BUILD FAILURE") : "Build should not fail"

assert logContent.contains("HexaGlue analyzing:") :
    "Should contain HexaGlue analyzing output"

// =============================================================================
// JPA PLUGIN VERIFICATION
// =============================================================================

assert logContent.contains("io.hexaglue.plugin.jpa") :
    "Build log should contain JPA plugin execution"

assert logContent.contains("JPA generation complete:") :
    "Build log should contain JPA generation completion message"

assert logContent.contains("1 entities") :
    "Should generate 1 entity (Book)"

// =============================================================================
// CUSTOM PACKAGE VERIFICATION
// =============================================================================

def customPackage = new File(basedir, "target/generated-sources/hexaglue/com/example/custom/persistence")
assert customPackage.exists() : "Custom persistence package should exist at com/example/custom/persistence"

["BookEntity.java", "BookJpaRepository.java", "BookMapper.java", "BookRepositoryAdapter.java"].each { file ->
    def f = new File(customPackage, file)
    assert f.exists() : "${file} should be generated in custom package"
}

// Verify files are NOT in default location
def defaultPackage = new File(basedir, "target/generated-sources/hexaglue/com/example/infrastructure/persistence")
assert !defaultPackage.exists() :
    "Files should NOT be in default package (com/example/infrastructure/persistence)"

// Verify BookEntity contains correct package declaration
def bookEntity = new File(customPackage, "BookEntity.java").text
assert bookEntity.contains("package com.example.custom.persistence;") :
    "BookEntity should have correct package declaration"
assert bookEntity.contains("@Entity") : "BookEntity should contain @Entity annotation"

println """
=============================================================================
SUCCESS: test-param-jpa-infra-package integration test passed!

Validated:
  - JPA files generated in custom package com.example.custom.persistence
  - BookEntity, BookJpaRepository, BookMapper, BookRepositoryAdapter present
  - Files NOT in default location
  - Correct package declaration in generated files
=============================================================================
"""

return true

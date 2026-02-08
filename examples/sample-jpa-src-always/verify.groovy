/*
 * Verify script for sample-jpa-src-always integration test.
 * Validates JPA generation into src/main/java/ with overwrite=always policy.
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
    "Should generate 1 entity (Product)"

// =============================================================================
// GENERATED FILES IN src/main/java/
// =============================================================================

def infraPackage = new File(basedir, "src/main/java/com/example/infrastructure/persistence")
assert infraPackage.exists() : "Infrastructure persistence package should exist in src/main/java"

["ProductEntity.java", "ProductJpaRepository.java", "ProductMapper.java", "ProductRepositoryAdapter.java"].each { file ->
    def f = new File(infraPackage, file)
    assert f.exists() : "${file} should be generated in src/main/java"
}

// Verify ProductEntity contains @Entity annotation
def productEntity = new File(infraPackage, "ProductEntity.java").text
assert productEntity.contains("@Entity") : "ProductEntity should contain @Entity annotation"

// =============================================================================
// VERIFY NOT IN target/hexaglue/generated-sources/
// =============================================================================

def targetGenerated = new File(basedir, "target/hexaglue/generated-sources/com/example/infrastructure/persistence")
assert !targetGenerated.exists() :
    "Generated files should NOT be in target/hexaglue/generated-sources/ when outputDirectory=src/main/java"

// =============================================================================
// MANIFEST VERIFICATION
// =============================================================================

def manifest = new File(basedir, "target/hexaglue/manifest.txt")
assert manifest.exists() : "Manifest file should exist at target/hexaglue/manifest.txt"

def manifestContent = manifest.text
assert manifestContent.contains("src/main/java/") :
    "Manifest should contain src/main/java/ paths"
assert manifestContent.contains("|sha256:") :
    "Manifest should contain SHA-256 checksums"

println """
=============================================================================
SUCCESS: sample-jpa-src-always integration test passed!

Validated:
  - JPA generation into src/main/java/ with overwrite=always
  - ProductEntity, ProductJpaRepository, ProductMapper, ProductRepositoryAdapter generated
  - Files NOT duplicated in target/hexaglue/generated-sources/
  - Manifest created with SHA-256 checksums
=============================================================================
"""

return true

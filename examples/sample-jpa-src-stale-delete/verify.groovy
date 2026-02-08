/*
 * Verify script for sample-jpa-src-stale-delete integration test.
 * Validates that stale files in src/ are deleted with staleFilePolicy=DELETE.
 */

def buildLog = new File(basedir, "build.log")
assert buildLog.exists() : "build.log should exist"

def logContent = buildLog.text

// =============================================================================
// BUILD VERIFICATION
// =============================================================================

assert logContent.contains("BUILD SUCCESS") : "Build should succeed"
assert !logContent.contains("BUILD FAILURE") : "Build should not fail"

// =============================================================================
// STALE FILE DELETION
// =============================================================================

// Verify stale file was deleted (exact message from StaleFileCleaner:106)
assert logContent.contains("Deleted stale generated file") :
    "Build log should contain 'Deleted stale generated file' message"

// Verify OldProductEntity no longer exists on disk
def infraPackage = new File(basedir, "src/main/java/com/example/infrastructure/persistence")
def staleFile = new File(infraPackage, "OldProductEntity.java")
assert !staleFile.exists() : "OldProductEntity.java should have been deleted (stale file)"

// =============================================================================
// CURRENT FILES VERIFICATION
// =============================================================================

// Verify freshly generated files exist
["ProductEntity.java", "ProductJpaRepository.java", "ProductMapper.java", "ProductRepositoryAdapter.java"].each { file ->
    def f = new File(infraPackage, file)
    assert f.exists() : "${file} should exist (freshly generated)"
}

// =============================================================================
// MANIFEST VERIFICATION
// =============================================================================

def manifest = new File(basedir, "target/hexaglue/manifest.txt")
assert manifest.exists() : "Manifest should exist"

def manifestContent = manifest.text
assert !manifestContent.contains("OldProductEntity") :
    "New manifest should NOT contain OldProductEntity (stale file removed)"
assert manifestContent.contains("ProductEntity.java") :
    "New manifest should contain ProductEntity.java"

println """
=============================================================================
SUCCESS: sample-jpa-src-stale-delete integration test passed!

Validated:
  - OldProductEntity.java was deleted (stale file cleanup)
  - Current JPA files generated correctly
  - New manifest does not reference the stale file
  - staleFilePolicy=DELETE correctly applied
=============================================================================
"""

return true

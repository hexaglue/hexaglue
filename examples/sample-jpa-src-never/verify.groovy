/*
 * Verify script for sample-jpa-src-never integration test.
 * Validates that pre-existing files are NOT overwritten with overwrite=never policy,
 * while new files are still created.
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
// JPA PLUGIN VERIFICATION
// =============================================================================

assert logContent.contains("io.hexaglue.plugin.jpa") :
    "Build log should contain JPA plugin execution"

// Verify NEVER policy log message (exact message from FileSystemCodeWriter:255)
assert logContent.contains("overwrite policy: NEVER") :
    "Build log should contain overwrite policy: NEVER skip message"

// =============================================================================
// PRE-EXISTING FILE PROTECTION
// =============================================================================

def infraPackage = new File(basedir, "src/main/java/com/example/infrastructure/persistence")
assert infraPackage.exists() : "Infrastructure persistence package should exist"

// Verify the pre-existing ProductEntity was NOT overwritten
def productEntity = new File(infraPackage, "ProductEntity.java").text
assert productEntity.contains("neverOverwriteMarker") :
    "ProductEntity should still contain the neverOverwriteMarker (was NOT overwritten)"

// =============================================================================
// NEW FILE CREATION
// =============================================================================

// New files should still be created even with NEVER policy (they don't exist yet)
def repoFile = new File(infraPackage, "ProductJpaRepository.java")
assert repoFile.exists() : "ProductJpaRepository should be created (new file, not pre-existing)"

println """
=============================================================================
SUCCESS: sample-jpa-src-never integration test passed!

Validated:
  - Pre-existing ProductEntity was NOT overwritten (neverOverwriteMarker preserved)
  - New ProductJpaRepository was created (did not exist before)
  - overwrite=never policy correctly applied
=============================================================================
"""

return true

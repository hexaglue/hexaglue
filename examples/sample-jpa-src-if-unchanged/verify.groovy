/*
 * Verify script for sample-jpa-src-if-unchanged integration test.
 * Validates that manually edited files (checksum mismatch) are protected
 * by the IF_UNCHANGED overwrite policy, while new files are still created.
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
// IF_UNCHANGED POLICY VERIFICATION
// =============================================================================

// Verify IF_UNCHANGED skip message (exact message from FileSystemCodeWriter:265)
assert logContent.contains("manually modified since last generation") :
    "Build log should contain 'manually modified since last generation' skip message"

// =============================================================================
// MANUALLY EDITED FILE PROTECTION
// =============================================================================

def infraPackage = new File(basedir, "src/main/java/com/example/infrastructure/persistence")
assert infraPackage.exists() : "Infrastructure persistence package should exist"

// Verify the manually edited ProductEntity was NOT overwritten
def productEntity = new File(infraPackage, "ProductEntity.java").text
assert productEntity.contains("customField") :
    "ProductEntity should still contain customField (manually edited file was protected)"

// =============================================================================
// NEW FILE CREATION
// =============================================================================

// New files should still be created (no previous checksum = no protection needed)
def repoFile = new File(infraPackage, "ProductJpaRepository.java")
assert repoFile.exists() : "ProductJpaRepository should be created (new file, no previous checksum)"

println """
=============================================================================
SUCCESS: sample-jpa-src-if-unchanged integration test passed!

Validated:
  - Manually edited ProductEntity was protected (customField preserved)
  - Checksum mismatch correctly detected (dummy manifest vs actual file)
  - New ProductJpaRepository was created (no previous checksum)
  - overwrite=if-unchanged policy correctly applied
=============================================================================
"""

return true

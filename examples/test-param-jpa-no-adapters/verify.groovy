/*
 * Verify script for test-param-jpa-no-adapters integration test.
 * Validates generateAdapters=false parameter.
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
// ENTITY GENERATION VERIFICATION
// =============================================================================

def infraPackage = new File(basedir, "target/generated-sources/hexaglue/com/example/infrastructure/persistence")
assert infraPackage.exists() : "Infrastructure persistence package should exist"

def bookEntity = new File(infraPackage, "BookEntity.java")
assert bookEntity.exists() : "BookEntity.java should be generated"

def entityContent = bookEntity.text
assert entityContent.contains("@Entity") : "BookEntity should contain @Entity annotation"

// =============================================================================
// NO ADAPTER VERIFICATION
// =============================================================================

// List all files in the infrastructure package
def files = infraPackage.listFiles({ file ->
    file.name.endsWith("Adapter.java") || file.name.contains("Adapter")
} as FileFilter)

assert files == null || files.length == 0 :
    "No adapter files should be generated when generateAdapters=false (found: ${files?.collect { it.name }})"

// Specifically check for BookRepositoryAdapter
def bookRepositoryAdapter = new File(infraPackage, "BookRepositoryAdapter.java")
assert !bookRepositoryAdapter.exists() : "BookRepositoryAdapter.java should NOT be generated when generateAdapters=false"

println """
=============================================================================
SUCCESS: test-param-jpa-no-adapters integration test passed!

Validated:
  - JPA generation with generateAdapters=false
  - BookEntity generated successfully
  - NO adapter files generated
  - NO BookRepositoryAdapter.java
=============================================================================
"""

return true

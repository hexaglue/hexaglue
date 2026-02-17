/*
 * Verify script for test-param-jpa-no-embeddables integration test.
 * Validates generateEmbeddables=false parameter.
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

def infraPackage = new File(basedir, "target/hexaglue/generated-sources/com/example/infrastructure/persistence")
assert infraPackage.exists() : "Infrastructure persistence package should exist"

def bookEntity = new File(infraPackage, "BookEntity.java")
assert bookEntity.exists() : "BookEntity.java should be generated"

def entityContent = bookEntity.text
assert entityContent.contains("@Entity") : "BookEntity should contain @Entity annotation"

// =============================================================================
// NO EMBEDDABLE VERIFICATION
// =============================================================================

// List all files in the infrastructure package
def files = infraPackage.listFiles({ file ->
    file.name.endsWith("Embeddable.java") ||
    file.name.contains("Embeddable") ||
    (file.isFile() && file.text.contains("@Embeddable"))
} as FileFilter)

assert files == null || files.length == 0 :
    "No embeddable files should be generated when generateEmbeddables=false (found: ${files?.collect { it.name }})"

// Specifically check for common embeddable patterns (Title, BookId as embeddables)
def titleEmbeddable = new File(infraPackage, "TitleEmbeddable.java")
assert !titleEmbeddable.exists() : "TitleEmbeddable.java should NOT be generated when generateEmbeddables=false"

def bookIdEmbeddable = new File(infraPackage, "BookIdEmbeddable.java")
assert !bookIdEmbeddable.exists() : "BookIdEmbeddable.java should NOT be generated when generateEmbeddables=false"

println """
=============================================================================
SUCCESS: test-param-jpa-no-embeddables integration test passed!

Validated:
  - JPA generation with generateEmbeddables=false
  - BookEntity generated successfully
  - NO embeddable files generated
  - NO @Embeddable classes present
=============================================================================
"""

return true

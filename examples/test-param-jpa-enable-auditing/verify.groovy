/*
 * Verify script for test-param-jpa-enable-auditing integration test.
 * Validates enableAuditing parameter.
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
// AUDITING ANNOTATIONS VERIFICATION
// =============================================================================

def infraPackage = new File(basedir, "target/hexaglue/generated-sources/com/example/infrastructure/persistence")
assert infraPackage.exists() : "Infrastructure persistence package should exist"

def bookEntity = new File(infraPackage, "BookEntity.java")
assert bookEntity.exists() : "BookEntity.java should be generated"

def entityContent = bookEntity.text
assert entityContent.contains("@Entity") : "BookEntity should contain @Entity annotation"

// Verify auditing annotations are present
def hasCreatedDate = entityContent.contains("@CreatedDate") || entityContent.contains("createdDate")
def hasLastModifiedDate = entityContent.contains("@LastModifiedDate") || entityContent.contains("lastModifiedDate")

assert hasCreatedDate : "BookEntity should contain auditing field for creation date when enableAuditing=true"
assert hasLastModifiedDate : "BookEntity should contain auditing field for modification date when enableAuditing=true"

println """
=============================================================================
SUCCESS: test-param-jpa-enable-auditing integration test passed!

Validated:
  - JPA generation with enableAuditing=true
  - BookEntity contains auditing annotations/fields
  - @CreatedDate or createdDate field present
  - @LastModifiedDate or lastModifiedDate field present
=============================================================================
"""

return true

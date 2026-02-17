/*
 * Verify script for test-param-jpa-enable-optimistic-locking integration test.
 * Validates enableOptimisticLocking parameter.
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
// OPTIMISTIC LOCKING VERIFICATION
// =============================================================================

def infraPackage = new File(basedir, "target/hexaglue/generated-sources/com/example/infrastructure/persistence")
assert infraPackage.exists() : "Infrastructure persistence package should exist"

def bookEntity = new File(infraPackage, "BookEntity.java")
assert bookEntity.exists() : "BookEntity.java should be generated"

def entityContent = bookEntity.text
assert entityContent.contains("@Entity") : "BookEntity should contain @Entity annotation"

// Verify @Version annotation is present
assert entityContent.contains("@Version") : "BookEntity should contain @Version annotation when enableOptimisticLocking=true"

// Verify version field is present
def hasVersionField = entityContent.contains("private Long version") ||
                      entityContent.contains("private Integer version") ||
                      entityContent.contains("version")

assert hasVersionField : "BookEntity should contain a version field when enableOptimisticLocking=true"

println """
=============================================================================
SUCCESS: test-param-jpa-enable-optimistic-locking integration test passed!

Validated:
  - JPA generation with enableOptimisticLocking=true
  - BookEntity contains @Version annotation
  - Version field present in entity
=============================================================================
"""

return true

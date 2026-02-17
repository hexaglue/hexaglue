/*
 * Verify script for test-param-jpa-no-mappers integration test.
 * Validates generateMappers=false parameter.
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
// NO MAPPER VERIFICATION
// =============================================================================

// List all files in the infrastructure package
def files = infraPackage.listFiles({ file ->
    file.name.endsWith("Mapper.java") || file.name.contains("Mapper")
} as FileFilter)

assert files == null || files.length == 0 :
    "No mapper files should be generated when generateMappers=false (found: ${files?.collect { it.name }})"

// Specifically check for BookMapper
def bookMapper = new File(infraPackage, "BookMapper.java")
assert !bookMapper.exists() : "BookMapper.java should NOT be generated when generateMappers=false"

println """
=============================================================================
SUCCESS: test-param-jpa-no-mappers integration test passed!

Validated:
  - JPA generation with generateMappers=false
  - BookEntity generated successfully
  - NO mapper files generated
  - NO BookMapper.java
=============================================================================
"""

return true

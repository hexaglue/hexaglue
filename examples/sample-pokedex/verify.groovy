/*
 * Verify script for sample-pokedex integration test.
 * Validates that the JPA plugin executed correctly.
 */

def buildLog = new File(basedir, "build.log")
assert buildLog.exists() : "build.log should exist"

def logContent = buildLog.text

// Verify build success
assert !logContent.contains("BUILD FAILURE") : "Build should not fail"

// Verify HexaGlue plugin executed
assert logContent.contains("HexaGlue analyzing:") :
    "Should contain HexaGlue analyzing output"

// Verify classification completed
assert logContent.contains("Classification complete:") :
    "Should contain 'Classification complete:'"

// Check generated sources directory
def generatedSources = new File(basedir, "target/generated-sources/hexaglue")
assert generatedSources.exists() :
    "Generated sources directory should exist"

// Verify JPA plugin ran
assert logContent.contains("io.hexaglue.plugin.jpa") :
    "Build log should contain JPA plugin execution"

// Verify JPA generation completed
assert logContent.contains("JPA generation complete:") :
    "Build log should contain JPA generation completion message"

println 'SUCCESS: sample-pokedex integration test passed - JPA plugin executed correctly'
return true

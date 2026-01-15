/*
 * Verify script for sample-basic (minimal) integration test.
 * Validates that the living-doc plugin executed correctly.
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

// Check living-doc directory
def livingDocDir = new File(basedir, "target/hexaglue/reports/living-doc")
assert livingDocDir.exists() :
    "Living documentation directory should exist"

// Verify living-doc plugin ran
assert logContent.contains("io.hexaglue.plugin.livingdoc") :
    "Build log should contain living-doc plugin execution"

println 'SUCCESS: sample-basic integration test passed - living-doc plugin executed correctly'
return true

/*
 * Verify script for tutorial-validation integration test.
 * Validates that HexaGlue classification and validation features work correctly.
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
def generatedSources = new File(basedir, "target/hexaglue/generated-sources")
assert generatedSources.exists() :
    "Generated sources directory should exist"

// Check living-doc directory
def livingDocDir = new File(basedir, "target/hexaglue/reports/living-doc")
assert livingDocDir.exists() :
    "Living documentation directory should exist"

// Verify Living Documentation plugin ran
assert logContent.contains("io.hexaglue.plugin.livingdoc") :
    "Build log should contain Living Documentation plugin execution"

// Verify Living Documentation generation completed (supports both legacy and v4 message formats)
assert logContent.contains("Living documentation complete") :
    "Build log should contain Living Documentation completion message"

println 'SUCCESS: tutorial-validation integration test passed - classification and validation features work correctly'
return true

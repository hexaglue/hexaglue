/*
 * Verify script for validation-demo integration test.
 * Validates that the validation and generation goals executed correctly.
 */

def buildLog = new File(basedir, "build.log")
assert buildLog.exists() : "build.log should exist"

def logContent = buildLog.text

// Verify build success
assert !logContent.contains("BUILD FAILURE") : "Build should not fail"

// Verify HexaGlue validation executed
assert logContent.contains("HexaGlue validating:") || logContent.contains("HexaGlue generate-and-audit:") :
    "Should contain HexaGlue validation or generate-and-audit output"

// Verify classification completed
assert logContent.contains("Classification complete:") :
    "Should contain 'Classification complete:'"

// Check generated sources directory
def generatedSources = new File(basedir, "target/hexaglue/generated-sources")
assert generatedSources.exists() :
    "Generated sources directory should exist"

// Check validation report
def validationReport = new File(basedir, "target/hexaglue/reports/validation/validation-report.md")
assert validationReport.exists() :
    "Validation report should exist at target/hexaglue/reports/validation/validation-report.md"

println "SUCCESS: validation-demo integration test passed"
return true

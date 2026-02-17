/*
 * Verify script for test-param-validation-report-path integration test.
 * Validates that validationReportPath redirects the validation report.
 */
def buildLog = new File(basedir, "build.log")
assert buildLog.exists() : "build.log should exist"
def logContent = buildLog.text

assert logContent.contains("BUILD SUCCESS") : "Build should succeed"
assert !logContent.contains("BUILD FAILURE") : "Build should not fail"

// CUSTOM REPORT PATH VERIFICATION
def customReport = new File(basedir, "target/custom/report.md")
assert customReport.exists() : "Validation report should exist at target/custom/report.md"

println """
=============================================================================
SUCCESS: test-param-validation-report-path integration test passed!
Validated:
  - validationReportPath correctly redirected to target/custom/report.md
  - Report file exists at custom location
=============================================================================
"""
return true

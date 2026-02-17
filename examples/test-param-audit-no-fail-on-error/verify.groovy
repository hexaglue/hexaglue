/*
 * Verify script for test-param-audit-no-fail-on-error integration test.
 * Validates that failOnError=false allows build to succeed despite audit violations.
 */
def buildLog = new File(basedir, "build.log")
assert buildLog.exists() : "build.log should exist"
def logContent = buildLog.text

// BUILD SUCCESS VERIFICATION
assert logContent.contains("BUILD SUCCESS") : "Build should succeed with failOnError=false"

// VIOLATIONS STILL LOGGED
assert logContent.contains("violation") || logContent.contains("VIOLATION") :
    "Build log should still mention violations (as warnings)"

// VERIFY AUDIT RAN
assert logContent.contains("hexaglue-plugin-audit") || logContent.contains("Audit") :
    "Build log should show audit plugin execution"

println """
=============================================================================
SUCCESS: test-param-audit-no-fail-on-error integration test passed!
Validated:
  - failOnError=false allows BUILD SUCCESS despite violations
  - Audit plugin ran and logged violations as warnings
  - Build completed successfully
=============================================================================
"""
return true

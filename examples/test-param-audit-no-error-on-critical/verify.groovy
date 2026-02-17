/*
 * Verify script for test-param-audit-no-error-on-critical integration test.
 * Validates that errorOnCritical=false allows build to succeed despite CRITICAL violations.
 */
def buildLog = new File(basedir, "build.log")
assert buildLog.exists() : "build.log should exist"
def logContent = buildLog.text

// BUILD SUCCESS VERIFICATION
assert logContent.contains("BUILD SUCCESS") : "Build should succeed with errorOnCritical=false"

// CRITICAL INFO STILL LOGGED (as info/warning)
// Note: CRITICAL violations may still appear in logs but shouldn't fail build
assert logContent.contains("CRITICAL") || logContent.contains("critical") || logContent.contains("violation") :
    "Build log should still mention violations (as info/warnings)"

// VERIFY AUDIT RAN
assert logContent.contains("hexaglue-plugin-audit") || logContent.contains("Audit") :
    "Build log should show audit plugin execution"

println """
=============================================================================
SUCCESS: test-param-audit-no-error-on-critical integration test passed!
Validated:
  - errorOnCritical=false allows BUILD SUCCESS despite CRITICAL violations
  - Audit plugin ran and logged violations as info/warnings
  - Build completed successfully
=============================================================================
"""
return true

/*
 * Verify script for test-param-audit-no-error-on-blocker integration test.
 * Validates that errorOnBlocker=false allows build to succeed despite BLOCKER violations.
 */
def buildLog = new File(basedir, "build.log")
assert buildLog.exists() : "build.log should exist"
def logContent = buildLog.text

// BUILD SUCCESS VERIFICATION
assert logContent.contains("BUILD SUCCESS") : "Build should succeed with errorOnBlocker=false"

// BLOCKER INFO STILL LOGGED (as info/warning)
// Note: BLOCKER violations may still appear in logs but shouldn't fail build
assert logContent.contains("BLOCKER") || logContent.contains("blocker") || logContent.contains("violation") :
    "Build log should still mention violations (as info/warnings)"

// VERIFY AUDIT RAN
assert logContent.contains("hexaglue-plugin-audit") || logContent.contains("Audit") :
    "Build log should show audit plugin execution"

println """
=============================================================================
SUCCESS: test-param-audit-no-error-on-blocker integration test passed!
Validated:
  - errorOnBlocker=false allows BUILD SUCCESS despite BLOCKER violations
  - Audit plugin ran and logged violations as info/warnings
  - Build completed successfully
=============================================================================
"""
return true

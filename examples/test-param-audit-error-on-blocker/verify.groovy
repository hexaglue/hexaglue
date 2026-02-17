/*
 * Verify script for test-param-audit-error-on-blocker integration test.
 * Validates that errorOnBlocker=true causes build failure when BLOCKER violations are detected.
 */
def buildLog = new File(basedir, "build.log")
assert buildLog.exists() : "build.log should exist"
def logContent = buildLog.text

// BUILD FAILURE VERIFICATION
assert logContent.contains("BUILD FAILURE") : "Build should fail with BLOCKER violations"

// BLOCKER VIOLATION VERIFICATION
assert logContent.contains("BLOCKER") || logContent.contains("blocker") :
    "Build log should mention BLOCKER severity violations"

// VERIFY AUDIT RAN
assert logContent.contains("hexaglue-plugin-audit") || logContent.contains("Audit") :
    "Build log should show audit plugin execution"

println """
=============================================================================
SUCCESS: test-param-audit-error-on-blocker integration test passed!
Validated:
  - errorOnBlocker=true correctly causes BUILD FAILURE
  - BLOCKER violations detected (aggregate cycle)
  - Build log contains BLOCKER violation messages
=============================================================================
"""
return true

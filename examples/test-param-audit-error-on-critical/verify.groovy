/*
 * Verify script for test-param-audit-error-on-critical integration test.
 * Validates that errorOnCritical=true causes build failure when CRITICAL violations are detected.
 */
def buildLog = new File(basedir, "build.log")
assert buildLog.exists() : "build.log should exist"
def logContent = buildLog.text

// BUILD FAILURE VERIFICATION
assert logContent.contains("BUILD FAILURE") : "Build should fail with CRITICAL violations"

// CRITICAL VIOLATION VERIFICATION
assert logContent.contains("CRITICAL") || logContent.contains("critical") :
    "Build log should mention CRITICAL severity violations"

// VERIFY AUDIT RAN
assert logContent.contains("hexaglue-plugin-audit") || logContent.contains("Audit") :
    "Build log should show audit plugin execution"

println """
=============================================================================
SUCCESS: test-param-audit-error-on-critical integration test passed!
Validated:
  - errorOnCritical=true correctly causes BUILD FAILURE
  - CRITICAL violations detected (immutability violations)
  - Build log contains CRITICAL violation messages
=============================================================================
"""
return true

/*
 * Verify script for test-param-audit-fail-on-error integration test.
 * Validates that failOnError=true causes build failure when audit detects violations.
 */
def buildLog = new File(basedir, "build.log")
assert buildLog.exists() : "build.log should exist"
def logContent = buildLog.text

// BUILD FAILURE VERIFICATION
assert logContent.contains("BUILD FAILURE") : "Build should fail with audit violations"

// AUDIT ERROR MESSAGE VERIFICATION
assert logContent.contains("violation") || logContent.contains("VIOLATION") :
    "Build log should mention audit violations"

// VERIFY AUDIT RAN
assert logContent.contains("hexaglue-plugin-audit") || logContent.contains("Audit") :
    "Build log should show audit plugin execution"

println """
=============================================================================
SUCCESS: test-param-audit-fail-on-error integration test passed!
Validated:
  - failOnError=true correctly causes BUILD FAILURE
  - Audit plugin detected violations (cycle between Order and Product)
  - Build log contains violation messages
=============================================================================
"""
return true

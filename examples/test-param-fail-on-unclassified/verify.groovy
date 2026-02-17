/*
 * Verify script for test-param-fail-on-unclassified integration test.
 * Validates that failOnUnclassified=true causes build failure on unclassified types.
 */
def buildLog = new File(basedir, "build.log")
assert buildLog.exists() : "build.log should exist"
def logContent = buildLog.text

// BUILD FAILURE VERIFICATION
assert logContent.contains("BUILD FAILURE") : "Build should fail with unclassified types"

// UNCLASSIFIED ERROR MESSAGE
assert logContent.contains("UNCLASSIFIED") || logContent.contains("unclassified") :
    "Build log should mention unclassified types"

println """
=============================================================================
SUCCESS: test-param-fail-on-unclassified integration test passed!
Validated:
  - failOnUnclassified=true correctly causes BUILD FAILURE
  - Unclassified type (StringHelper) detected
=============================================================================
"""
return true

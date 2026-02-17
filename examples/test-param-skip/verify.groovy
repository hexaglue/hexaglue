/*
 * Verify script for test-param-skip integration test.
 * Validates that skip=true disables HexaGlue analysis.
 */
def buildLog = new File(basedir, "build.log")
assert buildLog.exists() : "build.log should exist"
def logContent = buildLog.text

// BUILD VERIFICATION
assert logContent.contains("BUILD SUCCESS") : "Build should succeed"
assert !logContent.contains("BUILD FAILURE") : "Build should not fail"

// SKIP VERIFICATION
assert !logContent.contains("HexaGlue analyzing:") :
    "HexaGlue analysis should be skipped when skip=true"

println """
=============================================================================
SUCCESS: test-param-skip integration test passed!
Validated:
  - skip=true correctly disables HexaGlue analysis
  - No 'HexaGlue analyzing:' message in build log
=============================================================================
"""
return true

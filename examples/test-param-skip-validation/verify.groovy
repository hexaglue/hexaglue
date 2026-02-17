/*
 * Verify script for test-param-skip-validation integration test.
 * Validates that skipValidation=true skips validation phase.
 */
def buildLog = new File(basedir, "build.log")
assert buildLog.exists() : "build.log should exist"
def logContent = buildLog.text

assert logContent.contains("BUILD SUCCESS") : "Build should succeed"
assert !logContent.contains("BUILD FAILURE") : "Build should not fail"

// SKIP VALIDATION VERIFICATION
assert !logContent.contains("Validation complete") :
    "Validation should be skipped when skipValidation=true"

// HexaGlue should still run (just skip validation phase)
assert logContent.contains("HexaGlue analyzing:") :
    "HexaGlue analysis should still run"

println """
=============================================================================
SUCCESS: test-param-skip-validation integration test passed!
Validated:
  - skipValidation=true correctly skips validation phase
  - HexaGlue analysis still runs
=============================================================================
"""
return true

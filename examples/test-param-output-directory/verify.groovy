/*
 * Verify script for test-param-output-directory integration test.
 * Validates that outputDirectory redirects generated files.
 */
def buildLog = new File(basedir, "build.log")
assert buildLog.exists() : "build.log should exist"
def logContent = buildLog.text

assert logContent.contains("BUILD SUCCESS") : "Build should succeed"
assert !logContent.contains("BUILD FAILURE") : "Build should not fail"

// CUSTOM OUTPUT DIRECTORY VERIFICATION
def customOutput = new File(basedir, "target/custom-output")
assert customOutput.exists() : "Custom output directory target/custom-output/ should exist"

// Verify JPA files generated in custom location
def infraDir = new File(customOutput, "com/example/infrastructure/persistence")
assert infraDir.exists() : "Infrastructure persistence package should exist in custom output"

def entityFile = new File(infraDir, "BookEntity.java")
assert entityFile.exists() : "BookEntity.java should exist in custom output directory"

// Verify NOT in default location
def defaultOutput = new File(basedir, "target/generated-sources/hexaglue/com/example/infrastructure/persistence")
assert !defaultOutput.exists() : "Files should NOT be in default target/generated-sources/hexaglue/"

println """
=============================================================================
SUCCESS: test-param-output-directory integration test passed!
Validated:
  - outputDirectory correctly redirected to target/custom-output/
  - BookEntity.java generated in custom location
  - Files NOT in default target/generated-sources/hexaglue/
=============================================================================
"""
return true

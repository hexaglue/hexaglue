/*
 * Verify script for simple-domain integration test.
 */

def buildLog = new File(basedir, "build.log")
assert buildLog.exists() : "build.log should exist"

def logContent = buildLog.text

// Verify plugin executed
assert logContent.contains("HexaGlue analyzing: com.example") :
    "Should contain 'HexaGlue analyzing: com.example'"

// Verify analysis completed with at least 2 types
assert logContent.contains("Analysis complete:") :
    "Should contain 'Analysis complete:'"

// Check generated sources directory
def generatedSources = new File(basedir, "target/generated-sources/hexaglue")
assert generatedSources.exists() :
    "Generated sources directory should exist"

println "SUCCESS: simple-domain test passed"
return true

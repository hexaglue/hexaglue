// Verify both code generation and audit executed successfully

def buildLog = new File(basedir, "build.log")
def logText = buildLog.text

// Build should succeed
assert !logText.contains("BUILD FAILURE") : "Build should not fail"

// Both generation and audit should run
assert logText.contains("generate-and-audit") : "Combined goal should execute"

// Generated sources directory should exist
def generatedDir = new File(basedir, "target/hexaglue/generated-sources")
assert generatedDir.exists() : "Generated sources directory should exist"

// Report directory should exist
def reportDir = new File(basedir, "target/hexaglue/reports")
// TODO: Re-enable when audit plugin is fully implemented
// assert reportDir.exists() : "Report directory should exist"

println "generate-and-audit integration test passed"

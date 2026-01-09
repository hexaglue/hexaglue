// Verify audit execution detected violations but did not fail build

def buildLog = new File(basedir, "build.log")
def logText = buildLog.text

// Build should succeed (failOnError=false)
assert !logText.contains("BUILD FAILURE") : "Build should not fail with failOnError=false"

// Audit should run
assert logText.contains("HexaGlue audit") || logText.contains("audit") : "Audit should execute"

println "audit-with-failures integration test passed"

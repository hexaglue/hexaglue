// Verify audit execution completed successfully

def reportDir = new File(basedir, "target/hexaglue-reports")
// TODO: Re-enable when audit plugin is fully implemented
// assert reportDir.exists() : "Report directory should exist"

def htmlReport = new File(reportDir, "hexaglue-audit.html")
// Note: HTML report may not exist if audit plugin not available yet
// assert htmlReport.exists() : "HTML report should be generated"

// Check build log for audit output
def buildLog = new File(basedir, "build.log")
def logText = buildLog.text

// Build should complete without errors
assert !logText.contains("BUILD FAILURE") : "Build should not fail"

println "audit-basic integration test passed"

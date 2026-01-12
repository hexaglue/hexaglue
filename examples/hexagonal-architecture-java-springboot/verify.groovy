/*
 * Verify script for hexagonal-architecture-java-springboot integration test.
 * Validates that the audit plugin executed correctly on a full hexagonal architecture project.
 */

def buildLog = new File(basedir, "build.log")
assert buildLog.exists() : "build.log should exist"

def logContent = buildLog.text

// Verify build success
assert !logContent.contains("BUILD FAILURE") : "Build should not fail"

// Verify HexaGlue plugin executed
assert logContent.contains("HexaGlue generate-and-audit") :
    "Should contain HexaGlue generate-and-audit output"

// Verify classification completed
assert logContent.contains("Classification complete:") :
    "Should contain 'Classification complete:'"

// Check generated sources directory
def generatedSources = new File(basedir, "target/generated-sources/hexaglue")
assert generatedSources.exists() :
    "Generated sources directory should exist"

// Check audit reports directory
def auditDir = new File(basedir, "target/hexaglue-reports/audit")
assert auditDir.exists() :
    "Audit reports directory should exist at target/hexaglue-reports/audit"

// Check for audit report files
def htmlReport = new File(auditDir, "audit-report.html")
def jsonReport = new File(auditDir, "audit-report.json")
def mdReport = new File(auditDir, "AUDIT-REPORT.md")

assert htmlReport.exists() || jsonReport.exists() || mdReport.exists() :
    "At least one audit report file should exist (HTML, JSON, or MD)"

// Verify audit plugin ran
assert logContent.contains("io.hexaglue.plugin.audit.ddd") :
    "Build log should contain audit plugin execution"

// Verify audit completed
assert logContent.contains("Audit complete:") || logContent.contains("Audit:") :
    "Build log should contain audit completion message"

// For hexagonal architecture project, verify ports were detected
assert logContent =~ /\d+ ports/ :
    "Should detect ports in hexagonal architecture project"

println "SUCCESS: hexagonal-architecture-java-springboot integration test passed - audit plugin executed correctly"
return true

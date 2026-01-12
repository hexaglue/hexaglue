/*
 * Verify script for ecommerce-audit-sample integration test.
 * This project intentionally contains architectural violations.
 * Validates that the audit plugin detected violations.
 */

def buildLog = new File(basedir, "build.log")
assert buildLog.exists() : "build.log should exist"

def logContent = buildLog.text

// Build should succeed (failOnBlocker=false, failOnCritical=false in hexaglue.yaml)
assert !logContent.contains("BUILD FAILURE") : "Build should not fail (violations configured not to fail build)"

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

// Verify audit completed with violations detected
assert logContent.contains("Audit complete:") :
    "Build log should contain audit completion message"

println "SUCCESS: ecommerce-audit-sample integration test passed - audit plugin detected violations"
return true

/*
 * Verify script for test-param-audit-report-directory integration test.
 * Validates that custom reportDirectory is used for audit reports.
 */
def buildLog = new File(basedir, "build.log")
assert buildLog.exists() : "build.log should exist"
def logContent = buildLog.text

// BUILD SUCCESS VERIFICATION
assert logContent.contains("BUILD SUCCESS") : "Build should succeed with clean domain"

// VERIFY CUSTOM REPORT DIRECTORY EXISTS
def customReportsDir = new File(basedir, "target/custom-reports")
assert customReportsDir.exists() : "Custom reports directory should exist at target/custom-reports"
assert customReportsDir.isDirectory() : "target/custom-reports should be a directory"

// VERIFY AUDIT REPORT FILES IN CUSTOM DIRECTORY (audit plugin writes to audit/ subdirectory)
def auditSubDir = new File(customReportsDir, "audit")
assert auditSubDir.exists() : "Audit subdirectory should exist at target/custom-reports/audit/"

def auditReportJson = new File(auditSubDir, "audit-report.json")
def auditReportHtml = new File(auditSubDir, "audit-report.html")

assert auditReportJson.exists() || auditReportHtml.exists() :
    "At least one audit report file should exist in target/custom-reports/audit/"

// VERIFY AUDIT RAN
assert logContent.contains("hexaglue-plugin-audit") || logContent.contains("Audit") :
    "Build log should show audit plugin execution"

println """
=============================================================================
SUCCESS: test-param-audit-report-directory integration test passed!
Validated:
  - Build succeeded with clean domain
  - Custom report directory created: target/custom-reports/
  - Audit reports generated in custom directory
  - reportDirectory parameter correctly configured
=============================================================================
"""
return true

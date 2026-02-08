/*
 * Verify script for sample-multimodule integration test.
 * Validates reactor execution, convention-based role detection, JPA auto-routing,
 * JPA generation completeness, and report content.
 */

def buildLog = new File(basedir, "build.log")
assert buildLog.exists() : "build.log should exist"

def logContent = buildLog.text

// ── Build success ──────────────────────────────────────────────────────────────
assert !logContent.contains("BUILD FAILURE") : "Build should not fail"

// ── Reactor goals executed ─────────────────────────────────────────────────────
assert logContent.contains("reactor-generate") :
    "Build log should contain reactor-generate execution"

assert logContent.contains("reactor-audit") :
    "Build log should contain reactor-audit execution"

// ── Convention-based role detection ────────────────────────────────────────────
assert logContent.contains("detected by convention") :
    "Build log should show module roles detected by convention"

// ── JPA auto-routing to infrastructure module ──────────────────────────────────
assert logContent.contains("JPA auto-routing") :
    "Build log should show JPA auto-routing to infrastructure module"

// ── JPA generation summary ─────────────────────────────────────────────────────
assert logContent.contains("JPA generation complete:") :
    "Build log should contain JPA generation completion message"

assert logContent.contains("2 entities") :
    "Build log should report 2 entities (AccountEntity + TransactionEntity)"

assert logContent.contains("1 embeddables") :
    "Build log should report 1 embeddable (AddressEmbeddable)"

assert logContent.contains("1 repositories") :
    "Build log should report 1 repository (AccountJpaRepository)"

assert logContent.contains("1 mappers") :
    "Build log should report 1 mapper (AccountMapper)"

assert logContent.contains("1 adapters") :
    "Build log should report 1 adapter (AccountRepositoryAdapter)"

// ── Child entity discovery ─────────────────────────────────────────────────────
assert logContent.contains("child entity types from aggregate roots") :
    "Build log should show child entity discovery from aggregate root collections"

// ── Embeddable generation ──────────────────────────────────────────────────────
assert logContent.contains("Generated embeddable:") :
    "Build log should show embeddable generation"

// ── Generated sources in parent target directory ─────────────────────────────
def generatedSourcesDir = new File(basedir, "target/generated-sources/hexaglue/modules/banking-infrastructure")
assert generatedSourcesDir.exists() :
    "Generated sources should exist under parent target/generated-sources/hexaglue/modules/"

def generatedFiles = []
generatedSourcesDir.eachFileRecurse { if (it.name.endsWith('.java')) generatedFiles << it.name }

assert generatedFiles.contains("AccountEntity.java") :
    "AccountEntity.java should be generated under parent target"

assert generatedFiles.contains("TransactionEntity.java") :
    "TransactionEntity.java should be generated under parent target"

// ── Compiled classes in child module target ──────────────────────────────────
def infraClassesDir = new File(basedir, "banking-infrastructure/target/classes")
assert infraClassesDir.exists() :
    "banking-infrastructure/target/classes should exist after compilation"

def compiledClasses = []
infraClassesDir.eachFileRecurse { if (it.name.endsWith('.class')) compiledClasses << it.name }

assert compiledClasses.contains("AccountEntity.class") :
    "AccountEntity.class should exist in banking-infrastructure (proof of successful compilation)"

// ── Reports generated at reactor level ─────────────────────────────────────────
def reportsDir = new File(basedir, "target/hexaglue/reports")
assert reportsDir.exists() :
    "Reports directory should exist at reactor level"

// ── Audit report generated ─────────────────────────────────────────────────────
def auditReport = new File(basedir, "target/hexaglue/reports/audit/audit-report.html")
assert auditReport.exists() :
    "Audit HTML report should exist at reactor level"

// ── Audit JSON report content ──────────────────────────────────────────────────
def auditJson = new File(basedir, "target/hexaglue/reports/audit/audit-report.json")
assert auditJson.exists() :
    "Audit JSON report should exist at reactor level"

def auditJsonContent = auditJson.text

assert auditJsonContent.contains("moduleTopology") :
    "Audit JSON should contain moduleTopology section"

assert auditJsonContent.contains("banking-domain") :
    "Audit JSON should reference banking-domain module"

assert auditJsonContent.contains("banking-infrastructure") :
    "Audit JSON should reference banking-infrastructure module"

assert auditJsonContent.contains("DOMAIN") :
    "Audit JSON should contain DOMAIN role"

assert auditJsonContent.contains("INFRASTRUCTURE") :
    "Audit JSON should contain INFRASTRUCTURE role"

// ── Living documentation generated ─────────────────────────────────────────────
def livingDocDir = new File(basedir, "target/hexaglue/reports/living-doc")
assert livingDocDir.exists() :
    "Living documentation directory should exist at reactor level"

// ── Living documentation modules.md content ────────────────────────────────────
def modulesDoc = new File(basedir, "target/hexaglue/reports/living-doc/modules.md")
assert modulesDoc.exists() :
    "Living documentation modules.md should exist"

def modulesContent = modulesDoc.text

assert modulesContent.contains("banking-domain") :
    "modules.md should reference banking-domain module"

assert modulesContent.contains("banking-infrastructure") :
    "modules.md should reference banking-infrastructure module"

assert modulesContent.contains("DOMAIN") :
    "modules.md should contain DOMAIN role"

assert modulesContent.contains("INFRASTRUCTURE") :
    "modules.md should contain INFRASTRUCTURE role"

println 'SUCCESS: sample-multimodule integration test passed - reactor execution, convention detection, JPA auto-routing, generation completeness, and report content verified'
return true

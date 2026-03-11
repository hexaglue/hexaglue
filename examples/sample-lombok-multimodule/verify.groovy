/*
 * Verify script for sample-lombok-multimodule integration test.
 * Validates that delombok works correctly in a multi-module project where
 * Lombok is declared at the parent POM level (inherited by children).
 * Focus: constructor annotations (@RequiredArgsConstructor, @NoArgsConstructor).
 */

def buildLog = new File(basedir, "build.log")
assert buildLog.exists() : "build.log should exist"

def logContent = buildLog.text

// ── Build success ──────────────────────────────────────────────────────────────
assert !logContent.contains("BUILD FAILURE") : "Build should not fail"

// ── Delombok execution ─────────────────────────────────────────────────────────
assert logContent.contains("hexaglue-delombok") :
    "Build log should contain delombok execution"

// ── Delombok output on child module ────────────────────────────────────────────
def delombokDir = new File(basedir, "library-domain/target/hexaglue/delombok-sources")
assert delombokDir.exists() :
    "Delombok sources should exist for library-domain (jar-packaged child)"

def delombokFiles = []
delombokDir.eachFileRecurse { if (it.name.endsWith('.java')) delombokFiles << it.name }
assert !delombokFiles.isEmpty() :
    "Delombok should have produced Java files for library-domain"

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

assert logContent.contains("1 entities") :
    "Build log should report 1 entity (BookEntity)"

assert logContent.contains("2 embeddables") :
    "Build log should report 2 embeddables (ChapterEmbeddable + GenreEmbeddable)"

assert logContent.contains("1 repositories") :
    "Build log should report 1 repository (BookJpaRepository)"

assert logContent.contains("1 mappers") :
    "Build log should report 1 mapper (BookMapper)"

assert logContent.contains("1 adapters") :
    "Build log should report 1 adapter (BookRepositoryAdapter)"

// ── Generated sources in parent target directory ─────────────────────────────
def generatedSourcesDir = new File(basedir, "target/generated-sources/hexaglue/modules/library-infrastructure")
assert generatedSourcesDir.exists() :
    "Generated sources should exist under parent target/generated-sources/hexaglue/modules/"

def generatedFiles = []
generatedSourcesDir.eachFileRecurse { if (it.name.endsWith('.java')) generatedFiles << it.name }

assert generatedFiles.contains("BookEntity.java") :
    "BookEntity.java should be generated under parent target"

// ── Compiled classes in child module target ──────────────────────────────────
def infraClassesDir = new File(basedir, "library-infrastructure/target/classes")
assert infraClassesDir.exists() :
    "library-infrastructure/target/classes should exist after compilation"

def compiledClasses = []
infraClassesDir.eachFileRecurse { if (it.name.endsWith('.class')) compiledClasses << it.name }

assert compiledClasses.contains("BookEntity.class") :
    "BookEntity.class should exist in library-infrastructure (proof of successful compilation)"

// ── Generated content validation ─────────────────────────────────────────────
def bookEntityFiles = []
generatedSourcesDir.eachFileRecurse { if (it.name == 'BookEntity.java') bookEntityFiles << it }
assert !bookEntityFiles.isEmpty() :
    "BookEntity.java should exist in generated sources"

def bookEntityContent = bookEntityFiles[0].text
assert bookEntityContent.contains("@Entity") :
    "BookEntity.java should contain @Entity annotation"
assert bookEntityContent.contains("@Id") :
    "BookEntity.java should contain @Id annotation"

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

assert auditJsonContent.contains("library-domain") :
    "Audit JSON should reference library-domain module"

assert auditJsonContent.contains("library-infrastructure") :
    "Audit JSON should reference library-infrastructure module"

// ── Living documentation generated ─────────────────────────────────────────────
def livingDocDir = new File(basedir, "target/hexaglue/reports/living-doc")
assert livingDocDir.exists() :
    "Living documentation directory should exist at reactor level"

println 'SUCCESS: sample-lombok-multimodule integration test passed - delombok on children, reactor execution, convention detection, JPA auto-routing, generation, and reports verified'
return true

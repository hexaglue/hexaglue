import java.nio.file.Files
import java.nio.file.Paths

def baseDir = basedir.toPath()
def buildLog = baseDir.resolve("build.log").toFile()
def auditDir = baseDir.resolve("target/hexaglue/reports/audit").toFile()

// Check build success
def buildLogContent = buildLog.text
assert buildLogContent.contains("BUILD SUCCESS"), "Build should succeed"

// Check audit directory exists
assert auditDir.exists(), "audit reports directory should exist when generateDocs is true"

// Check for some audit-related files (diagrams, reports, etc.)
def hasAuditFiles = auditDir.listFiles().any { file ->
    file.name.endsWith(".md") || file.name.endsWith(".html") || file.name.endsWith(".mmd")
}

println "✓ Build succeeded"
println "✓ Audit directory exists: ${auditDir.absolutePath}"
if (hasAuditFiles) {
    println "✓ Audit documentation files generated"
}

return true

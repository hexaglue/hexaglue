import java.nio.file.Files
import java.nio.file.Paths

def baseDir = basedir.toPath()
def buildLog = baseDir.resolve("build.log").toFile()
def customDocsDir = baseDir.resolve("target/hexaglue/reports/custom-docs").toFile()
def defaultLivingDocDir = baseDir.resolve("target/hexaglue/reports/living-doc").toFile()

// Check build success
def buildLogContent = buildLog.text
assert buildLogContent.contains("BUILD SUCCESS"), "Build should succeed"

// Check custom-docs directory exists
assert customDocsDir.exists(), "custom-docs directory should exist"
assert customDocsDir.list().length > 0, "custom-docs directory should contain files"

// Check default living-doc directory does NOT exist
assert !defaultLivingDocDir.exists(), "Default living-doc directory should NOT exist when outputDir is customized"

println "✓ Build succeeded"
println "✓ Files generated in custom directory: ${customDocsDir.absolutePath}"
println "✓ Default living-doc directory correctly not created"

return true

import java.nio.file.Files
import java.nio.file.Paths

def baseDir = basedir.toPath()
def buildLog = baseDir.resolve("build.log").toFile()
def domainDocFile = baseDir.resolve("target/hexaglue/reports/living-doc/domain.md").toFile()

// Check build success
def buildLogContent = buildLog.text
assert buildLogContent.contains("BUILD SUCCESS"), "Build should succeed"

// Check domain.md exists
assert domainDocFile.exists(), "domain.md should be generated"

// Check for debug sections
def domainContent = domainDocFile.text
def hasDebugInfo = domainContent.contains("Classification Trace") ||
                   domainContent.contains("Source Location") ||
                   domainContent.contains("Debug")

assert hasDebugInfo, "domain.md should contain debug sections when includeDebugSections is true"

println "✓ Build succeeded"
println "✓ domain.md contains debug sections"

return true

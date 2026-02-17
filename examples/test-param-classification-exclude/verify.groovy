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

// Check that StringHelper is NOT mentioned
def domainContent = domainDocFile.text
assert !domainContent.contains("StringHelper"), "StringHelper should be excluded from classification and documentation"

println "✓ Build succeeded"
println "✓ StringHelper correctly excluded from classification"

return true

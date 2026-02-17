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

// Check that BookId is mentioned and classified as VALUE_OBJECT
def domainContent = domainDocFile.text
assert domainContent.contains("BookId"), "BookId should appear in documentation"
assert domainContent.contains("VALUE_OBJECT") || domainContent.contains("Value Object"),
    "BookId should be classified as VALUE_OBJECT"

println "✓ Build succeeded"
println "✓ BookId explicitly classified as VALUE_OBJECT"

return true

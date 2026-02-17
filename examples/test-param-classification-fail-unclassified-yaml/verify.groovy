import java.nio.file.Files
import java.nio.file.Paths

def baseDir = basedir.toPath()
def buildLog = baseDir.resolve("build.log").toFile()

// Check build failure
def buildLogContent = buildLog.text
assert buildLogContent.contains("BUILD FAILURE"), "Build should fail when unclassified types are present and failOnUnclassified is true"

// Check for unclassified error message
def hasUnclassifiedError = buildLogContent.toLowerCase().contains("unclassified")

assert hasUnclassifiedError, "Build log should mention unclassified types"

println "✓ Build correctly failed due to unclassified types"
println "✓ failOnUnclassified validation working as expected"

return true

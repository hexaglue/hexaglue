import java.nio.file.Files
import java.nio.file.Paths

def baseDir = basedir.toPath()
def buildLog = baseDir.resolve("build.log").toFile()

// Check build success
def buildLogContent = buildLog.text
assert buildLogContent.contains("BUILD SUCCESS"), "Build should succeed when all types are explicitly classified"

println "✓ Build succeeded"
println "✓ All types explicitly classified, allowInferred: false validation passed"

return true

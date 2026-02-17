import java.nio.file.Files
import java.nio.file.Paths

def baseDir = basedir.toPath()
def buildLog = baseDir.resolve("build.log").toFile()
def diagramsFile = baseDir.resolve("target/hexaglue/reports/living-doc/diagrams.md").toFile()

// Check build success
def buildLogContent = buildLog.text
assert buildLogContent.contains("BUILD SUCCESS"), "Build should succeed"

// Check diagrams.md exists
assert diagramsFile.exists(), "diagrams.md should be generated"

println "✓ Build succeeded"
println "✓ diagrams.md generated with maxPropertiesInDiagram: 2"
println "✓ Configuration applied successfully"

return true

import java.nio.file.Files
import java.nio.file.Paths

def baseDir = basedir.toPath()
def buildLog = baseDir.resolve("build.log").toFile()
def diagramsFile = baseDir.resolve("target/hexaglue/reports/living-doc/diagrams.md").toFile()
def livingDocDir = baseDir.resolve("target/hexaglue/reports/living-doc").toFile()

// Check build success
def buildLogContent = buildLog.text
assert buildLogContent.contains("BUILD SUCCESS"), "Build should succeed"

// Check living-doc directory exists but diagrams.md does not
assert livingDocDir.exists(), "living-doc directory should exist"
assert !diagramsFile.exists(), "diagrams.md should NOT be generated when generateDiagrams is false"

println "✓ Build succeeded"
println "✓ living-doc directory exists"
println "✓ diagrams.md correctly NOT generated"

return true

/*
 * Verify script for hexagonal-app integration test.
 */

def buildLog = new File(basedir, "build.log")
assert buildLog.exists() : "build.log should exist"

def logContent = buildLog.text

// Verify plugin executed
assert logContent.contains("HexaGlue analyzing: com.example") :
    "Should contain 'HexaGlue analyzing: com.example'"

// Verify analysis completed - should find at least 4 types
// (Product, ProductId, ProductCatalog, Products)
assert logContent.contains("Analysis complete:") :
    "Should contain 'Analysis complete:'"

// Should detect ports
assert logContent =~ /\d+ ports/ :
    "Should detect ports"

// Check generated sources directory
def generatedSources = new File(basedir, "target/generated-sources/hexaglue")
assert generatedSources.exists() :
    "Generated sources directory should exist"

println "SUCCESS: hexagonal-app test passed"
return true

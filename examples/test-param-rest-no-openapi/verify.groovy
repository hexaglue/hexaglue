def buildLog = new File(basedir, 'build.log').text

// Assert build succeeded (proves code compiles without OpenAPI dependency)
assert buildLog.contains('BUILD SUCCESS'): 'Build should succeed without OpenAPI dependency'

// Verify REST plugin executed
assert buildLog.contains('io.hexaglue.plugin.rest'):
    'Build log should contain REST plugin execution'

// Check that BookController.java was generated
def bookController = new File(basedir, 'target/generated-sources/hexaglue/com/example/api/controller/BookController.java')
assert bookController.exists(): 'BookController.java should be generated'

// Verify generated source does NOT contain OpenAPI annotations
def source = bookController.text
assert !source.contains('@Tag'): 'Generated source should NOT contain @Tag annotation'
assert !source.contains('@Operation'): 'Generated source should NOT contain @Operation annotation'
assert source.contains('@RestController'): 'Generated source should still contain @RestController'

println "✓ BUILD SUCCESS (compiles without OpenAPI dependency)"
println "✓ BookController.java generated"
println "✓ No @Tag annotation (OpenAPI annotations disabled)"
println "✓ No @Operation annotation (OpenAPI annotations disabled)"
println "✓ @RestController still present"

return true

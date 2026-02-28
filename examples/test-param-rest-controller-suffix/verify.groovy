def buildLog = new File(basedir, 'build.log').text

// Assert build succeeded
assert buildLog.contains('BUILD SUCCESS'): 'Build should succeed'

// Verify REST plugin executed
assert buildLog.contains('io.hexaglue.plugin.rest'):
    'Build log should contain REST plugin execution'

// Check that BookRestApi.java was generated (custom controllerSuffix)
def bookRestApi = new File(basedir, 'target/hexaglue/generated-sources/com/example/api/controller/BookRestApi.java')
assert bookRestApi.exists(): 'BookRestApi.java should be generated with custom controllerSuffix'

// Check that default BookController.java was NOT generated
def bookController = new File(basedir, 'target/hexaglue/generated-sources/com/example/api/controller/BookController.java')
assert !bookController.exists(): 'BookController.java should NOT be generated (default suffix should be overridden)'

// Verify generated source contains the custom class name
def source = bookRestApi.text
assert source.contains('class BookRestApi'): 'Generated source should contain class BookRestApi'

println "✓ BUILD SUCCESS"
println "✓ BookRestApi.java generated (custom controllerSuffix applied)"
println "✓ BookController.java NOT generated (default suffix overridden)"
println "✓ Generated source contains 'class BookRestApi'"

return true

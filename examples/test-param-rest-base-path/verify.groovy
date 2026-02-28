def buildLog = new File(basedir, 'build.log').text

// Assert build succeeded
assert buildLog.contains('BUILD SUCCESS'): 'Build should succeed'

// Verify REST plugin executed
assert buildLog.contains('io.hexaglue.plugin.rest'):
    'Build log should contain REST plugin execution'

// Check that BookController.java was generated
def bookController = new File(basedir, 'target/hexaglue/generated-sources/com/example/api/controller/BookController.java')
assert bookController.exists(): 'BookController.java should be generated'

// Verify generated source contains custom base path "/v2/books"
def source = bookController.text
assert source.contains('"/v2/books"'): 'Generated source should contain custom base path "/v2/books"'
assert !source.contains('"/api/books"'): 'Generated source should NOT contain default base path "/api/books"'

println "✓ BUILD SUCCESS"
println "✓ BookController.java generated"
println '✓ Source contains "/v2/books" (custom basePath applied)'
println '✓ Source does NOT contain "/api/books" (default basePath overridden)'

return true

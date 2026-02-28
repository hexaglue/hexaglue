def buildLog = new File(basedir, 'build.log').text

// Assert build succeeded
assert buildLog.contains('BUILD SUCCESS'): 'Build should succeed'

// Verify REST plugin executed
assert buildLog.contains('io.hexaglue.plugin.rest'):
    'Build log should contain REST plugin execution'

// Check that BookController.java was generated
def bookController = new File(basedir, 'target/generated-sources/hexaglue/com/example/api/controller/BookController.java')
assert bookController.exists(): 'BookController.java should be generated'

// Check that GlobalExceptionHandler.java was NOT generated
def exceptionHandler = new File(basedir, 'target/generated-sources/hexaglue/com/example/api/exception/GlobalExceptionHandler.java')
assert !exceptionHandler.exists(): 'GlobalExceptionHandler.java should NOT be generated when generateExceptionHandler=false'

println "✓ BUILD SUCCESS"
println "✓ BookController.java generated"
println "✓ GlobalExceptionHandler.java NOT generated (generateExceptionHandler=false)"

return true

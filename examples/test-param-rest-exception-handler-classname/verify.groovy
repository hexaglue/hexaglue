def buildLog = new File(basedir, 'build.log').text

// Assert build succeeded
assert buildLog.contains('BUILD SUCCESS'): 'Build should succeed'

// Verify REST plugin executed
assert buildLog.contains('io.hexaglue.plugin.rest'):
    'Build log should contain REST plugin execution'

// Check that BookApiExceptionHandler.java was generated (custom className)
def customHandler = new File(basedir, 'target/hexaglue/generated-sources/com/example/api/exception/BookApiExceptionHandler.java')
assert customHandler.exists(): 'BookApiExceptionHandler.java should be generated with custom exceptionHandlerClassName'

// Check that default GlobalExceptionHandler.java was NOT generated
def defaultHandler = new File(basedir, 'target/hexaglue/generated-sources/com/example/api/exception/GlobalExceptionHandler.java')
assert !defaultHandler.exists(): 'GlobalExceptionHandler.java should NOT be generated (default className should be overridden)'

println "✓ BUILD SUCCESS"
println "✓ BookApiExceptionHandler.java generated (custom exceptionHandlerClassName applied)"
println "✓ GlobalExceptionHandler.java NOT generated (default className overridden)"

return true

def buildLog = new File(basedir, 'build.log').text

// Assert build succeeded
assert buildLog.contains('BUILD SUCCESS'): 'Build should succeed'

// Verify REST plugin executed
assert buildLog.contains('io.hexaglue.plugin.rest'):
    'Build log should contain REST plugin execution'

// Check that files exist under custom package com.example.rest
def bookController = new File(basedir, 'target/generated-sources/hexaglue/com/example/rest/controller/BookController.java')
assert bookController.exists(): 'BookController.java should be generated under com.example.rest.controller'

def createBookRequest = new File(basedir, 'target/generated-sources/hexaglue/com/example/rest/dto/CreateBookRequest.java')
assert createBookRequest.exists(): 'CreateBookRequest.java should be generated under com.example.rest.dto'

def exceptionHandler = new File(basedir, 'target/generated-sources/hexaglue/com/example/rest/exception/GlobalExceptionHandler.java')
assert exceptionHandler.exists(): 'GlobalExceptionHandler.java should be generated under com.example.rest.exception'

// Check that files do NOT exist under default package com.example.api
def defaultController = new File(basedir, 'target/generated-sources/hexaglue/com/example/api/controller/BookController.java')
assert !defaultController.exists(): 'BookController.java should NOT be generated under default com.example.api.controller'

def defaultDto = new File(basedir, 'target/generated-sources/hexaglue/com/example/api/dto/CreateBookRequest.java')
assert !defaultDto.exists(): 'CreateBookRequest.java should NOT be generated under default com.example.api.dto'

def defaultException = new File(basedir, 'target/generated-sources/hexaglue/com/example/api/exception/GlobalExceptionHandler.java')
assert !defaultException.exists(): 'GlobalExceptionHandler.java should NOT be generated under default com.example.api.exception'

println "✓ BUILD SUCCESS"
println "✓ BookController.java generated under com.example.rest.controller"
println "✓ CreateBookRequest.java generated under com.example.rest.dto"
println "✓ GlobalExceptionHandler.java generated under com.example.rest.exception"
println "✓ No files generated under default com.example.api (custom apiPackage applied)"

return true

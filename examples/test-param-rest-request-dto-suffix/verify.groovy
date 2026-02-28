def buildLog = new File(basedir, 'build.log').text

// Assert build succeeded
assert buildLog.contains('BUILD SUCCESS'): 'Build should succeed'

// Verify REST plugin executed
assert buildLog.contains('io.hexaglue.plugin.rest'):
    'Build log should contain REST plugin execution'

// Check that request DTOs use custom "Cmd" suffix
def createBookCmd = new File(basedir, 'target/hexaglue/generated-sources/com/example/api/dto/CreateBookCmd.java')
assert createBookCmd.exists(): 'CreateBookCmd.java should be generated with custom requestDtoSuffix'

def updateBookCmd = new File(basedir, 'target/hexaglue/generated-sources/com/example/api/dto/UpdateBookCmd.java')
assert updateBookCmd.exists(): 'UpdateBookCmd.java should be generated with custom requestDtoSuffix'

// Check that default *Request.java was NOT generated
def createBookRequest = new File(basedir, 'target/hexaglue/generated-sources/com/example/api/dto/CreateBookRequest.java')
assert !createBookRequest.exists(): 'CreateBookRequest.java should NOT be generated (default suffix should be overridden)'

def updateBookRequest = new File(basedir, 'target/hexaglue/generated-sources/com/example/api/dto/UpdateBookRequest.java')
assert !updateBookRequest.exists(): 'UpdateBookRequest.java should NOT be generated (default suffix should be overridden)'

println "✓ BUILD SUCCESS"
println "✓ CreateBookCmd.java generated (custom requestDtoSuffix applied)"
println "✓ UpdateBookCmd.java generated (custom requestDtoSuffix applied)"
println "✓ CreateBookRequest.java NOT generated (default suffix overridden)"

return true

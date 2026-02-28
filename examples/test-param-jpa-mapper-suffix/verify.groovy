def buildLog = new File(basedir, 'build.log').text

// Assert build succeeded
assert buildLog.contains('BUILD SUCCESS'): 'Build should succeed'

// Check that BookMap.java was generated (NOT BookMapper.java)
def bookMap = new File(basedir, 'target/generated-sources/hexaglue/com/example/infrastructure/persistence/BookMap.java')
assert bookMap.exists(): 'BookMap.java should be generated with custom mapperSuffix'

def bookMapper = new File(basedir, 'target/generated-sources/hexaglue/com/example/infrastructure/persistence/BookMapper.java')
assert !bookMapper.exists(): 'BookMapper.java should NOT be generated (default suffix should be overridden)'

println "✓ BUILD SUCCESS"
println "✓ BookMap.java generated (custom mapperSuffix applied)"
println "✓ BookMapper.java NOT generated (default suffix overridden)"

return true

def buildLog = new File(basedir, 'build.log').text

// Assert build succeeded
assert buildLog.contains('BUILD SUCCESS'): 'Build should succeed'

// Check that BookJpa.java was generated (NOT BookEntity.java)
def bookJpa = new File(basedir, 'target/hexaglue/generated-sources/com/example/infrastructure/persistence/BookJpa.java')
assert bookJpa.exists(): 'BookJpa.java should be generated with custom entitySuffix'

def bookEntity = new File(basedir, 'target/hexaglue/generated-sources/com/example/infrastructure/persistence/BookEntity.java')
assert !bookEntity.exists(): 'BookEntity.java should NOT be generated (default suffix should be overridden)'

println "✓ BUILD SUCCESS"
println "✓ BookJpa.java generated (custom entitySuffix applied)"
println "✓ BookEntity.java NOT generated (default suffix overridden)"

return true

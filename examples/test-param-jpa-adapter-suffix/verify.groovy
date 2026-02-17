def buildLog = new File(basedir, 'build.log').text

// Assert build succeeded
assert buildLog.contains('BUILD SUCCESS'): 'Build should succeed'

// Check that BookRepositoryImpl.java was generated (NOT BookRepositoryAdapter.java)
def bookRepositoryImpl = new File(basedir, 'target/hexaglue/generated-sources/com/example/infrastructure/persistence/BookRepositoryImpl.java')
assert bookRepositoryImpl.exists(): 'BookRepositoryImpl.java should be generated with custom adapterSuffix'

def bookRepositoryAdapter = new File(basedir, 'target/hexaglue/generated-sources/com/example/infrastructure/persistence/BookRepositoryAdapter.java')
assert !bookRepositoryAdapter.exists(): 'BookRepositoryAdapter.java should NOT be generated (default suffix should be overridden)'

println "✓ BUILD SUCCESS"
println "✓ BookRepositoryImpl.java generated (custom adapterSuffix applied)"
println "✓ BookRepositoryAdapter.java NOT generated (default suffix overridden)"

return true

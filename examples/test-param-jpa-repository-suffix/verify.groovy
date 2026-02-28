def buildLog = new File(basedir, 'build.log').text

// Assert build succeeded
assert buildLog.contains('BUILD SUCCESS'): 'Build should succeed'

// Check that BookRepo.java was generated (NOT BookJpaRepository.java)
def bookRepo = new File(basedir, 'target/generated-sources/hexaglue/com/example/infrastructure/persistence/BookRepo.java')
assert bookRepo.exists(): 'BookRepo.java should be generated with custom repositorySuffix'

def bookJpaRepository = new File(basedir, 'target/generated-sources/hexaglue/com/example/infrastructure/persistence/BookJpaRepository.java')
assert !bookJpaRepository.exists(): 'BookJpaRepository.java should NOT be generated (default suffix should be overridden)'

println "✓ BUILD SUCCESS"
println "✓ BookRepo.java generated (custom repositorySuffix applied)"
println "✓ BookJpaRepository.java NOT generated (default suffix overridden)"

return true

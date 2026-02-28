def buildLog = new File(basedir, 'build.log').text

// Assert build succeeded
assert buildLog.contains('BUILD SUCCESS'): 'Build should succeed'

// Check that BookEntity.java was generated (default suffix)
def bookEntity = new File(basedir, 'target/generated-sources/hexaglue/com/example/infrastructure/persistence/BookEntity.java')
assert bookEntity.exists(): 'BookEntity.java should be generated'

// Read the entity file and verify it contains the custom table prefix
def entityContent = bookEntity.text
assert entityContent.contains('app_book'): 'Entity should contain @Table annotation with prefix app_book'

println "✓ BUILD SUCCESS"
println "✓ BookEntity.java generated"
println "✓ Table name contains custom prefix: app_book"

return true

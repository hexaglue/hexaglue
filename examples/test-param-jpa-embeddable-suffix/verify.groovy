def buildLog = new File(basedir, 'build.log').text

// Assert build succeeded
assert buildLog.contains('BUILD SUCCESS'): 'Build should succeed'

// Check that AddressEmb.java was generated (NOT AddressEmbeddable.java)
def addressEmb = new File(basedir, 'target/generated-sources/hexaglue/com/example/infrastructure/persistence/AddressEmb.java')
assert addressEmb.exists(): 'AddressEmb.java should be generated with custom embeddableSuffix'

def addressEmbeddable = new File(basedir, 'target/generated-sources/hexaglue/com/example/infrastructure/persistence/AddressEmbeddable.java')
assert !addressEmbeddable.exists(): 'AddressEmbeddable.java should NOT be generated (default suffix should be overridden)'

println "✓ BUILD SUCCESS"
println "✓ AddressEmb.java generated (custom embeddableSuffix applied)"
println "✓ AddressEmbeddable.java NOT generated (default suffix overridden)"

return true

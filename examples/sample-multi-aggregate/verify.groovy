/*
 * Verify script for sample-multi-aggregate integration test.
 * Validates that all 3 plugins (JPA, REST, LivingDoc) executed correctly
 * with 3 aggregate roots, 3 driving ports, 3 driven ports, and 3 application services.
 */

def buildLog = new File(basedir, "build.log")
assert buildLog.exists() : "build.log should exist"

def logContent = buildLog.text

// ─────────────────────────────────────────────────────────────────────
// 1. Build outcome
// ─────────────────────────────────────────────────────────────────────

assert !logContent.contains("BUILD FAILURE") : "Build should not fail"
assert logContent.contains("BUILD SUCCESS") : "Build should succeed"

// ─────────────────────────────────────────────────────────────────────
// 2. HexaGlue core pipeline
// ─────────────────────────────────────────────────────────────────────

assert logContent.contains("HexaGlue analyzing:") :
    "Should contain HexaGlue analyzing output"

assert logContent.contains("Classification complete:") :
    "Should contain classification completion"

assert logContent.contains("15 domain types") :
    "Should classify 15 domain types (3 aggregates, 6 value objects, 3 identifiers, 3 application services)"

assert logContent.contains("6 ports") :
    "Should classify 6 ports (3 driving + 3 driven)"

assert logContent.contains("0 conflicts") :
    "Should have 0 classification conflicts"

// ─────────────────────────────────────────────────────────────────────
// 3. Plugin execution (all 3 plugins)
// ─────────────────────────────────────────────────────────────────────

assert logContent.contains("Discovered 4 plugins") :
    "Should discover 4 plugins (livingdoc, rest, jpa, audit)"

assert logContent.contains("Filtered to 3 plugins matching enabled categories: [GENERATOR]") :
    "Should filter to 3 generator plugins"

assert logContent.contains("io.hexaglue.plugin.livingdoc") :
    "Build log should contain LivingDoc plugin execution"

assert logContent.contains("io.hexaglue.plugin.rest") :
    "Build log should contain REST plugin execution"

assert logContent.contains("io.hexaglue.plugin.jpa") :
    "Build log should contain JPA plugin execution"

assert logContent.contains("io.hexaglue.plugin.audit") :
    "Build log should contain Audit plugin execution"

// ─────────────────────────────────────────────────────────────────────
// 4. JPA plugin: generation summary
// ─────────────────────────────────────────────────────────────────────

assert logContent.contains("JPA generation complete:") :
    "Should contain JPA generation completion message"

assert logContent.contains("3 entities") :
    "JPA should generate 3 entities (Customer, Order, Product)"

assert logContent.contains("3 embeddables") :
    "JPA should generate 3 embeddables (Address, Money, OrderLine)"

assert logContent.contains("3 repositories") :
    "JPA should generate 3 JPA repositories"

assert logContent.contains("3 mappers") :
    "JPA should generate 3 mappers"

assert logContent.contains("3 adapters") :
    "JPA should generate 3 repository adapters"

// ─────────────────────────────────────────────────────────────────────
// 5. JPA plugin: generated file existence and content
// ─────────────────────────────────────────────────────────────────────

def jpaPackage = new File(basedir, "target/generated-sources/hexaglue/com/ecommerce/infrastructure/persistence")
assert jpaPackage.exists() : "JPA persistence package should exist"

["CustomerEntity", "OrderEntity", "ProductEntity"].each { name ->
    def entityFile = new File(jpaPackage, "${name}.java")
    assert entityFile.exists() : "${name}.java should be generated"
    def content = entityFile.text
    assert content.contains("@Entity") : "${name} should contain @Entity annotation"
    assert content.contains("@Id") : "${name} should contain @Id annotation"
    assert content.contains("@Table") : "${name} should contain @Table annotation"
    assert content.contains("io.hexaglue.plugin.jpa") : "${name} should contain HexaGlue JPA @Generated marker"
}

["AddressEmbeddable", "MoneyEmbeddable", "OrderLineEmbeddable"].each { name ->
    def embeddableFile = new File(jpaPackage, "${name}.java")
    assert embeddableFile.exists() : "${name}.java should be generated"
    assert embeddableFile.text.contains("@Embeddable") : "${name} should contain @Embeddable annotation"
}

["CustomerMapper", "OrderMapper", "ProductMapper"].each { name ->
    def mapperFile = new File(jpaPackage, "${name}.java")
    assert mapperFile.exists() : "${name}.java should be generated"
}

["CustomerRepositoryAdapter", "OrderRepositoryAdapter", "ProductRepositoryAdapter"].each { name ->
    def adapterFile = new File(jpaPackage, "${name}.java")
    assert adapterFile.exists() : "${name}.java should be generated"
    assert adapterFile.text.contains("@Component") : "${name} should contain @Component annotation"
    assert adapterFile.text.contains("implements") : "${name} should implement a repository port"
}

["CustomerJpaRepository", "OrderJpaRepository", "ProductJpaRepository"].each { name ->
    def repoFile = new File(jpaPackage, "${name}.java")
    assert repoFile.exists() : "${name}.java should be generated"
    assert repoFile.text.contains("JpaRepository") : "${name} should extend JpaRepository"
}

// ─────────────────────────────────────────────────────────────────────
// 6. REST plugin: generation summary
// ─────────────────────────────────────────────────────────────────────

assert logContent.contains("REST plugin generated @Configuration class with 3 @Bean method(s)") :
    "REST plugin should generate @Configuration with 3 @Bean methods"

assert logContent.contains("REST plugin generated 3 controller(s)") :
    "REST plugin should generate 3 controllers"

assert logContent.contains("20 DTO(s)") :
    "REST plugin should generate 20 DTOs"

// ─────────────────────────────────────────────────────────────────────
// 7. REST plugin: RestConfiguration with 3 @Bean wiring application services
// ─────────────────────────────────────────────────────────────────────

def restConfigFile = new File(basedir, "target/generated-sources/hexaglue/com/ecommerce/api/config/RestConfiguration.java")
assert restConfigFile.exists() : "RestConfiguration.java should be generated"

def restConfigContent = restConfigFile.text
assert restConfigContent.contains("@Configuration") : "RestConfiguration should have @Configuration"
assert restConfigContent.contains("@Bean") : "RestConfiguration should contain @Bean methods"
assert restConfigContent.contains("CustomerService") : "RestConfiguration should wire CustomerService"
assert restConfigContent.contains("ProductService") : "RestConfiguration should wire ProductService"
assert restConfigContent.contains("OrderService") : "RestConfiguration should wire OrderService"
assert restConfigContent.contains("ManagingCustomers") : "RestConfiguration should return ManagingCustomers port"
assert restConfigContent.contains("ManagingProducts") : "RestConfiguration should return ManagingProducts port"
assert restConfigContent.contains("OrderingProducts") : "RestConfiguration should return OrderingProducts port"

// ─────────────────────────────────────────────────────────────────────
// 8. REST plugin: controller files and content
// ─────────────────────────────────────────────────────────────────────

def controllerPackage = new File(basedir, "target/generated-sources/hexaglue/com/ecommerce/api/controller")
assert controllerPackage.exists() : "Controller package should exist"

["ManagingCustomersController", "ManagingProductsController", "OrderingProductsController"].each { name ->
    def controllerFile = new File(controllerPackage, "${name}.java")
    assert controllerFile.exists() : "${name}.java should be generated"
    def content = controllerFile.text
    assert content.contains("@RestController") : "${name} should contain @RestController annotation"
    assert content.contains("@RequestMapping") : "${name} should contain @RequestMapping annotation"
    assert content.contains("io.hexaglue.plugin.rest") : "${name} should contain HexaGlue REST @Generated marker"
}

// ─────────────────────────────────────────────────────────────────────
// 9. REST plugin: DTO files
// ─────────────────────────────────────────────────────────────────────

def dtoPackage = new File(basedir, "target/generated-sources/hexaglue/com/ecommerce/api/dto")
assert dtoPackage.exists() : "DTO package should exist"

def dtoFiles = dtoPackage.listFiles({ it.name.endsWith(".java") } as FileFilter)
assert dtoFiles != null && dtoFiles.length == 20 : "Should generate exactly 20 DTO files, found: ${dtoFiles?.length}"

// Verify key request/response DTOs
["RegisterCustomerRequest", "CustomerResponse", "CreateProductRequest", "ProductResponse",
 "CreateOrderRequest", "OrderResponse", "AddLineItemRequest"].each { name ->
    def dtoFile = new File(dtoPackage, "${name}.java")
    assert dtoFile.exists() : "${name}.java should be generated"
}

// ─────────────────────────────────────────────────────────────────────
// 10. REST plugin: GlobalExceptionHandler
// ─────────────────────────────────────────────────────────────────────

def exceptionHandler = new File(basedir, "target/generated-sources/hexaglue/com/ecommerce/api/exception/GlobalExceptionHandler.java")
assert exceptionHandler.exists() : "GlobalExceptionHandler.java should be generated"
assert exceptionHandler.text.contains("@RestControllerAdvice") : "GlobalExceptionHandler should have @RestControllerAdvice"

// ─────────────────────────────────────────────────────────────────────
// 11. LivingDoc plugin: reports
// ─────────────────────────────────────────────────────────────────────

def livingDocDir = new File(basedir, "target/hexaglue/reports/living-doc")
assert livingDocDir.exists() : "Living documentation directory should exist"

["README.md", "domain.md", "ports.md", "diagrams.md"].each { name ->
    def docFile = new File(livingDocDir, name)
    assert docFile.exists() : "Living doc ${name} should be generated"
}

def domainDoc = new File(livingDocDir, "domain.md").text
assert domainDoc.contains("Customer") : "domain.md should document Customer aggregate"
assert domainDoc.contains("Order") : "domain.md should document Order aggregate"
assert domainDoc.contains("Product") : "domain.md should document Product aggregate"
assert domainDoc.contains("Aggregate Root") : "domain.md should label aggregate roots"
assert domainDoc.contains("Value Object") : "domain.md should document value objects"

def portsDoc = new File(livingDocDir, "ports.md").text
assert portsDoc.contains("ManagingCustomers") : "ports.md should document ManagingCustomers driving port"
assert portsDoc.contains("ManagingProducts") : "ports.md should document ManagingProducts driving port"
assert portsDoc.contains("OrderingProducts") : "ports.md should document OrderingProducts driving port"
assert portsDoc.contains("CustomerRepository") : "ports.md should document CustomerRepository driven port"

assert logContent.contains("3 aggregate roots") :
    "LivingDoc should report 3 aggregate roots"

assert logContent.contains("3 driving ports") :
    "LivingDoc should report 3 driving ports"

assert logContent.contains("3 driven ports") :
    "LivingDoc should report 3 driven ports"

// ─────────────────────────────────────────────────────────────────────
// 12. Audit plugin: report and verdict
// ─────────────────────────────────────────────────────────────────────

def auditDir = new File(basedir, "target/hexaglue/reports/audit")
assert auditDir.exists() : "Audit reports directory should exist"

["audit-report.json", "audit-report.html", "AUDIT-REPORT.md"].each { name ->
    def reportFile = new File(auditDir, name)
    assert reportFile.exists() : "Audit report ${name} should be generated"
}

assert logContent.contains("0 violations") :
    "Audit should report 0 violations"

assert logContent.contains("Status: PASSED") :
    "Audit status should be PASSED"

def auditJson = new File(auditDir, "audit-report.json").text
assert auditJson.contains('"status": "PASSED"') : "Audit JSON should contain PASSED status"
assert auditJson.contains('"violations": []') || auditJson.contains('"totalViolations": 0') ||
    auditJson.contains('"score"') : "Audit JSON should be a valid report"
assert auditJson.contains('"ddd-compliance"') : "Audit JSON should contain DDD compliance KPI"
assert auditJson.contains('"hexagonal-compliance"') : "Audit JSON should contain hexagonal compliance KPI"

// ─────────────────────────────────────────────────────────────────────
// 13. Compilation: all generated + source code compiled
// ─────────────────────────────────────────────────────────────────────

assert logContent.contains("Compiling 63 source files") :
    "Should compile 63 source files (23 source + 40 generated Java files)"

assert logContent.contains("Tests run: 1, Failures: 0, Errors: 0") :
    "Spring Boot contextLoads() test should pass"

// ─────────────────────────────────────────────────────────────────────
// 14. Total generated files count
// ─────────────────────────────────────────────────────────────────────

assert logContent.contains("44 files generated") :
    "Plugins should generate 44 files total"

def generatedDir = new File(basedir, "target/generated-sources/hexaglue")
def generatedJavaFiles = []
generatedDir.eachFileRecurse { if (it.name.endsWith('.java')) generatedJavaFiles << it.name }
assert generatedJavaFiles.size() == 40 :
    "Should have 40 generated Java files, found: ${generatedJavaFiles.size()}"

println """
=============================================================================
SUCCESS: sample-multi-aggregate integration test passed!

Validated:
  - Classification: 15 domain types, 6 ports, 0 conflicts
  - JPA plugin: 3 entities, 3 embeddables, 3 repos, 3 mappers, 3 adapters
  - REST plugin: 3 controllers, 20 DTOs, RestConfiguration with 3 @Bean
  - REST @Bean wiring: CustomerService, ProductService, OrderService
  - LivingDoc plugin: domain.md, ports.md, diagrams.md, README.md
  - Audit plugin: 0 violations, PASSED, JSON/HTML/Markdown reports
  - Compilation: 63 source files, contextLoads() test green
  - Total: 44 files generated (40 Java + 4 living-doc)
=============================================================================
"""

return true

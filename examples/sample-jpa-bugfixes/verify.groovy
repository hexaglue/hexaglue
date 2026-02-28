/*
 * Verify script for sample-jpa-bugfixes integration test.
 * Validates that all JPA plugin bug fixes are correctly applied:
 * - BUG-003: Bidirectional relationship with mappedBy
 * - BUG-004: @ElementCollection with @Enumerated(STRING) for enum lists
 * - BUG-006: @CreatedDate/@LastModifiedDate auditing fields
 * - BUG-007: @Version optimistic locking field
 * - BUG-008: Entity relationships use entity types (not domain types)
 * - BUG-009: Mappers use 'uses' clause for entity relationships
 */

def buildLog = new File(basedir, "build.log")
assert buildLog.exists() : "build.log should exist"

def logContent = buildLog.text

// =============================================================================
// BUILD VERIFICATION
// =============================================================================

// Verify build success
assert !logContent.contains("BUILD FAILURE") : "Build should not fail"
assert logContent.contains("BUILD SUCCESS") : "Build should succeed"

// Verify HexaGlue plugin executed
assert logContent.contains("HexaGlue analyzing:") :
    "Should contain HexaGlue analyzing output"

// Verify classification completed
assert logContent.contains("Classification complete:") :
    "Should contain 'Classification complete:'"

// =============================================================================
// JPA PLUGIN VERIFICATION
// =============================================================================

// Verify JPA plugin ran
assert logContent.contains("io.hexaglue.plugin.jpa") :
    "Build log should contain JPA plugin execution"

// Verify JPA generation completed with correct counts
assert logContent.contains("JPA generation complete:") :
    "Build log should contain JPA generation completion message"

assert logContent.contains("4 entities") :
    "Should generate 4 entities (Student, Course, Tag, Lesson)"

assert logContent.contains("4 repositories") :
    "Should generate 4 repositories"

assert logContent.contains("4 mappers") :
    "Should generate 4 mappers"

// Verify entity mapping for BUG-008
assert logContent.contains("Built entity mapping for 4 domain types") :
    "Should build entity mapping for all domain types (BUG-008)"

// Verify bidirectional detection for BUG-003
assert logContent.contains("Detected 1 bidirectional relationship mappings") :
    "Should detect bidirectional relationship mappings (BUG-003)"

// =============================================================================
// GENERATED FILES VERIFICATION
// =============================================================================

def generatedSources = new File(basedir, "target/generated-sources/hexaglue")
assert generatedSources.exists() : "Generated sources directory should exist"

def infraPackage = new File(generatedSources, "com/university/infrastructure/persistence")
assert infraPackage.exists() : "Infrastructure persistence package should exist"

// Verify all entities are generated
["StudentEntity.java", "CourseEntity.java", "TagEntity.java", "LessonEntity.java"].each { entity ->
    def entityFile = new File(infraPackage, entity)
    assert entityFile.exists() : "${entity} should be generated"
}

// Verify all mappers are generated
["StudentMapper.java", "CourseMapper.java", "TagMapper.java", "LessonMapper.java"].each { mapper ->
    def mapperFile = new File(infraPackage, mapper)
    assert mapperFile.exists() : "${mapper} should be generated"
}

// Verify all repositories are generated
["StudentJpaRepository.java", "CourseJpaRepository.java", "TagJpaRepository.java", "LessonJpaRepository.java"].each { repo ->
    def repoFile = new File(infraPackage, repo)
    assert repoFile.exists() : "${repo} should be generated"
}

// =============================================================================
// BUG FIX VALIDATIONS
// =============================================================================

// BUG-006 & BUG-007: Verify StudentEntity has auditing and version fields
def studentEntity = new File(infraPackage, "StudentEntity.java").text
assert studentEntity.contains("@CreatedDate") : "BUG-006: StudentEntity should have @CreatedDate"
assert studentEntity.contains("@LastModifiedDate") : "BUG-006: StudentEntity should have @LastModifiedDate"
assert studentEntity.contains("@Version") : "BUG-007: StudentEntity should have @Version"
assert studentEntity.contains("@EntityListeners(AuditingEntityListener.class)") :
    "BUG-006: StudentEntity should have @EntityListeners"

// BUG-004: Verify StudentEntity has @Enumerated for enum collection
assert studentEntity.contains("@ElementCollection") : "BUG-004: StudentEntity should have @ElementCollection for achievements"
assert studentEntity.contains("@Enumerated(EnumType.STRING)") : "BUG-004: StudentEntity should have @Enumerated(STRING)"

// BUG-008: Verify CourseEntity uses TagEntity, not Tag
def courseEntity = new File(infraPackage, "CourseEntity.java").text
assert courseEntity.contains("List<TagEntity>") : "BUG-008: CourseEntity should use List<TagEntity>, not List<Tag>"
assert courseEntity.contains("List<LessonEntity>") : "BUG-008: CourseEntity should use List<LessonEntity>, not List<Lesson>"
assert !courseEntity.contains("import com.university.domain.course.Tag;") :
    "BUG-008: CourseEntity should not import domain Tag"

// BUG-003: Verify CourseEntity has mappedBy for bidirectional relationship
assert courseEntity.contains("mappedBy = \"course\"") : "BUG-003: CourseEntity.lessons should have mappedBy"

// BUG-008: Verify TagEntity uses CourseEntity
def tagEntity = new File(infraPackage, "TagEntity.java").text
assert tagEntity.contains("List<CourseEntity>") : "BUG-008: TagEntity should use List<CourseEntity>, not List<Course>"

// BUG-008: Verify LessonEntity uses CourseEntity for @ManyToOne
def lessonEntity = new File(infraPackage, "LessonEntity.java").text
assert lessonEntity.contains("CourseEntity course") : "BUG-008: LessonEntity should use CourseEntity, not Course"
assert lessonEntity.contains("@ManyToOne") : "LessonEntity should have @ManyToOne for course"
assert lessonEntity.contains("@JoinColumn") : "LessonEntity should have @JoinColumn for course"

// BUG-009: Verify LessonMapper uses CourseMapper
def lessonMapper = new File(infraPackage, "LessonMapper.java").text
assert lessonMapper.contains("uses = {CourseMapper.class}") :
    "BUG-009: LessonMapper should use CourseMapper for entity relationship"

// =============================================================================
// TEST EXECUTION VERIFICATION
// =============================================================================

// Verify tests executed successfully
assert logContent.contains("Tests run:") : "Tests should have been executed"
assert !logContent.contains("Failures: 1") && !logContent.contains("Errors: 1") :
    "All tests should pass"

// Check surefire reports exist
def surefireReports = new File(basedir, "target/surefire-reports")
assert surefireReports.exists() : "Surefire reports directory should exist"

// Verify specific test classes ran
["StudentCrudTest", "CourseCrudTest", "AuditingTest"].each { testClass ->
    def testReport = surefireReports.listFiles().find { it.name.contains(testClass) }
    assert testReport != null : "${testClass} should have been executed"
}

println """
=============================================================================
SUCCESS: sample-jpa-bugfixes integration test passed!

Validated bug fixes:
  - BUG-003: Bidirectional relationship with mappedBy ✓
  - BUG-004: @ElementCollection with @Enumerated(STRING) ✓
  - BUG-006: @CreatedDate/@LastModifiedDate auditing ✓
  - BUG-007: @Version optimistic locking ✓
  - BUG-008: Entity relationships use entity types ✓
  - BUG-009: Mappers use 'uses' clause ✓

Generated artifacts:
  - 4 entities (Student, Course, Tag, Lesson)
  - 4 repositories
  - 4 mappers
  - 2 adapters

Tests executed:
  - StudentCrudTest (enum collections, auditing, versioning)
  - CourseCrudTest (relationships, bidirectional mappedBy)
  - AuditingTest (@CreatedDate/@LastModifiedDate)
=============================================================================
"""

return true

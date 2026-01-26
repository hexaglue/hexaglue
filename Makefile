# HexaGlue Makefile
# Run 'make help' for available targets

.PHONY: help clean compile test format format-check \
        quality checkstyle spotbugs pmd \
        coverage mutation integration \
        build quick verify ci all \
        release-check release install

# Default target
.DEFAULT_GOAL := help

# Colors for output
CYAN := \033[36m
GREEN := \033[32m
YELLOW := \033[33m
RED := \033[31m
RESET := \033[0m

# Directories
BUILD_DIR := build
REPORTS_DIR := target/quality

## help: Show this help message
help:
	@echo ""
	@echo "$(CYAN)HexaGlue Build Targets$(RESET)"
	@echo ""
	@echo "$(YELLOW)Basic targets:$(RESET)"
	@echo "  $(GREEN)clean$(RESET)          Clean all build artifacts"
	@echo "  $(GREEN)compile$(RESET)        Compile without tests"
	@echo "  $(GREEN)test$(RESET)           Run all tests"
	@echo "  $(GREEN)format$(RESET)         Apply code formatting (Palantir)"
	@echo "  $(GREEN)format-check$(RESET)   Check code formatting"
	@echo ""
	@echo "$(YELLOW)Quality targets:$(RESET)"
	@echo "  $(GREEN)quality$(RESET)        Run all quality checks with aggregated reports"
	@echo "  $(GREEN)checkstyle$(RESET)     Run Checkstyle with aggregated report"
	@echo "  $(GREEN)spotbugs$(RESET)       Run SpotBugs (XML reports per module)"
	@echo "  $(GREEN)pmd$(RESET)            Run PMD with aggregated report"
	@echo ""
	@echo "$(YELLOW)Coverage & Testing:$(RESET)"
	@echo "  $(GREEN)coverage$(RESET)       Run tests and generate aggregated coverage report"
	@echo "  $(GREEN)mutation$(RESET)       Run mutation testing (hexaglue-core)"
	@echo "  $(GREEN)integration$(RESET)    Run integration tests on examples"
	@echo ""
	@echo "$(YELLOW)Combined targets:$(RESET)"
	@echo "  $(GREEN)build$(RESET)          Clean build with tests"
	@echo "  $(GREEN)quick$(RESET)          Quick rebuild (clean + compile, no tests)"
	@echo "  $(GREEN)verify$(RESET)         Tests + quality (incremental)"
	@echo "  $(GREEN)ci$(RESET)             Full CI pipeline (clean + test + quality)"
	@echo "  $(GREEN)all$(RESET)            Everything (clean + test + quality + coverage)"
	@echo ""
	@echo "$(YELLOW)Release:$(RESET)"
	@echo "  $(GREEN)release-check$(RESET)  Build release artifacts (dry-run)"
	@echo "  $(GREEN)release$(RESET)        Deploy to Maven Central"
	@echo ""
	@echo "$(YELLOW)Reports location after 'make quality':$(RESET)"
	@echo "  - Checkstyle: $(REPORTS_DIR)/checkstyle-aggregate.html"
	@echo "  - PMD:        $(REPORTS_DIR)/pmd.html"
	@echo "  - Source XRef:$(REPORTS_DIR)/xref/"
	@echo "  - SpotBugs:   <module>/target/spotbugsXml.xml (per module)"
	@echo ""
	@echo "$(YELLOW)Reports location after 'make coverage':$(RESET)"
	@echo "  - Coverage:   target/coverage/index.html"
	@echo ""

# =============================================================================
# Basic Targets
# =============================================================================

## clean: Clean all build artifacts
clean:
	@echo "$(CYAN)Cleaning build artifacts...$(RESET)"
	@mvn clean -q
	@rm -rf $(REPORTS_DIR) $(BUILD_DIR)/build.log

## compile: Compile all modules without tests
compile:
	@echo "$(CYAN)Compiling all modules...$(RESET)"
	@mvn compile -DskipTests -q

## test: Run all tests
test:
	@echo "$(CYAN)Running tests...$(RESET)"
	@mvn test

## format: Apply code formatting (Palantir)
format:
	@echo "$(CYAN)Applying code formatting...$(RESET)"
	@mvn com.diffplug.spotless:spotless-maven-plugin:apply -q

## format-check: Check code formatting
format-check:
	@echo "$(CYAN)Checking code formatting...$(RESET)"
	@mvn com.diffplug.spotless:spotless-maven-plugin:check

# =============================================================================
# Quality Targets
# =============================================================================

## quality: Run all quality checks with aggregated HTML reports
quality:
	@echo "$(CYAN)Running quality checks...$(RESET)"
	@mkdir -p $(BUILD_DIR)
	@rm -rf $(REPORTS_DIR) target/reports
	@# Run all quality checks (compile + check)
	@mvn verify -Pquality -DskipTests 2>&1 | tee $(BUILD_DIR)/build.log
	@# Generate aggregated HTML reports
	@echo "$(CYAN)Generating aggregated reports...$(RESET)"
	@mvn checkstyle:checkstyle-aggregate pmd:aggregate-pmd jxr:aggregate -DskipTests -q
	@# Move reports to quality directory
	@mv target/reports $(REPORTS_DIR)
	@echo ""
	@echo "$(GREEN)Quality reports generated:$(RESET)"
	@echo "  - Checkstyle: $(REPORTS_DIR)/checkstyle-aggregate.html"
	@echo "  - PMD:        $(REPORTS_DIR)/pmd.html"
	@echo "  - Source XRef:$(REPORTS_DIR)/xref/"
	@echo "  - SpotBugs:   <module>/target/spotbugsXml.xml (per module)"
	@echo "  - Build log:  $(BUILD_DIR)/build.log"

## checkstyle: Run Checkstyle with aggregated HTML report
checkstyle:
	@echo "$(CYAN)Running Checkstyle...$(RESET)"
	@rm -rf target/reports/checkstyle*
	@mvn checkstyle:check checkstyle:checkstyle-aggregate -Pquality -q
	@mkdir -p $(REPORTS_DIR)
	@cp target/reports/checkstyle-aggregate.html $(REPORTS_DIR)/ 2>/dev/null || true
	@cp -r target/reports/css target/reports/images $(REPORTS_DIR)/ 2>/dev/null || true
	@echo "$(GREEN)Report: $(REPORTS_DIR)/checkstyle-aggregate.html$(RESET)"

## spotbugs: Run SpotBugs (generates XML reports per module)
spotbugs:
	@echo "$(CYAN)Running SpotBugs...$(RESET)"
	@mvn spotbugs:check -Pquality
	@echo "$(GREEN)Reports: <module>/target/spotbugsXml.xml$(RESET)"
	@echo "$(YELLOW)Note: SpotBugs does not support aggregated HTML reports$(RESET)"

## pmd: Run PMD with aggregated HTML report
pmd:
	@echo "$(CYAN)Running PMD...$(RESET)"
	@rm -rf target/reports/pmd*
	@mvn pmd:check pmd:aggregate-pmd -Pquality -q
	@mkdir -p $(REPORTS_DIR)
	@cp target/reports/pmd.html $(REPORTS_DIR)/ 2>/dev/null || true
	@cp -r target/reports/css target/reports/images $(REPORTS_DIR)/ 2>/dev/null || true
	@echo "$(GREEN)Report: $(REPORTS_DIR)/pmd.html$(RESET)"

# =============================================================================
# Coverage & Testing
# =============================================================================

## coverage: Run tests and generate aggregated coverage report
coverage:
	@echo "$(CYAN)Running tests with coverage...$(RESET)"
	@mvn verify -pl !build/distribution
	@echo "$(GREEN)Coverage report: target/coverage/index.html$(RESET)"

## mutation: Run mutation testing with PITest on hexaglue-core
mutation:
	@echo "$(CYAN)Running mutation tests on hexaglue-core...$(RESET)"
	@mvn test -pl hexaglue-core -q
	@mvn org.pitest:pitest-maven:mutationCoverage -pl hexaglue-core
	@echo "$(GREEN)Mutation report: hexaglue-core/target/pit-reports/index.html$(RESET)"

## integration: Run integration tests on examples
integration:
	@echo "$(CYAN)Running integration tests...$(RESET)"
	@mvn verify -pl build/integration-tests

# =============================================================================
# Combined Targets
# =============================================================================

## build: Clean build with tests
build:
	@echo "$(CYAN)Clean build with tests...$(RESET)"
	@mvn clean test

## quick: Quick rebuild without tests (clean + compile + install)
quick:
	@echo "$(CYAN)Quick rebuild (no tests)...$(RESET)"
	@mvn clean install -DskipTests

## verify: Tests + quality checks (incremental, no clean)
verify:
	@echo "$(CYAN)Running tests and quality checks...$(RESET)"
	@mkdir -p $(BUILD_DIR)
	@rm -rf $(REPORTS_DIR) target/reports
	@mvn verify -Pquality 2>&1 | tee $(BUILD_DIR)/build.log
	@mvn checkstyle:checkstyle-aggregate pmd:aggregate-pmd jxr:aggregate -DskipTests -q
	@mv target/reports $(REPORTS_DIR)
	@echo "$(GREEN)Done. Reports in $(REPORTS_DIR)/$(RESET)"

## ci: Full CI pipeline (clean + test + quality)
ci:
	@echo "$(CYAN)Running full CI pipeline...$(RESET)"
	@mkdir -p $(BUILD_DIR)
	@rm -rf $(REPORTS_DIR) target/reports
	@mvn clean verify -Pquality 2>&1 | tee $(BUILD_DIR)/build.log
	@mvn checkstyle:checkstyle-aggregate pmd:aggregate-pmd jxr:aggregate -DskipTests -q
	@mv target/reports $(REPORTS_DIR)
	@echo "$(GREEN)CI build complete.$(RESET)"
	@echo "  - Quality: $(REPORTS_DIR)/"
	@echo "  - Coverage: target/coverage/index.html"
	@echo "  - Build log: $(BUILD_DIR)/build.log"

## all: Full build (clean + test + quality + coverage)
all:
	@echo "$(CYAN)Running full build...$(RESET)"
	@mkdir -p $(BUILD_DIR)
	@rm -rf $(REPORTS_DIR) target/reports
	@mvn clean verify -Pquality 2>&1 | tee $(BUILD_DIR)/build.log
	@mvn checkstyle:checkstyle-aggregate pmd:aggregate-pmd jxr:aggregate -DskipTests -q
	@mv target/reports $(REPORTS_DIR)
	@echo "$(GREEN)Full build complete!$(RESET)"
	@echo "  - Quality:  $(REPORTS_DIR)/"
	@echo "  - Coverage: target/coverage/index.html"
	@echo "  - Build log: $(BUILD_DIR)/build.log"

# =============================================================================
# Release
# =============================================================================

## release-check: Build release artifacts without deploying
release-check:
	@echo "$(CYAN)Building release artifacts...$(RESET)"
	@mvn clean verify -Prelease -DskipTests

## release: Deploy to Maven Central
release:
	@echo "$(YELLOW)Deploying to Maven Central...$(RESET)"
	@mvn clean deploy -Prelease -DskipTests

# =============================================================================
# Aliases (backwards compatibility)
# =============================================================================

## install: Alias for 'quick' (backwards compatibility)
install: quick

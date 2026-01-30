# HexaGlue Makefile
# Run 'make help' for available targets

.PHONY: help clean compile test format format-check \
        quality checkstyle spotbugs pmd \
        coverage mutation integration \
        build quick verify ci all \
        release-check release install

.DELETE_ON_ERROR:

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

# Shared command fragments (DRY)
QUALITY_PREPARE = mkdir -p $(BUILD_DIR) && rm -rf $(REPORTS_DIR) target/reports
QUALITY_AGGREGATE = echo "$(CYAN)Generating aggregated reports...$(RESET)" \
    && mvn checkstyle:checkstyle-aggregate pmd:aggregate-pmd jxr:aggregate -DskipTests -q \
    && mv target/reports $(REPORTS_DIR)

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
	@echo "  $(GREEN)quality$(RESET)        Run quality checks with aggregated reports (no tests)"
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
	@echo "  $(GREEN)build$(RESET)          clean + test"
	@echo "  $(GREEN)quick$(RESET)          Clean install without tests"
	@echo "  $(GREEN)install$(RESET)        Clean build with tests + install to local repo"
	@echo "  $(GREEN)verify$(RESET)         test + quality (incremental, no clean)"
	@echo "  $(GREEN)ci$(RESET)             clean + verify"
	@echo "  $(GREEN)all$(RESET)            ci + coverage"
	@echo ""
	@echo "$(YELLOW)Release:$(RESET)"
	@echo "  $(GREEN)release-check$(RESET)  Build release artifacts (dry-run)"
	@echo "  $(GREEN)release$(RESET)        Deploy to Maven Central"
	@echo ""
	@echo "$(YELLOW)Reports location:$(RESET)"
	@echo "  - Checkstyle: $(REPORTS_DIR)/checkstyle-aggregate.html"
	@echo "  - PMD:        $(REPORTS_DIR)/pmd.html"
	@echo "  - Source XRef: $(REPORTS_DIR)/xref/"
	@echo "  - SpotBugs:   <module>/target/spotbugsXml.xml (per module)"
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

## quality: Run quality checks with aggregated HTML reports (no tests)
quality:
	@echo "$(CYAN)Running quality checks...$(RESET)"
	@$(QUALITY_PREPARE)
	@mvn verify -Pquality -DskipTests 2>&1 | tee $(BUILD_DIR)/build.log
	@$(QUALITY_AGGREGATE)
	@echo ""
	@echo "$(GREEN)Quality reports generated:$(RESET)"
	@echo "  - Checkstyle: $(REPORTS_DIR)/checkstyle-aggregate.html"
	@echo "  - PMD:        $(REPORTS_DIR)/pmd.html"
	@echo "  - Source XRef: $(REPORTS_DIR)/xref/"
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

## build: Clean build with tests (clean + test)
build: clean test
	@echo "$(GREEN)Build complete.$(RESET)"

## quick: Clean install without tests
quick:
	@echo "$(CYAN)Quick rebuild (no tests)...$(RESET)"
	@mvn clean install -DskipTests -q

## verify: Tests + quality checks with reports (incremental, no clean)
verify:
	@echo "$(CYAN)Running tests and quality checks...$(RESET)"
	@$(QUALITY_PREPARE)
	@mvn verify -Pquality 2>&1 | tee $(BUILD_DIR)/build.log
	@$(QUALITY_AGGREGATE)
	@echo "$(GREEN)Done. Reports in $(REPORTS_DIR)/$(RESET)"

## ci: Full CI pipeline (clean + verify)
ci: clean verify
	@echo "$(GREEN)CI build complete. Reports in $(REPORTS_DIR)/$(RESET)"

## all: Full build (ci + coverage)
all: ci coverage
	@echo "$(GREEN)Full build complete.$(RESET)"

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

## install: Clean build with tests and install to local Maven repo
install:
	@echo "$(CYAN)Building and installing to local repo...$(RESET)"
	@mvn clean install

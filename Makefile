# HexaGlue Makefile
# Run 'make help' for available targets

.PHONY: help install test format format-check quality coverage integration \
        release-check release ci clean build all

# Default target
.DEFAULT_GOAL := help

# Colors for output
CYAN := \033[36m
GREEN := \033[32m
YELLOW := \033[33m
RESET := \033[0m

## help: Show this help message
help:
	@echo ""
	@echo "$(CYAN)HexaGlue Build Targets$(RESET)"
	@echo ""
	@grep -E '^## ' $(MAKEFILE_LIST) | sed -e 's/## //' | awk -F': ' '{printf "  $(GREEN)%-15s$(RESET) %s\n", $$1, $$2}'
	@echo ""

# =============================================================================
# Development
# =============================================================================

## install: Install all modules locally (skip tests)
install:
	@echo "$(CYAN)Installing all modules...$(RESET)"
	mvn install -DskipTests

## build: Build all modules (skip tests)
build: install

## clean: Clean all build artifacts
clean:
	@echo "$(CYAN)Cleaning build artifacts...$(RESET)"
	mvn clean
	@rm -f build/build.log

## test: Run all tests
test:
	@echo "$(CYAN)Running tests...$(RESET)"
	mvn test

# =============================================================================
# Code Quality
# =============================================================================

## format: Apply code formatting (Palantir)
format:
	@echo "$(CYAN)Applying code formatting...$(RESET)"
	mvn com.diffplug.spotless:spotless-maven-plugin:apply

## format-check: Check code formatting
format-check:
	@echo "$(CYAN)Checking code formatting...$(RESET)"
	mvn com.diffplug.spotless:spotless-maven-plugin:check

## quality: Run all quality checks (Checkstyle, SpotBugs, PMD)
quality: install
	@echo "$(CYAN)Running quality checks...$(RESET)"
	mvn verify -Pquality -DskipTests 2>&1 | tee build/build.log
	@echo "$(GREEN)Quality report saved to build/build.log$(RESET)"

## checkstyle: Run Checkstyle only
checkstyle: install
	@echo "$(CYAN)Running Checkstyle...$(RESET)"
	mvn checkstyle:check -Pquality

## spotbugs: Run SpotBugs only
spotbugs: install
	@echo "$(CYAN)Running SpotBugs...$(RESET)"
	mvn spotbugs:check -Pquality

## pmd: Run PMD only
pmd: install
	@echo "$(CYAN)Running PMD...$(RESET)"
	mvn pmd:check -Pquality

# =============================================================================
# Testing & Coverage
# =============================================================================

## coverage: Generate aggregated coverage report
coverage:
	@echo "$(CYAN)Generating coverage report...$(RESET)"
	mvn verify -pl build/coverage
	@echo "$(GREEN)Coverage report: build/target/jacoco-aggregate/index.html$(RESET)"

## integration: Run integration tests on examples
integration:
	@echo "$(CYAN)Running integration tests...$(RESET)"
	mvn verify -pl build/integration-tests

# =============================================================================
# Release & Distribution
# =============================================================================

## release-check: Build release artifacts without deploying
release-check: install
	@echo "$(CYAN)Building release artifacts...$(RESET)"
	mvn verify -Prelease -DskipTests

## release: Deploy to Maven Central
release: install
	@echo "$(YELLOW)Deploying to Maven Central...$(RESET)"
	mvn deploy -Prelease -DskipTests

# =============================================================================
# CI Pipeline
# =============================================================================

## ci: Full CI build (clean, test, quality)
ci:
	@echo "$(CYAN)Running full CI pipeline...$(RESET)"
	mvn clean verify -Pquality 2>&1 | tee build/build.log
	@echo "$(GREEN)CI build complete. Log saved to build/build.log$(RESET)"

## all: Full build with tests, quality, and coverage
all: clean test quality coverage
	@echo "$(GREEN)Full build complete!$(RESET)"

# =============================================================================
# Shortcuts
# =============================================================================

## verify: Alias for 'make test quality'
verify: test quality

## quick: Quick build (clean install, skip tests)
quick: clean install
	@echo "$(GREEN)Quick build complete!$(RESET)"

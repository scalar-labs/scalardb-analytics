# Makefile for building and publishing ScalarDB Analytics Server Docker image

# Configuration
DOCKER := docker
DOCKERFILE := Dockerfile
BUILD_CONTEXT := ../..
# Platforms for multi-platform builds
PLATFORMS ?= linux/amd64,linux/arm64

# GitHub Packages credentials
# First try environment variables, then fall back to ~/.gradle/gradle.properties
GRADLE_PROPS := $(HOME)/.gradle/gradle.properties
GPR_USERNAME ?= $(shell grep '^gprUsername=' $(GRADLE_PROPS) 2>/dev/null | cut -d'=' -f2-)
GPR_PASSWORD ?= $(shell grep '^gprPassword=' $(GRADLE_PROPS) 2>/dev/null | cut -d'=' -f2-)

# Get version from git describe, stripping any leading "v" so Docker image tags stay
# non-v-prefixed (e.g. "3.16.0") even though git tags are v-prefixed (e.g. "v3.16.0").
# Produces "3.16.0" for exact tags or "3.16.0-2-g975e5da" for commits after tags.
VERSION := $(patsubst v%,%,$(shell git describe --tags --always))

IMAGE_PREFIX := ghcr.io/scalar-labs/scalardb-analytics-server

IMAGE_LATEST := $(IMAGE_PREFIX):latest
IMAGE_VERSIONED := $(IMAGE_PREFIX):$(VERSION)

# Default target
.PHONY: all
all: build

# Build the Docker image with 'latest' tag
.PHONY: build
build:
	@echo "Building Docker image: $(IMAGE_LATEST)"
	@echo "Build context: $(BUILD_CONTEXT)"
	GPR_USERNAME="$(GPR_USERNAME)" GPR_PASSWORD="$(GPR_PASSWORD)" \
	$(DOCKER) build -t $(IMAGE_LATEST) -f $(DOCKERFILE) \
		--secret id=gpr_username,env=GPR_USERNAME \
		--secret id=gpr_password,env=GPR_PASSWORD \
		$(BUILD_CONTEXT)

# Tag the latest image with version
.PHONY: tag
tag:
	@echo "Tagging latest image with version: $(IMAGE_VERSIONED)"
	$(DOCKER) tag $(IMAGE_LATEST) $(IMAGE_VERSIONED)

# Push to GitHub Packages or AWS ECR
.PHONY: push
push:
	@echo "Pushing images to registry..."
	@echo "Pushing: $(IMAGE_LATEST)"
	$(DOCKER) push $(IMAGE_LATEST)
	@if [ -n "$$(docker images -q $(IMAGE_VERSIONED) 2>/dev/null)" ]; then \
		echo "Pushing: $(IMAGE_VERSIONED)"; \
		$(DOCKER) push $(IMAGE_VERSIONED); \
	else \
		echo "Warning: Versioned image $(IMAGE_VERSIONED) not found. Run 'make tag' first."; \
	fi

# Build, tag and push in one command
.PHONY: release
release: build tag push

# Build and push multi-platform images (amd64 and arm64)
.PHONY: build-and-push-multiplatform
build-and-push-multiplatform:
	@echo "Building multi-platform Docker image for: $(PLATFORMS)"
	@echo "Build context: $(BUILD_CONTEXT)"
	GPR_USERNAME="$(GPR_USERNAME)" GPR_PASSWORD="$(GPR_PASSWORD)" \
	$(DOCKER) buildx build \
		--platform $(PLATFORMS) \
		-t $(IMAGE_LATEST) \
		-t $(IMAGE_VERSIONED) \
		-f $(DOCKERFILE) \
		--secret id=gpr_username,env=GPR_USERNAME \
		--secret id=gpr_password,env=GPR_PASSWORD \
		--push \
		$(BUILD_CONTEXT)

# Show current version
.PHONY: version
version:
	@echo "Git version: $(VERSION)"
	@echo "Latest image: $(IMAGE_LATEST)"
	@echo "Versioned image: $(IMAGE_VERSIONED)"

# Clean up local images
.PHONY: clean
clean:
	@echo "Removing local images..."
	-$(DOCKER) rmi $(IMAGE_LATEST) $(IMAGE_VERSIONED)

# Help target
.PHONY: help
help:
	@echo "Available targets:"
	@echo "  build                        - Build the Docker image with 'latest' tag"
	@echo "  tag                          - Tag the latest image with git version"
	@echo "  push                         - Push images to GitHub Packages"
	@echo "  release                      - Build, tag, and push (complete workflow)"
	@echo "  build-and-push-multiplatform - Build and push multi-platform images (amd64 + arm64)"
	@echo "  version                      - Show current version information"
	@echo "  clean                        - Remove local images"
	@echo "  help                         - Show this help message"
	@echo ""
	@echo "Multi-platform builds:"
	@echo "  make build-and-push-multiplatform                    # Builds and pushes for amd64 and arm64"
	@echo "  make build-and-push-multiplatform PLATFORMS=linux/amd64,linux/arm64,linux/arm/v7  # Custom platforms"
	@echo ""
	@echo "Current configuration:"
	@echo "  PLATFORMS: $(PLATFORMS)"
	@echo "  VERSION: $(VERSION)"
	@echo "  LATEST: $(IMAGE_LATEST)"
	@echo "  VERSIONED: $(IMAGE_VERSIONED)"

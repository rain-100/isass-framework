# Spring Boot Adapter Phase 1 Design

## Goal

Implement the first roadmap step for Spring decoupling by making `isass-adapter-springboot` the Spring Boot integration entry point for framework core common components.

## Scope

This phase moves only the Spring Boot auto-configuration entry point out of `isass-core-common`. It does not yet remove every Spring type from `isass-core-common`; those dependencies will be peeled away in later roadmap steps.

The CTE recursive-query roadmap item is explicitly marked as postponed and will not be implemented in this phase.

## Architecture

`isass-core-common` remains the owner of common entities, exceptions, utilities, converters, and managers. `isass-adapter-springboot` imports `isass-core-common` and contributes a Spring Boot auto-configuration class that component-scans the existing common package.

The adapter is not a container for every Spring-related implementation in the framework. Feature module auto-configurations stay in their own modules so services only receive the capabilities they depend on. For example, a service that only needs Spring Event MQ depends on `isass-adapter-springboot` and `isass-mq-springevent`, not net or database modules.

Business services that run on Spring Boot should depend on `isass-adapter-springboot`. Non-Spring consumers can continue moving toward direct `isass-core-common` usage as later phases remove the remaining Spring dependencies from common code.

Security-specific Spring components belong to `isass-security-springsecurity`. Core-common may retain pure Java interfaces, constants, and aggregators, while Spring annotation wiring lives in the security module.

## Validation

Framework tests should verify that `isass-adapter-springboot` publishes a Boot auto-configuration import and registers a known common component. Attachment service is the first adapted business service and should continue to compile with the new dependency.

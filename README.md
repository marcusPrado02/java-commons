# Commons Platform

Supreme shared commons platform for high-quality Java microservices.

## Goals
- Clean Architecture / Hexagonal
- DDD-first
- Framework-agnostic core
- Optional adapters and Spring Boot starters
- Strong governance (architecture rules, quality gates)

## GroupId
com.marcusprado02.commons

## Java
- Java 21+

## Structure
- `commons-kernel-*` → domain-safe, framework-free
- `commons-app-*` → application layer helpers
- `commons-ports-*` → hexagonal ports
- `commons-adapters-*` → infrastructure implementations
- `commons-spring-starter-*` → productivity starters

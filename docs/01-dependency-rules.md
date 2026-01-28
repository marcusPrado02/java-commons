# Dependency Rules

- kernel-* MUST NOT depend on app-*, ports-*, adapters-*, spring-*
- app-* MAY depend on kernel-*
- ports-* MAY depend on kernel-*
- adapters-* MAY depend on ports-* + app-* + kernel-*
- spring-starter-* MAY depend on any module (glue only)

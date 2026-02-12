/**
 * Framework-agnostic REST query layer for dynamic filtering, pagination, and sorting.
 *
 * <p>This package provides utilities and controllers for building REST APIs with advanced querying
 * capabilities while maintaining Clean Architecture principles.
 *
 * <h2>Core Components:</h2>
 *
 * <ul>
 *   <li>{@link com.marcusprado02.commons.adapters.web.rest.PageableResponse} - Immutable pageable
 *       response DTO
 *   <li>{@link com.marcusprado02.commons.adapters.web.rest.QueryParamsParser} - Query parameter
 *       parsing utilities
 *   <li>{@link com.marcusprado02.commons.adapters.web.rest.GenericSearchController} - Generic
 *       search orchestration
 * </ul>
 *
 * <h2>Features:</h2>
 *
 * <ul>
 *   <li>Framework-agnostic (no Spring, JAX-RS, or other dependencies)
 *   <li>Dynamic filtering with multiple operators (eq, neq, like, gt, lt, gte, lte, in)
 *   <li>Multi-field sorting (asc, desc)
 *   <li>Pagination with size limits
 *   <li>Null-safe, defensive parsing
 *   <li>Immutable objects
 * </ul>
 *
 * <h2>Usage Example:</h2>
 *
 * <pre>{@code
 * // Create controller
 * var controller = new GenericSearchController<>("/api/users", userRepository);
 *
 * // Execute search
 * Map<String, String> params = Map.of(
 *     "page", "0",
 *     "size", "20",
 *     "filter", "name:like:John,age:gte:18",
 *     "sort", "name:asc"
 * );
 * PageableResponse<User> response = controller.search(params);
 * }</pre>
 *
 * <h2>Query Parameter Format:</h2>
 *
 * <ul>
 *   <li>Filter: {@code ?filter=field:operator:value,field2:operator2:value2}
 *   <li>Sort: {@code ?sort=field:direction,field2:direction2}
 *   <li>Pagination: {@code ?page=0&size=20}
 * </ul>
 *
 * @see com.marcusprado02.commons.ports.persistence.contract.PageableRepository
 * @see com.marcusprado02.commons.ports.persistence.specification.SearchCriteria
 * @since 0.1.0
 */
package com.marcusprado02.commons.adapters.web.rest;

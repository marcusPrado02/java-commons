package com.marcusprado02.commons.kernel.errors;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProblemExceptionsTest {

    @Test
    void should_map_validation_to_ValidationException() {
        Problem p = Problem.of(
                ErrorCode.of("X.VALIDATION"),
                ErrorCategory.VALIDATION,
                Severity.ERROR,
                "invalid"
        );

        DomainException ex = ProblemExceptions.from(p);
        assertInstanceOf(ValidationException.class, ex);
        assertEquals(p, ex.problem());
    }

    @Test
    void should_map_not_found_to_NotFoundException() {
        Problem p = Problem.of(
                ErrorCode.of("X.NOT_FOUND"),
                ErrorCategory.NOT_FOUND,
                Severity.ERROR,
                "missing"
        );

        DomainException ex = ProblemExceptions.from(p);
        assertInstanceOf(NotFoundException.class, ex);
        assertEquals(p, ex.problem());
    }
}

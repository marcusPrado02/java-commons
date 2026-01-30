package com.marcusprado02.commons.kernel.result;

import com.marcusprado02.commons.kernel.errors.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResultsTest {

    @Test
    void orThrow_should_return_value_when_ok() {
        Result<String> r = Result.ok("ok");
        assertEquals("ok", Results.orThrow(r));
    }

    @Test
    void orThrow_should_throw_specific_exception_when_fail() {
        Problem p = Problem.of(
                ErrorCode.of("USER.NOT_FOUND"),
                ErrorCategory.NOT_FOUND,
                Severity.ERROR,
                "User not found"
        );

        Result<String> r = Result.fail(p);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> Results.orThrow(r));
        assertInstanceOf(NotFoundException.class, ex);
        assertEquals(p, ((DomainException) ex).problem());
    }

    @Test
    void catchingDomain_should_convert_exception_to_fail() {
        Problem p = Problem.of(
                ErrorCode.of("X.BUSINESS"),
                ErrorCategory.BUSINESS,
                Severity.ERROR,
                "boom"
        );

        Result<String> r = Results.catchingDomain(() -> {
            throw new BusinessException(p);
        });

        assertTrue(r.isFail());
        assertEquals(p, r.problemOrNull());
    }
}

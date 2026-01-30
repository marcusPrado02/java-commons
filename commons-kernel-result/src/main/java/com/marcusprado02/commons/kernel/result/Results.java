package com.marcusprado02.commons.kernel.result;

import com.marcusprado02.commons.kernel.errors.DomainException;
import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.errors.ProblemExceptions;


import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public final class Results {

    private Results() {}

    /** Convert a DomainException to a Result.fail with the associated Problem. */
    public static <T> Result<T> fromException(DomainException ex) {
        Objects.requireNonNull(ex, "ex");
        Problem p = ex.problem();
        if (p == null) {
            // fallback defensivo; em produção geralmente não deveria acontecer
            throw new IllegalStateException("DomainException without Problem", ex);
        }
        return Result.fail(p);
    }

    /**
     * Unwrap: returns value if Ok, otherwise throws DomainException (with the Problem).
     * Useful in internal layers of the domain/application.
     */
    public static <T> T orThrow(Result<T> result) {
        Objects.requireNonNull(result, "result");
        if (result.isOk()) return result.getOrNull();
        throw ProblemExceptions.from(result.problemOrNull());
    }

    /**
     * Unwrap custom: allows mapping the Problem to a specific exception.
     * Ex: (p) -> new ValidationException(p)
     */
    public static <T, X extends RuntimeException> T orThrow(
            Result<T> result,
            Function<Problem, X> exceptionFactory
    ) {
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(exceptionFactory, "exceptionFactory");
        if (result.isOk()) return result.getOrNull();
        throw exceptionFactory.apply(result.problemOrNull());
    }

    /**
     * Executes supplier and catches DomainException to turn into Result.fail.
     * This reduces try/catch scattered across application services.
     */
    public static <T> Result<T> catchingDomain(Supplier<Result<T>> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        try {
            return Objects.requireNonNull(supplier.get());
        } catch (DomainException ex) {
            return fromException(ex);
        }
    }

    /** Variant for supplier that returns T (turns into Result.ok or Result.fail). */
    public static <T> Result<T> catchingDomainValue(Supplier<T> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        try {
            return Result.ok(supplier.get());
        } catch (DomainException ex) {
            return fromException(ex);
        }
    }
}

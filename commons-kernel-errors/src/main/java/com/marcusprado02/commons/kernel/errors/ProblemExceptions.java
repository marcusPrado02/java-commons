package com.marcusprado02.commons.kernel.errors;

import java.util.Objects;

public final class ProblemExceptions {

    private ProblemExceptions() {}

    /** Create a DomainException based on the category of the Problem. */
    public static DomainException from(Problem problem) {
        Objects.requireNonNull(problem, "problem");
        return switch (problem.category()) {
            case VALIDATION -> new ValidationException(problem);
            case BUSINESS -> new BusinessException(problem);
            case NOT_FOUND -> new NotFoundException(problem);
            case CONFLICT -> new ConflictException(problem);
            case UNAUTHORIZED -> new UnauthorizedException(problem);
            case FORBIDDEN -> new ForbiddenException(problem);
            case TECHNICAL -> new TechnicalException(problem);
        };
    }
}

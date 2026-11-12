package com.han.commerce.common.presentation;

import com.han.commerce.common.domain.DomainException;
import com.han.commerce.common.domain.ErrorType;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DomainException.class)
    public ProblemDetail handleDomainException(DomainException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(toStatus(e.getErrorCode().getType()), e.getMessage());
        problem.setTitle(e.getErrorCode().name());
        return problem;
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(OptimisticLockingFailureException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "다른 요청이 같은 데이터를 먼저 변경했습니다. 다시 시도해 주세요.");
        problem.setTitle("CONCURRENT_MODIFICATION");
        return problem;
    }

    private HttpStatus toStatus(ErrorType type) {
        return switch (type) {
            case INVALID -> HttpStatus.BAD_REQUEST;
            case UNAUTHENTICATED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
        };
    }

}

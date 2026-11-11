package com.han.commerce.support;

import com.han.commerce.common.domain.DomainException;
import com.han.commerce.common.domain.ErrorCode;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public final class DomainAssertions {

    private DomainAssertions() {
    }

    public static void assertErrorCode(ThrowingCallable code, ErrorCode expected) {
        assertThatThrownBy(code)
                .isInstanceOfSatisfying(DomainException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(expected));
    }

}
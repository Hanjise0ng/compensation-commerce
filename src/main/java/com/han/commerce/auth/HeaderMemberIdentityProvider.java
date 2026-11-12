package com.han.commerce.auth;

import com.han.commerce.common.domain.DomainException;
import com.han.commerce.common.domain.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!jwt")
public class HeaderMemberIdentityProvider implements MemberIdentityProvider {

    private static final String HEADER = "X-Member-Id";

    @Override
    public AuthenticatedMember resolve(HttpServletRequest request) {
        String value = request.getHeader(HEADER);
        if (value == null || value.isBlank()) {
            throw new DomainException(ErrorCode.UNAUTHENTICATED);
        }
        try {
            return new AuthenticatedMember(Long.valueOf(value.trim()));
        } catch (NumberFormatException e) {
            throw new DomainException(ErrorCode.UNAUTHENTICATED, "회원 식별자 형식 오류");
        }
    }

}
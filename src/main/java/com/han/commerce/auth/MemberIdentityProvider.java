package com.han.commerce.auth;

import jakarta.servlet.http.HttpServletRequest;

public interface MemberIdentityProvider {

    AuthenticatedMember resolve(HttpServletRequest request);

}
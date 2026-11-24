package com.han.commerce.auth.application.port;

import com.han.commerce.auth.AuthenticatedMember;
import jakarta.servlet.http.HttpServletRequest;

public interface MemberIdentityProvider {

    AuthenticatedMember resolve(HttpServletRequest request);

}
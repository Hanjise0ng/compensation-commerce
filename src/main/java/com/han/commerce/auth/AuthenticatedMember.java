package com.han.commerce.auth;

public class AuthenticatedMember {

    private final Long memberId;

    public AuthenticatedMember(Long memberId) {
        this.memberId = memberId;
    }

    public Long getMemberId() {
        return memberId;
    }

}
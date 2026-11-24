/**
 * Spring Security 설정이 놓일 자리다. 아직 구현하지 않았다.
 *
 * <p>지금은 {@code header} 패키지의 헤더 방식이 인증을 맡는다. Spring Security를 붙이면 여기에
 * 필터 체인 설정, {@code SecurityContext}에서 회원을 꺼내는 {@code MemberIdentityProvider} 구현,
 * 인증 실패(401)와 권한 없음(403)을 기존 오류 형식으로 응답하는 처리기가 들어온다.
 * 컨트롤러와 주문·결제 서비스는 바뀌지 않는다.
 */
package com.han.commerce.auth.infrastructure.security;
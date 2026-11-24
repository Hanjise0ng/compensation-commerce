/**
 * JWT 토큰의 검증과 발급 설정이 놓일 자리다. 아직 구현하지 않았다.
 *
 * <p>직접 만든 필터 대신 Spring Security의 OAuth2 Resource Server 지원으로 토큰을 검증하는 것을
 * 전제로 한다. 여기에는 디코더·인코더 설정과 비밀 키 같은 설정값이 들어온다.
 * 비밀 값은 환경 변수로 주입하고 저장소에 올리지 않는다.
 */
package com.han.commerce.auth.infrastructure.jwt;
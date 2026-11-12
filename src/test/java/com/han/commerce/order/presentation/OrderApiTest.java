package com.han.commerce.order.presentation;

import com.han.commerce.product.domain.Product;
import com.han.commerce.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OrderApiTest extends IntegrationTestSupport {

    @Value("${local.server.port}")
    private int port;

    private RestClient client() {
        return RestClient.create("http://localhost:" + port);
    }

    private Map<String, Object> orderBody(Long productId, int quantity) {
        return Map.of(
                "items", List.of(Map.of("productId", productId, "quantity", quantity)),
                "pointAmount", 0);
    }

    @Test
    @DisplayName("인증 헤더가 없으면 401을 응답한다")
    void unauthenticated() {
        Product product = fixtures.product("키보드", 10_000, 10);

        HttpStatusCode status = client().post().uri("/api/orders")
                .header("Idempotency-Key", "key-1")
                .contentType(MediaType.APPLICATION_JSON)
                .body(orderBody(product.getId(), 1))
                .exchange((request, response) -> response.getStatusCode());

        assertThat(status).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("주문을 생성하고 주문번호로 조회한다")
    void placeAndGet() {
        Long memberId = fixtures.member();
        Product product = fixtures.product("키보드", 10_000, 10);

        ResponseEntity<Map> created = client().post().uri("/api/orders")
                .header("X-Member-Id", String.valueOf(memberId))
                .header("Idempotency-Key", "key-1")
                .contentType(MediaType.APPLICATION_JSON)
                .body(orderBody(product.getId(), 2))
                .retrieve()
                .toEntity(Map.class);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String orderNo = (String) created.getBody().get("orderNo");
        assertThat(((Number) created.getBody().get("payAmount")).longValue()).isEqualTo(20_000L);

        Map detail = client().get().uri("/api/orders/{orderNo}", orderNo)
                .header("X-Member-Id", String.valueOf(memberId))
                .retrieve()
                .body(Map.class);

        assertThat(detail.get("status")).isEqualTo("PENDING");
        assertThat((List<?>) detail.get("items")).hasSize(1);
    }

    @Test
    @DisplayName("다른 회원의 주문은 조회할 수 없다")
    void forbidden() {
        Long owner = fixtures.member();
        Long other = fixtures.member();
        Product product = fixtures.product("키보드", 10_000, 10);

        Map created = client().post().uri("/api/orders")
                .header("X-Member-Id", String.valueOf(owner))
                .header("Idempotency-Key", "key-1")
                .contentType(MediaType.APPLICATION_JSON)
                .body(orderBody(product.getId(), 1))
                .retrieve()
                .body(Map.class);

        HttpStatusCode status = client().get().uri("/api/orders/{orderNo}", created.get("orderNo"))
                .header("X-Member-Id", String.valueOf(other))
                .exchange((request, response) -> response.getStatusCode());

        assertThat(status).isEqualTo(HttpStatus.FORBIDDEN);
    }

}
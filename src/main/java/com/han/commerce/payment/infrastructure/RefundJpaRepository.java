package com.han.commerce.payment.infrastructure;

import com.han.commerce.payment.domain.Refund;
import com.han.commerce.payment.domain.RefundRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundJpaRepository extends RefundRepository, JpaRepository<Refund, Long> {
}
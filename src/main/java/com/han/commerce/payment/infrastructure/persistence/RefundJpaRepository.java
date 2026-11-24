package com.han.commerce.payment.infrastructure.persistence;

import com.han.commerce.payment.domain.refund.Refund;
import com.han.commerce.payment.domain.refund.RefundRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundJpaRepository extends RefundRepository, JpaRepository<Refund, Long> {
}
package com.han.commerce.payment.infrastructure;

import com.han.commerce.payment.domain.Payment;
import com.han.commerce.payment.domain.PaymentRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentJpaRepository extends PaymentRepository, JpaRepository<Payment, Long> {

}
package com.han.commerce.point.infrastructure;

import com.han.commerce.point.domain.PointWallet;
import com.han.commerce.point.domain.PointWalletRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointWalletJpaRepository extends PointWalletRepository, JpaRepository<PointWallet, Long> {

}
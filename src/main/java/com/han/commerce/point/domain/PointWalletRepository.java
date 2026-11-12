package com.han.commerce.point.domain;

import java.util.Optional;

public interface PointWalletRepository {

    Optional<PointWallet> findByMemberId(Long memberId);

}
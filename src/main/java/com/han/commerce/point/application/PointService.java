package com.han.commerce.point.application;

import com.han.commerce.common.domain.error.DomainException;
import com.han.commerce.common.domain.error.ErrorCode;
import com.han.commerce.common.domain.Money;
import com.han.commerce.point.domain.PointHistoryRepository;
import com.han.commerce.point.domain.PointWallet;
import com.han.commerce.point.domain.PointWalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(propagation = Propagation.MANDATORY)
public class PointService {

    private final PointWalletRepository pointWalletRepository;
    private final PointHistoryRepository pointHistoryRepository;

    public PointService(PointWalletRepository pointWalletRepository,
                        PointHistoryRepository pointHistoryRepository) {
        this.pointWalletRepository = pointWalletRepository;
        this.pointHistoryRepository = pointHistoryRepository;
    }

    public void use(Long memberId, Money amount, Long orderId) {
        pointHistoryRepository.save(findWallet(memberId).use(amount, orderId));
    }

    public void restore(Long memberId, Money amount, Long orderId) {
        pointHistoryRepository.save(findWallet(memberId).restore(amount, orderId));
    }

    private PointWallet findWallet(Long memberId) {
        return pointWalletRepository.findByMemberId(memberId)
                .orElseThrow(() -> new DomainException(ErrorCode.POINT_WALLET_NOT_FOUND));
    }

}
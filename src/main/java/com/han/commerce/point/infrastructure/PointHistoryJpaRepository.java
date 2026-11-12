package com.han.commerce.point.infrastructure;

import com.han.commerce.point.domain.PointHistory;
import com.han.commerce.point.domain.PointHistoryRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointHistoryJpaRepository extends PointHistoryRepository, JpaRepository<PointHistory, Long> {

}
package com.han.commerce.order.infrastructure;

import com.han.commerce.common.application.ClockHolder;
import com.han.commerce.order.application.OrderNoGenerator;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
public class UuidOrderNoGenerator implements OrderNoGenerator {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final ClockHolder clockHolder;

    public UuidOrderNoGenerator(ClockHolder clockHolder) {
        this.clockHolder = clockHolder;
    }

    @Override
    public String generate() {
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        return "ORD-" + clockHolder.now().format(DATE) + "-" + random;
    }

}
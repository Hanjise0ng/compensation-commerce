package com.han.commerce.support;

import com.han.commerce.common.application.ClockHolder;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
@Primary
public class MutableClockHolder implements ClockHolder {

    private static final LocalDateTime BASE = LocalDateTime.of(2026, 11, 11, 12, 0);

    private volatile LocalDateTime current = BASE;

    @Override
    public LocalDateTime now() {
        return current;
    }

    public void advance(Duration duration) {
        this.current = current.plus(duration);
    }

    public void reset() {
        this.current = BASE;
    }

}
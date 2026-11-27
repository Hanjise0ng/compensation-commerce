package com.han.commerce.support;

import com.han.commerce.order.application.placement.port.OrderNoGenerator;
import com.han.commerce.order.infrastructure.generator.UuidOrderNoGenerator;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
@Primary
public class SequenceOrderNoGenerator implements OrderNoGenerator {

    private final UuidOrderNoGenerator delegate;
    private final Queue<String> scripted = new ConcurrentLinkedQueue<>();

    public SequenceOrderNoGenerator(UuidOrderNoGenerator delegate) {
        this.delegate = delegate;
    }

    public void willGenerate(String... orderNos) {
        scripted.addAll(List.of(orderNos));
    }

    public void reset() {
        scripted.clear();
    }

    @Override
    public String generate() {
        String next = scripted.poll();
        return next != null ? next : delegate.generate();
    }

}
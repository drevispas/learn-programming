package org.demo.footballresource.nodb.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.LivenessState;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradingService {

    private final ApplicationEventPublisher applicationEventPublisher;

    public int getPendingOrders() {
        int pendingOrders = new Random().nextInt(100);
        log.debug("Pending orders: {}", pendingOrders);
        return pendingOrders;
    }

    public int tradeCards(int orders) {
        if (getPendingOrders() > 90) {
            AvailabilityChangeEvent.publish(applicationEventPublisher, new Exception("Too many pending orders"), LivenessState.BROKEN);
        } else {
            AvailabilityChangeEvent.publish(applicationEventPublisher, new Exception("working fine"), LivenessState.CORRECT);
        }
        return orders;
    }
}

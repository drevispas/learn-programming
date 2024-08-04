package org.demo.footballresource.nodb.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.observation.annotation.Observed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Observed(name = "football.auction")
@Slf4j
@Service
public class AuctionService {

    // Map of player and bid content
    private final Map<String, String> bids = new ConcurrentHashMap<>();
    private final Counter bidReceivedCounter;
    private final Timer bidDuration;
    private final Random random = new Random();

    public AuctionService(MeterRegistry meterRegistry) {
        meterRegistry.gauge("football.bids.pending", bids, Map::size);
        this.bidReceivedCounter = meterRegistry.counter("football.bids.received");
        this.bidDuration = meterRegistry.timer("football.bids.duration");
    }

    public void addBid(String player, String bid) {
        bidDuration.record(() -> {
            bids.put(player, bid);
            bidReceivedCounter.increment();
            try {
                Thread.sleep(random.nextInt(20));
            } catch (InterruptedException e) {
                log.error("Error adding bid", e);
            }
            bids.remove(player);
        });
    }

    public void addBidAOP(String player, String bid) {
        bids.put(player, bid);
        try {
            Thread.sleep(random.nextInt(20));
        } catch (InterruptedException e) {
            log.error("Error adding bid", e);
        }
        bids.remove(player);
    }
}

package org.movieTicketing.config;

import org.movieTicketing.SeatLockManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class BookingConfig {

    @Bean
    public SeatLockManager seatLockManager(
            @Value("${booking.seat-lock-duration:5m}") Duration seatLockDuration) {
        return new SeatLockManager(seatLockDuration);
    }
}

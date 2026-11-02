package org.movieTicketing;

import java.time.LocalDateTime;

public class SeatLock {
    private final String userId;
    private final LocalDateTime expirationTime;

    public SeatLock(String userId, LocalDateTime expirationtime) {
        this.userId = userId;
        this.expirationTime = expirationtime;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expirationTime);
    }

    public String getUserId() {
        return this.userId;
    }
}

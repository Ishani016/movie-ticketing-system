package org.movieTicketing;

import org.movieTicketing.models.Screen;
import org.movieTicketing.models.Seat;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

// Pessimistic locking. Every access to `lockedSeats` goes through a
// synchronized method, so a plain HashMap is sufficient and cheaper than
// ConcurrentHashMap; mixing the two would just be redundant synchronization.
public class SeatLockManager {
    private final Map<String, SeatLock> lockedSeats = new HashMap<>();
    private final Duration lockDuration;

    public SeatLockManager(Duration lockDuration) {
        this.lockDuration = lockDuration;
    }

    public synchronized boolean lockSeat(Screen screen, Seat seat, String userId) {
        String lockKey = getLockKey(screen, seat);
        if (isLocked(lockKey)) {
            return false;
        }
        SeatLock seatLock = new SeatLock(userId, LocalDateTime.now().plus(lockDuration));
        lockedSeats.put(lockKey, seatLock);
        return true;
    }

    private synchronized boolean isLocked(String lockKey) {
        cleanUpLockIfExpired(lockKey);
        return lockedSeats.containsKey(lockKey);
    }

    /** True if the seat is currently locked, and the lock is held by {@code userId}. */
    public synchronized boolean isLockedByUser(Screen screen, Seat seat, String userId) {
        String lockKey = getLockKey(screen, seat);
        cleanUpLockIfExpired(lockKey);
        SeatLock lock = lockedSeats.get(lockKey);
        return lock != null && lock.getUserId().equals(userId);
    }

    /**
     * Releases a lock, but only if it is still held by {@code userId}. Must be
     * called (ideally in a finally block) by whoever calls lockSeat, otherwise
     * the seat stays locked until the lock naturally expires.
     */
    public synchronized boolean unlockSeat(Screen screen, Seat seat, String userId) {
        String lockKey = getLockKey(screen, seat);
        SeatLock lock = lockedSeats.get(lockKey);
        if (lock != null && lock.getUserId().equals(userId)) {
            lockedSeats.remove(lockKey);
            return true;
        }
        return false;
    }

    private void cleanUpLockIfExpired(String lockKey) {
        SeatLock lock = lockedSeats.get(lockKey);
        if(lock != null && lock.isExpired()) {
            lockedSeats.remove(lockKey);
        }
    }

    private String getLockKey(Screen screen, Seat seat) {
        return screen.getId() + "-" + seat.getSeatNumber();
    }
}

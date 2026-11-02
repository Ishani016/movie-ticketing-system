package org.movieTicketing;

import org.junit.jupiter.api.Test;
import org.movieTicketing.models.Genre;
import org.movieTicketing.models.Layout;
import org.movieTicketing.models.Movie;
import org.movieTicketing.models.Room;
import org.movieTicketing.models.Screen;
import org.movieTicketing.models.Seat;
import org.movieTicketing.service.impl.NormalRate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeatLockManagerTest {

    private Screen newScreen() {
        Room room = new Room(new ArrayList<>(), new Layout(1, 1, new NormalRate()));
        Movie movie = new Movie("Tenet", Genre.SCI_FI, 150L);
        return new Screen(UUID.randomUUID(), movie, LocalDateTime.now(),
                LocalDateTime.now().plusHours(2), room, new BigDecimal("10.00"));
    }

    @Test
    void secondUserCannotLockAnAlreadyLockedSeat() {
        SeatLockManager lockManager = new SeatLockManager(Duration.ofMinutes(5));
        Screen screen = newScreen();
        Seat seat = screen.getRoom().getLayout().getAllSeats().get(0);

        assertTrue(lockManager.lockSeat(screen, seat, "user-1"));
        assertFalse(lockManager.lockSeat(screen, seat, "user-2"));
    }

    @Test
    void unlockingReleasesTheSeatForOtherUsers() {
        SeatLockManager lockManager = new SeatLockManager(Duration.ofMinutes(5));
        Screen screen = newScreen();
        Seat seat = screen.getRoom().getLayout().getAllSeats().get(0);

        lockManager.lockSeat(screen, seat, "user-1");
        lockManager.unlockSeat(screen, seat, "user-1");

        assertTrue(lockManager.lockSeat(screen, seat, "user-2"));
    }

    @Test
    void aUserCannotUnlockAnotherUsersLock() {
        SeatLockManager lockManager = new SeatLockManager(Duration.ofMinutes(5));
        Screen screen = newScreen();
        Seat seat = screen.getRoom().getLayout().getAllSeats().get(0);

        lockManager.lockSeat(screen, seat, "user-1");

        assertFalse(lockManager.unlockSeat(screen, seat, "user-2"));
        assertFalse(lockManager.lockSeat(screen, seat, "user-2"));
    }

    @Test
    void isLockedByUserReflectsOwnership() {
        SeatLockManager lockManager = new SeatLockManager(Duration.ofMinutes(5));
        Screen screen = newScreen();
        Seat seat = screen.getRoom().getLayout().getAllSeats().get(0);

        lockManager.lockSeat(screen, seat, "user-1");

        assertTrue(lockManager.isLockedByUser(screen, seat, "user-1"));
        assertFalse(lockManager.isLockedByUser(screen, seat, "user-2"));
    }

    @Test
    void expiredLocksCanBeReacquired() throws InterruptedException {
        SeatLockManager lockManager = new SeatLockManager(Duration.ofMillis(50));
        Screen screen = newScreen();
        Seat seat = screen.getRoom().getLayout().getAllSeats().get(0);

        lockManager.lockSeat(screen, seat, "user-1");
        Thread.sleep(100);

        assertTrue(lockManager.lockSeat(screen, seat, "user-2"));
    }
}

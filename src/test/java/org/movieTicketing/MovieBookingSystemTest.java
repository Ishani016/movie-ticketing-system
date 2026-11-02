package org.movieTicketing;

import org.junit.jupiter.api.Test;
import org.movieTicketing.models.Genre;
import org.movieTicketing.models.Layout;
import org.movieTicketing.models.Movie;
import org.movieTicketing.models.Order;
import org.movieTicketing.models.PaymentStatus;
import org.movieTicketing.models.Room;
import org.movieTicketing.models.Screen;
import org.movieTicketing.models.Seat;
import org.movieTicketing.models.Ticket;
import org.movieTicketing.models.User;
import org.movieTicketing.service.impl.NormalRate;
import org.movieTicketing.service.impl.PremiumRate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MovieBookingSystemTest {

    private MovieBookingSystem newSystemWithScreen(Screen[] screenOut, Seat[] seatOut) {
        MovieBookingSystem system = new MovieBookingSystem();
        Movie movie = new Movie("Interstellar", Genre.SCI_FI, 169L);
        system.addMovie(movie);

        Layout layout = new Layout(1, 1, new NormalRate(), Map.of(0, new PremiumRate()));
        Room room = new Room(new ArrayList<>(), layout);
        Screen screen = new Screen(UUID.randomUUID(), movie, LocalDateTime.now(),
                LocalDateTime.now().plusHours(3), room, new BigDecimal("12.00"));
        system.addScreening(movie, screen);

        screenOut[0] = screen;
        seatOut[0] = layout.getAllSeats().get(0);
        return system;
    }

    @Test
    void addScreeningKeepsRoomScreeningsInSync() {
        Screen[] screenOut = new Screen[1];
        Seat[] seatOut = new Seat[1];
        newSystemWithScreen(screenOut, seatOut);

        assertTrue(screenOut[0].getRoom().getScreenings().contains(screenOut[0]));
    }

    @Test
    void bookTicketOptimisticallyChargesAccordingToTheSeatStrategy() {
        Screen[] screenOut = new Screen[1];
        Seat[] seatOut = new Seat[1];
        MovieBookingSystem system = newSystemWithScreen(screenOut, seatOut);

        Ticket ticket = system.bookTicketOptimistically(screenOut[0], seatOut[0]);

        assertEquals(new BigDecimal("18.00"), ticket.getCost()); // 12.00 * 1.5 premium multiplier
    }

    @Test
    void bookingTheSameSeatTwiceOptimisticallyThrows() {
        Screen[] screenOut = new Screen[1];
        Seat[] seatOut = new Seat[1];
        MovieBookingSystem system = newSystemWithScreen(screenOut, seatOut);

        system.bookTicketOptimistically(screenOut[0], seatOut[0]);

        assertThrows(IllegalStateException.class,
                () -> system.bookTicketOptimistically(screenOut[0], seatOut[0]));
    }

    @Test
    void bookTicketWithLockFailsOnceTheSeatIsAlreadyBookedAndReleasesItsOwnLock() {
        Screen[] screenOut = new Screen[1];
        Seat[] seatOut = new Seat[1];
        MovieBookingSystem system = newSystemWithScreen(screenOut, seatOut);

        system.bookTicketWithLock(screenOut[0], seatOut[0], "user-1");

        // Seat is now booked (not merely locked); a second user must be rejected,
        // and must not be blocked forever by a lock the first call failed to release.
        assertThrows(IllegalStateException.class,
                () -> system.bookTicketWithLock(screenOut[0], seatOut[0], "user-2"));
    }

    @Test
    void checkoutProducesAPendingOrderWithTheCorrectTotal() {
        Screen[] screenOut = new Screen[1];
        Seat[] seatOut = new Seat[1];
        MovieBookingSystem system = newSystemWithScreen(screenOut, seatOut);
        User customer = new User("Grace Hopper", "grace@example.com");

        Ticket ticket = system.bookTicketOptimistically(screenOut[0], seatOut[0]);
        Order order = system.checkout(customer, List.of(ticket));

        assertEquals(new BigDecimal("18.00"), order.calculateTotalPrice());
        assertEquals(customer, order.getCustomer());
        assertEquals(PaymentStatus.PENDING, order.getPaymentStatus());
        assertEquals(1, system.getOrders().size());
    }

    @Test
    void payOrderMarksItPaidOrFailed() {
        Screen[] screenOut = new Screen[1];
        Seat[] seatOut = new Seat[1];
        MovieBookingSystem system = newSystemWithScreen(screenOut, seatOut);
        User customer = new User("Grace Hopper", "grace@example.com");
        Ticket ticket = system.bookTicketOptimistically(screenOut[0], seatOut[0]);
        Order order = system.checkout(customer, List.of(ticket));

        system.payOrder(order, true);
        assertEquals(PaymentStatus.PAID, order.getPaymentStatus());

        system.payOrder(order, false);
        assertEquals(PaymentStatus.FAILED, order.getPaymentStatus());
    }

    @Test
    void cancellingATicketFreesTheSeatBackUp() {
        Screen[] screenOut = new Screen[1];
        Seat[] seatOut = new Seat[1];
        MovieBookingSystem system = newSystemWithScreen(screenOut, seatOut);
        system.bookTicketOptimistically(screenOut[0], seatOut[0]);

        assertTrue(system.cancelTicket(screenOut[0], seatOut[0]));
        assertTrue(system.getAvailableSeats(screenOut[0]).contains(seatOut[0]));

        // And the seat can now be booked again.
        system.bookTicketOptimistically(screenOut[0], seatOut[0]);
    }

    @Test
    void cancellingACheckedOutTicketRefundsItsOrder() {
        Screen[] screenOut = new Screen[1];
        Seat[] seatOut = new Seat[1];
        MovieBookingSystem system = newSystemWithScreen(screenOut, seatOut);
        Ticket ticket = system.bookTicketOptimistically(screenOut[0], seatOut[0]);
        Order order = system.checkout(new User("Ada Lovelace", "ada@example.com"), List.of(ticket));
        system.payOrder(order, true);

        system.cancelTicket(screenOut[0], seatOut[0]);

        assertEquals(PaymentStatus.REFUNDED, order.getPaymentStatus());
    }

    @Test
    void holdSeatThenConfirmBookingSucceeds() {
        Screen[] screenOut = new Screen[1];
        Seat[] seatOut = new Seat[1];
        MovieBookingSystem system = newSystemWithScreen(screenOut, seatOut);

        assertTrue(system.holdSeat(screenOut[0], seatOut[0], "user-1"));
        Ticket ticket = system.confirmBooking(screenOut[0], seatOut[0], "user-1");

        assertEquals(new BigDecimal("18.00"), ticket.getCost());
    }

    @Test
    void confirmBookingWithoutAHoldIsRejected() {
        Screen[] screenOut = new Screen[1];
        Seat[] seatOut = new Seat[1];
        MovieBookingSystem system = newSystemWithScreen(screenOut, seatOut);

        assertThrows(IllegalStateException.class,
                () -> system.confirmBooking(screenOut[0], seatOut[0], "user-1"));
    }

    @Test
    void releasingAHoldLetsAnotherUserHoldTheSeat() {
        Screen[] screenOut = new Screen[1];
        Seat[] seatOut = new Seat[1];
        MovieBookingSystem system = newSystemWithScreen(screenOut, seatOut);

        system.holdSeat(screenOut[0], seatOut[0], "user-1");
        system.releaseHold(screenOut[0], seatOut[0], "user-1");

        assertTrue(system.holdSeat(screenOut[0], seatOut[0], "user-2"));
    }

    /**
     * The original bookTicketOptimistically was a classic TOCTOU race:
     * isSeatBooked() (read) followed by a separate addTicket() (write), with no
     * atomicity between them. Under concurrency, N threads could all observe
     * "not booked" and all insert a ticket for the same seat. This test fires
     * many threads at the same seat and asserts exactly one wins.
     */
    @Test
    void concurrentBookingOfTheSameSeatHasExactlyOneWinner() throws InterruptedException {
        Screen[] screenOut = new Screen[1];
        Seat[] seatOut = new Seat[1];
        MovieBookingSystem system = newSystemWithScreen(screenOut, seatOut);

        int threadCount = 50;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLine = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger failures = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            pool.submit(() -> {
                try {
                    startLine.await();
                    system.bookTicketOptimistically(screenOut[0], seatOut[0]);
                    successes.incrementAndGet();
                } catch (IllegalStateException e) {
                    failures.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        startLine.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));

        assertEquals(1, successes.get());
        assertEquals(threadCount - 1, failures.get());
        assertEquals(1, system.getTicketsForScreening(screenOut[0]).size());
    }
}

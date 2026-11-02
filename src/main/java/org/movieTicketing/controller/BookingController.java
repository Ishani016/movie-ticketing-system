package org.movieTicketing.controller;

import jakarta.validation.Valid;
import org.movieTicketing.MovieBookingSystem;
import org.movieTicketing.dto.BookSeatRequest;
import org.movieTicketing.dto.HoldSeatRequest;
import org.movieTicketing.dto.TicketResponse;
import org.movieTicketing.models.Screen;
import org.movieTicketing.models.Seat;
import org.movieTicketing.models.Ticket;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final MovieBookingSystem bookingSystem;

    public BookingController(MovieBookingSystem bookingSystem) {
        this.bookingSystem = bookingSystem;
    }

    /**
     * Optimistic book: atomic check-and-insert, no hold window.
     */
    @PostMapping("/optimistic")
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse bookOptimistically(@Valid @RequestBody BookSeatRequest request) {
        Screen screen = bookingSystem.requireScreen(request.screenId());
        Seat seat = bookingSystem.requireSeat(screen, request.seatNumber());
        Ticket ticket = bookingSystem.bookTicketOptimistically(screen, seat);
        return TicketResponse.from(ticket);
    }

    /**
     * Hold then immediately confirm (pessimistic lock path without a decision window).
     */
    @PostMapping("/with-lock")
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse bookWithLock(@Valid @RequestBody BookSeatRequest request) {
        if (request.userId() == null || request.userId().isBlank()) {
            throw new IllegalArgumentException("userId is required for locked booking");
        }
        Screen screen = bookingSystem.requireScreen(request.screenId());
        Seat seat = bookingSystem.requireSeat(screen, request.seatNumber());
        Ticket ticket = bookingSystem.bookTicketWithLock(screen, seat, request.userId());
        return TicketResponse.from(ticket);
    }

    @PostMapping("/hold")
    public Map<String, Object> holdSeat(@Valid @RequestBody HoldSeatRequest request) {
        Screen screen = bookingSystem.requireScreen(request.screenId());
        Seat seat = bookingSystem.requireSeat(screen, request.seatNumber());
        boolean held = bookingSystem.holdSeat(screen, seat, request.userId());
        if (!held) {
            throw new IllegalStateException(
                    "Seat " + request.seatNumber() + " could not be held (already locked or booked)");
        }
        return Map.of(
                "held", true,
                "screenId", request.screenId(),
                "seatNumber", request.seatNumber(),
                "userId", request.userId()
        );
    }

    @PostMapping("/confirm")
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse confirmHold(@Valid @RequestBody HoldSeatRequest request) {
        Screen screen = bookingSystem.requireScreen(request.screenId());
        Seat seat = bookingSystem.requireSeat(screen, request.seatNumber());
        Ticket ticket = bookingSystem.confirmBooking(screen, seat, request.userId());
        return TicketResponse.from(ticket);
    }

    @DeleteMapping("/hold")
    public Map<String, Object> releaseHold(@Valid @RequestBody HoldSeatRequest request) {
        Screen screen = bookingSystem.requireScreen(request.screenId());
        Seat seat = bookingSystem.requireSeat(screen, request.seatNumber());
        boolean released = bookingSystem.releaseHold(screen, seat, request.userId());
        if (!released) {
            throw new IllegalStateException(
                    "Hold could not be released (missing, expired, or owned by another user)");
        }
        return Map.of(
                "released", true,
                "screenId", request.screenId(),
                "seatNumber", request.seatNumber()
        );
    }

    @DeleteMapping
    public Map<String, Object> cancelTicket(
            @RequestParam UUID screenId,
            @RequestParam String seatNumber) {
        Screen screen = bookingSystem.requireScreen(screenId);
        Seat seat = bookingSystem.requireSeat(screen, seatNumber);
        boolean cancelled = bookingSystem.cancelTicket(screen, seat);
        if (!cancelled) {
            throw new IllegalStateException("No ticket found for seat " + seatNumber + " on screen " + screenId);
        }
        return Map.of(
                "cancelled", true,
                "screenId", screenId,
                "seatNumber", seatNumber
        );
    }
}

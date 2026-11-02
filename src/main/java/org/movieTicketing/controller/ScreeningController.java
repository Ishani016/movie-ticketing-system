package org.movieTicketing.controller;

import jakarta.validation.Valid;
import org.movieTicketing.MovieBookingSystem;
import org.movieTicketing.dto.CreateScreeningRequest;
import org.movieTicketing.dto.ScreenResponse;
import org.movieTicketing.dto.SeatResponse;
import org.movieTicketing.dto.TicketResponse;
import org.movieTicketing.models.Movie;
import org.movieTicketing.models.Room;
import org.movieTicketing.models.Screen;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/screenings")
public class ScreeningController {

    private final MovieBookingSystem bookingSystem;

    public ScreeningController(MovieBookingSystem bookingSystem) {
        this.bookingSystem = bookingSystem;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ScreenResponse createScreening(@Valid @RequestBody CreateScreeningRequest request) {
        Movie movie = bookingSystem.requireMovie(request.movieId());
        Room room = bookingSystem.requireRoom(request.cinemaId(), request.roomId());

        LocalDateTime endTime = request.endTime() != null
                ? request.endTime()
                : request.startTime().plusMinutes(movie.getDurationInMinutes());

        if (!endTime.isAfter(request.startTime())) {
            throw new IllegalArgumentException("endTime must be after startTime");
        }

        Screen screen = new Screen(
                UUID.randomUUID(),
                movie,
                request.startTime(),
                endTime,
                room,
                request.basePrice()
        );
        bookingSystem.addScreening(movie, screen);
        return ScreenResponse.from(screen);
    }

    @GetMapping("/{screenId}")
    public ScreenResponse getScreening(@PathVariable UUID screenId) {
        return ScreenResponse.from(bookingSystem.requireScreen(screenId));
    }

    @GetMapping("/{screenId}/seats/available")
    public List<SeatResponse> availableSeats(@PathVariable UUID screenId) {
        Screen screen = bookingSystem.requireScreen(screenId);
        return bookingSystem.getAvailableSeats(screen).stream()
                .map(seat -> SeatResponse.from(seat, screen.getBasePrice()))
                .toList();
    }

    @GetMapping("/{screenId}/tickets")
    public List<TicketResponse> tickets(@PathVariable UUID screenId) {
        Screen screen = bookingSystem.requireScreen(screenId);
        return bookingSystem.getTicketsForScreening(screen).stream()
                .map(TicketResponse::from)
                .toList();
    }
}

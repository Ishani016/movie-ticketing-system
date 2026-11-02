package org.movieTicketing.controller;

import jakarta.validation.Valid;
import org.movieTicketing.MovieBookingSystem;
import org.movieTicketing.dto.CinemaResponse;
import org.movieTicketing.dto.CreateCinemaRequest;
import org.movieTicketing.dto.CreateRoomRequest;
import org.movieTicketing.models.Cinema;
import org.movieTicketing.models.Layout;
import org.movieTicketing.models.Location;
import org.movieTicketing.models.Room;
import org.movieTicketing.service.SeatPricingStrategy;
import org.movieTicketing.service.impl.LuxeRate;
import org.movieTicketing.service.impl.NormalRate;
import org.movieTicketing.service.impl.PremiumRate;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/cinemas")
public class CinemaController {

    private final MovieBookingSystem bookingSystem;

    public CinemaController(MovieBookingSystem bookingSystem) {
        this.bookingSystem = bookingSystem;
    }

    @GetMapping
    public List<CinemaResponse> listCinemas() {
        return bookingSystem.getCinemas().stream().map(CinemaResponse::from).toList();
    }

    @GetMapping("/{cinemaId}")
    public CinemaResponse getCinema(@PathVariable UUID cinemaId) {
        return CinemaResponse.from(bookingSystem.requireCinema(cinemaId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CinemaResponse createCinema(@Valid @RequestBody CreateCinemaRequest request) {
        List<Room> rooms = request.rooms().stream().map(this::toRoom).toList();
        Location location = new Location(
                request.location().latitude(),
                request.location().longitude(),
                request.location().address()
        );
        Cinema cinema = new Cinema(request.name(), location, new ArrayList<>(rooms));
        bookingSystem.addCinema(cinema);
        return CinemaResponse.from(cinema);
    }

    private Room toRoom(CreateRoomRequest request) {
        Map<Integer, SeatPricingStrategy> overrides = new HashMap<>();
        if (request.premiumRows() != null) {
            request.premiumRows().forEach(row -> overrides.put(row, new PremiumRate()));
        }
        if (request.luxeRows() != null) {
            request.luxeRows().forEach(row -> overrides.put(row, new LuxeRate()));
        }
        Layout layout = new Layout(request.rows(), request.columns(), new NormalRate(), overrides);
        return new Room(request.name(), new ArrayList<>(), layout);
    }
}

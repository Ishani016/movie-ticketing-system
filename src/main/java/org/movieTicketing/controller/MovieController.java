package org.movieTicketing.controller;

import jakarta.validation.Valid;
import org.movieTicketing.MovieBookingSystem;
import org.movieTicketing.dto.CreateMovieRequest;
import org.movieTicketing.dto.MovieResponse;
import org.movieTicketing.dto.ScreenResponse;
import org.movieTicketing.models.Movie;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/movies")
public class MovieController {

    private final MovieBookingSystem bookingSystem;

    public MovieController(MovieBookingSystem bookingSystem) {
        this.bookingSystem = bookingSystem;
    }

    @GetMapping
    public List<MovieResponse> listMovies() {
        return bookingSystem.getMovies().stream().map(MovieResponse::from).toList();
    }

    @GetMapping("/{movieId}")
    public MovieResponse getMovie(@PathVariable UUID movieId) {
        return MovieResponse.from(bookingSystem.requireMovie(movieId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MovieResponse createMovie(@Valid @RequestBody CreateMovieRequest request) {
        Movie movie = new Movie(request.name(), request.genre(), request.durationInMinutes());
        bookingSystem.addMovie(movie);
        return MovieResponse.from(movie);
    }

    @GetMapping("/{movieId}/screenings")
    public List<ScreenResponse> listScreenings(@PathVariable UUID movieId) {
        Movie movie = bookingSystem.requireMovie(movieId);
        return bookingSystem.getScreeningsForMovie(movie).stream()
                .map(ScreenResponse::from)
                .toList();
    }
}

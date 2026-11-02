package org.movieTicketing;

import org.movieTicketing.models.Movie;
import org.movieTicketing.models.Screen;
import org.movieTicketing.models.Seat;
import org.movieTicketing.models.Ticket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ScreeningManager {
    private final Map<Movie, List<Screen>> screenByMovie;
    private final Map<Screen, List<Ticket>> ticketsByScreen;

    public ScreeningManager() {
        this.screenByMovie = new HashMap<>();
        this.ticketsByScreen = new HashMap<>();
    }

    /**
     * Registers a screening. Also appends the screen to its Room's own
     * screenings list (Room.screenings and this manager's index would
     * otherwise be two sources of truth that can silently drift apart).
     */
    public synchronized void addScreen(Movie movie, Screen screen) {
        Objects.requireNonNull(screen.getRoom(), "screen must be assigned to a room");
        Objects.requireNonNull(screen.getRoom().getScreenings(),
                "Room.screenings must be initialised (e.g. new ArrayList<>()) before adding screenings");

        screenByMovie.computeIfAbsent(movie, k -> new ArrayList<>())
                .add(screen);

        List<Screen> roomScreenings = screen.getRoom().getScreenings();
        if (!roomScreenings.contains(screen)) {
            roomScreenings.add(screen);
        }
    }

    /**
     * Atomically checks whether the ticket's seat is already booked for this
     * screening and, if not, books it. Check-and-insert happen under the same
     * lock so concurrent callers can never both succeed for the same seat.
     *
     * @return true if the ticket was booked, false if the seat was already taken
     */
    public synchronized boolean addTicketIfAvailable(Screen screen, Ticket ticket) {
        List<Ticket> tickets = ticketsByScreen.computeIfAbsent(screen, k -> new ArrayList<>());
        boolean alreadyBooked = tickets.stream()
                .anyMatch(existing -> existing.getSeat().equals(ticket.getSeat()));
        if (alreadyBooked) {
            return false;
        }
        tickets.add(ticket);
        return true;
    }

    /**
     * Cancels a previously booked seat, freeing it back up for booking.
     *
     * @return true if a ticket for that seat was found and removed
     */
    public synchronized boolean cancelTicket(Screen screen, Seat seat) {
        List<Ticket> tickets = ticketsByScreen.get(screen);
        if (tickets == null) {
            return false;
        }
        return tickets.removeIf(ticket -> ticket.getSeat().equals(seat));
    }

    public synchronized List<Ticket> getTicketsForScreening(Screen screen) {
        return new ArrayList<>(ticketsByScreen.getOrDefault(screen, new ArrayList<>()));
    }

    public synchronized List<Seat> getAvailableSeats(Screen screen) {
        List<Seat> allSeats = screen.getRoom().getLayout().getAllSeats();
        List<Seat> bookedSeats = ticketsByScreen.getOrDefault(screen, new ArrayList<>())
                .stream().map(Ticket::getSeat).toList();

        List<Seat> availableSeats = new ArrayList<>(allSeats);
        availableSeats.removeAll(bookedSeats);
        return availableSeats;
    }

    public synchronized List<Screen> getScreeningsForMovie(Movie movie) {
        return new ArrayList<>(screenByMovie.getOrDefault(movie, new ArrayList<>()));
    }
}

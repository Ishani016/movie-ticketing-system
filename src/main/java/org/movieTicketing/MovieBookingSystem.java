package org.movieTicketing;

import org.movieTicketing.models.Cinema;
import org.movieTicketing.models.Movie;
import org.movieTicketing.models.Order;
import org.movieTicketing.models.Room;
import org.movieTicketing.models.Screen;
import org.movieTicketing.models.Seat;
import org.movieTicketing.models.Ticket;
import org.movieTicketing.models.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Service
public class MovieBookingSystem {
    private final List<Movie> movies;
    private final List<Cinema> cinemas;
    private final List<Order> orders;
    private final ScreeningManager screeningManager;
    private final SeatLockManager seatLockManager;

    public MovieBookingSystem() {
        this(Duration.ofMinutes(5));
    }

    public MovieBookingSystem(Duration seatLockDuration) {
        this.movies = new ArrayList<>();
        this.cinemas = new ArrayList<>();
        this.orders = new ArrayList<>();
        this.screeningManager = new ScreeningManager();
        this.seatLockManager = new SeatLockManager(seatLockDuration);
    }

    @Autowired
    public MovieBookingSystem(SeatLockManager seatLockManager) {
        this.movies = new ArrayList<>();
        this.cinemas = new ArrayList<>();
        this.orders = new ArrayList<>();
        this.screeningManager = new ScreeningManager();
        this.seatLockManager = seatLockManager;
    }

    public void addMovie(Movie movie) {
        movies.add(movie);
    }

    public void addCinema(Cinema cinema) {
        cinemas.add(cinema);
    }

    public List<Movie> getMovies() {
        return new ArrayList<>(movies);
    }

    public List<Cinema> getCinemas() {
        return new ArrayList<>(cinemas);
    }

    public Movie requireMovie(UUID movieId) {
        return findMovie(movieId)
                .orElseThrow(() -> new NoSuchElementException("Movie not found: " + movieId));
    }

    public Optional<Movie> findMovie(UUID movieId) {
        return movies.stream().filter(movie -> movie.getId().equals(movieId)).findFirst();
    }

    public Cinema requireCinema(UUID cinemaId) {
        return findCinema(cinemaId)
                .orElseThrow(() -> new NoSuchElementException("Cinema not found: " + cinemaId));
    }

    public Optional<Cinema> findCinema(UUID cinemaId) {
        return cinemas.stream().filter(cinema -> cinema.getId().equals(cinemaId)).findFirst();
    }

    public Room requireRoom(UUID cinemaId, UUID roomId) {
        Cinema cinema = requireCinema(cinemaId);
        return cinema.getRooms().stream()
                .filter(room -> room.getId().equals(roomId))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException(
                        "Room not found: " + roomId + " in cinema " + cinemaId));
    }

    public Screen requireScreen(UUID screenId) {
        return findScreen(screenId)
                .orElseThrow(() -> new NoSuchElementException("Screen not found: " + screenId));
    }

    public Optional<Screen> findScreen(UUID screenId) {
        return movies.stream()
                .flatMap(movie -> screeningManager.getScreeningsForMovie(movie).stream())
                .filter(screen -> screen.getId().equals(screenId))
                .findFirst();
    }

    public Seat requireSeat(Screen screen, String seatNumber) {
        return screen.getRoom().getLayout().getAllSeats().stream()
                .filter(seat -> seat.getSeatNumber().equals(seatNumber))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException(
                        "Seat not found: " + seatNumber + " on screen " + screen.getId()));
    }

    public Order requireOrder(UUID orderId) {
        return findOrder(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));
    }

    public synchronized Optional<Order> findOrder(UUID orderId) {
        return orders.stream().filter(order -> order.getId().equals(orderId)).findFirst();
    }

    public void addScreening(Movie movie, Screen screen) {
        screeningManager.addScreen(movie, screen);
    }

    public List<Screen> getScreeningsForMovie(Movie movie) {
        return screeningManager.getScreeningsForMovie(movie);
    }

    public List<Seat> getAvailableSeats(Screen screen) {
        return screeningManager.getAvailableSeats(screen);
    }

    public List<Ticket> getTicketsForScreening(Screen screen) {
        return screeningManager.getTicketsForScreening(screen);
    }

    /**
     * Books a seat via an atomic check-then-insert in ScreeningManager: no lock
     * is held, but the availability check and the insert happen as a single
     * synchronized unit, so two concurrent callers can never both win the same seat.
     */
    public Ticket bookTicketOptimistically(Screen screen, Seat seat) {
        Ticket ticket = new Ticket(calculatePrice(screen, seat), seat, screen);
        if (!screeningManager.addTicketIfAvailable(screen, ticket)) {
            throw new IllegalStateException("Seat " + seat.getSeatNumber() + " has been booked already");
        }
        return ticket;
    }

    /**
     * Step 1 of the pessimistic (hold-then-confirm) flow: reserves the seat for
     * {@code userId} for the lock duration configured on this system, giving the
     * user a window to decide/pay before the seat is released back to the pool.
     * Other users cannot even attempt to book a held seat until it is released
     * (via confirmBooking, releaseHold, or expiry).
     */
    public boolean holdSeat(Screen screen, Seat seat, String userId) {
        return seatLockManager.lockSeat(screen, seat, userId);
    }

    /** Releases a hold without booking, e.g. the user abandoned checkout. */
    public boolean releaseHold(Screen screen, Seat seat, String userId) {
        return seatLockManager.unlockSeat(screen, seat, userId);
    }

    /**
     * Step 2: confirms a booking for a seat the caller currently holds. Fails if
     * the hold was never acquired, already expired, or belongs to someone else.
     */
    public Ticket confirmBooking(Screen screen, Seat seat, String userId) {
        if (!seatLockManager.isLockedByUser(screen, seat, userId)) {
            throw new IllegalStateException(
                    "Seat " + seat.getSeatNumber() + " is not held by " + userId + " (hold missing or expired)");
        }
        try {
            return bookTicketOptimistically(screen, seat);
        } finally {
            seatLockManager.unlockSeat(screen, seat, userId);
        }
    }

    /**
     * Convenience for callers that don't need a separate decision window
     * between holding and booking: hold the seat and confirm immediately.
     */
    public Ticket bookTicketWithLock(Screen screen, Seat seat, String userId) {
        if (!holdSeat(screen, seat, userId)) {
            throw new IllegalStateException("Seat " + seat.getSeatNumber() + " is currently locked by another user");
        }
        return confirmBooking(screen, seat, userId);
    }

    /**
     * Best-effort availability snapshot; not atomic. Use bookTicketOptimistically
     * or bookTicketWithLock to actually reserve a seat without racing.
     */
    public boolean isSeatBooked(Screen screen, Seat seat) {
        return getTicketsForScreening(screen).stream()
                .anyMatch(ticket -> ticket.getSeat().equals(seat));
    }

    public synchronized Order checkout(User customer, List<Ticket> tickets) {
        Order order = new Order(customer, new ArrayList<>(tickets), LocalDateTime.now());
        orders.add(order);
        return order;
    }

    /**
     * Simulates charging the customer. In a real system this would delegate to
     * a payment gateway; here it just records the outcome on the Order.
     * Note: mutates the Order instance directly rather than through the
     * synchronized `orders` list, so concurrent payOrder/cancelTicket calls on
     * the *same* Order are not mutually exclusive (unlike seat booking, which is).
     */
    public void payOrder(Order order, boolean paymentSucceeded) {
        if (paymentSucceeded) {
            order.markAsPaid();
        } else {
            order.markAsFailed();
        }
    }

    public synchronized List<Order> getOrders() {
        return new ArrayList<>(orders);
    }

    /**
     * Cancels a booked seat, freeing it up for other customers, and marks the
     * owning order (if one was checked out) as refunded.
     */
    public boolean cancelTicket(Screen screen, Seat seat) {
        boolean cancelled = screeningManager.cancelTicket(screen, seat);
        if (cancelled) {
            findOrderFor(screen, seat).ifPresent(Order::markAsRefunded);
        }
        return cancelled;
    }

    private synchronized Optional<Order> findOrderFor(Screen screen, Seat seat) {
        return orders.stream()
                .filter(order -> order.getTickets().stream()
                        .anyMatch(ticket -> ticket.getScreen().equals(screen) && ticket.getSeat().equals(seat)))
                .findFirst();
    }

    private BigDecimal calculatePrice(Screen screen, Seat seat) {
        return seat.getSeatPricingStrategy().getPrice(screen.getBasePrice());
    }
}

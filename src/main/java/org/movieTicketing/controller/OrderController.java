package org.movieTicketing.controller;

import jakarta.validation.Valid;
import org.movieTicketing.MovieBookingSystem;
import org.movieTicketing.dto.CheckoutRequest;
import org.movieTicketing.dto.OrderResponse;
import org.movieTicketing.dto.PayOrderRequest;
import org.movieTicketing.models.Order;
import org.movieTicketing.models.Screen;
import org.movieTicketing.models.Seat;
import org.movieTicketing.models.Ticket;
import org.movieTicketing.models.User;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final MovieBookingSystem bookingSystem;

    public OrderController(MovieBookingSystem bookingSystem) {
        this.bookingSystem = bookingSystem;
    }

    @GetMapping
    public List<OrderResponse> listOrders() {
        return bookingSystem.getOrders().stream().map(OrderResponse::from).toList();
    }

    @GetMapping("/{orderId}")
    public OrderResponse getOrder(@PathVariable UUID orderId) {
        return OrderResponse.from(bookingSystem.requireOrder(orderId));
    }

    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse checkout(@Valid @RequestBody CheckoutRequest request) {
        List<Ticket> tickets = new ArrayList<>();
        for (CheckoutRequest.BookedSeatRef ref : request.tickets()) {
            Screen screen = bookingSystem.requireScreen(ref.screenId());
            Seat seat = bookingSystem.requireSeat(screen, ref.seatNumber());
            Ticket ticket = bookingSystem.getTicketsForScreening(screen).stream()
                    .filter(t -> t.getSeat().equals(seat))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "Seat " + ref.seatNumber() + " is not booked on screen " + ref.screenId()
                                    + "; book before checkout"));
            tickets.add(ticket);
        }

        User customer = new User(request.customerName(), request.customerEmail());
        Order order = bookingSystem.checkout(customer, tickets);
        return OrderResponse.from(order);
    }

    @PostMapping("/{orderId}/pay")
    public OrderResponse pay(@PathVariable UUID orderId, @Valid @RequestBody PayOrderRequest request) {
        Order order = bookingSystem.requireOrder(orderId);
        bookingSystem.payOrder(order, request.success());
        return OrderResponse.from(order);
    }
}

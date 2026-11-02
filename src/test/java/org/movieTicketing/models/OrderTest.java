package org.movieTicketing.models;

import org.junit.jupiter.api.Test;
import org.movieTicketing.service.impl.NormalRate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderTest {

    @Test
    void newOrderStartsPendingAndSumsTicketCosts() {
        Seat seat = new Seat("1-1", new NormalRate());
        Screen screen = null; // not needed for cost calculation
        Ticket ticketA = new Ticket(new BigDecimal("10.00"), seat, screen);
        Ticket ticketB = new Ticket(new BigDecimal("5.50"), seat, screen);
        User customer = new User("Alan Turing", "alan@example.com");

        Order order = new Order(customer, new ArrayList<>(List.of(ticketA, ticketB)), LocalDateTime.now());

        assertEquals(PaymentStatus.PENDING, order.getPaymentStatus());
        assertEquals(new BigDecimal("15.50"), order.calculateTotalPrice());
    }

    @Test
    void markAsPaidAndMarkAsFailedUpdateStatus() {
        Order order = new Order(new User("Alan Turing", "alan@example.com"), new ArrayList<>(), LocalDateTime.now());

        order.markAsPaid();
        assertEquals(PaymentStatus.PAID, order.getPaymentStatus());

        order.markAsFailed();
        assertEquals(PaymentStatus.FAILED, order.getPaymentStatus());
    }
}

package org.movieTicketing.models;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Order {
    @EqualsAndHashCode.Include
    private final UUID id;
    private final User customer;
    private final List<Ticket> tickets;
    private final LocalDateTime orderDate;
    private PaymentStatus paymentStatus;

    public Order(User customer, List<Ticket> tickets, LocalDateTime orderDate) {
        this.id = UUID.randomUUID();
        this.customer = customer;
        this.tickets = tickets;
        this.orderDate = orderDate;
        this.paymentStatus = PaymentStatus.PENDING;
    }

    public BigDecimal calculateTotalPrice() {
        return tickets.stream().map(Ticket::getCost).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void markAsPaid() {
        this.paymentStatus = PaymentStatus.PAID;
    }

    public void markAsFailed() {
        this.paymentStatus = PaymentStatus.FAILED;
    }

    public void markAsRefunded() {
        this.paymentStatus = PaymentStatus.REFUNDED;
    }
}

package org.movieTicketing.dto;

import org.movieTicketing.models.Order;
import org.movieTicketing.models.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID customerId,
        String customerName,
        String customerEmail,
        List<TicketResponse> tickets,
        LocalDateTime orderDate,
        PaymentStatus paymentStatus,
        BigDecimal total
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getCustomer().getId(),
                order.getCustomer().getName(),
                order.getCustomer().getEmail(),
                order.getTickets().stream().map(TicketResponse::from).toList(),
                order.getOrderDate(),
                order.getPaymentStatus(),
                order.calculateTotalPrice()
        );
    }
}

package org.movieTicketing.dto;

import org.movieTicketing.models.Ticket;

import java.math.BigDecimal;
import java.util.UUID;

public record TicketResponse(
        UUID screenId,
        String seatNumber,
        BigDecimal cost
) {
    public static TicketResponse from(Ticket ticket) {
        return new TicketResponse(
                ticket.getScreen().getId(),
                ticket.getSeat().getSeatNumber(),
                ticket.getCost()
        );
    }
}

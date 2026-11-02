package org.movieTicketing.models;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@AllArgsConstructor
@Data
public class Ticket {
    private BigDecimal cost;
    private Seat seat;
    private Screen screen;
}

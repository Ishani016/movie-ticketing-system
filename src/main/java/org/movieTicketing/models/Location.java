package org.movieTicketing.models;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@AllArgsConstructor
@Data
public class Location {
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String address;
}

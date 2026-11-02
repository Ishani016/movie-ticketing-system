package org.movieTicketing.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

// Screen <-> Room is a two-way reference (Room.screenings holds Screens back).
// Identity is pinned to `id` only, and `room` is excluded from toString, so
// neither equals/hashCode nor toString ever recurse into Room and back.
@AllArgsConstructor
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Screen {
    @EqualsAndHashCode.Include
    private UUID id;
    private Movie movie;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    @ToString.Exclude
    private Room room;
    private BigDecimal basePrice;

    public Duration getDuration() {
        return Duration.between(startTime, endTime);
    }
}

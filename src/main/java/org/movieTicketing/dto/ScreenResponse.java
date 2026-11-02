package org.movieTicketing.dto;

import org.movieTicketing.models.Screen;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ScreenResponse(
        UUID id,
        UUID movieId,
        String movieName,
        UUID roomId,
        String roomName,
        LocalDateTime startTime,
        LocalDateTime endTime,
        BigDecimal basePrice
) {
    public static ScreenResponse from(Screen screen) {
        return new ScreenResponse(
                screen.getId(),
                screen.getMovie().getId(),
                screen.getMovie().getName(),
                screen.getRoom().getId(),
                screen.getRoom().getName(),
                screen.getStartTime(),
                screen.getEndTime(),
                screen.getBasePrice()
        );
    }
}

package org.movieTicketing.dto;

import org.movieTicketing.models.Cinema;
import org.movieTicketing.models.Location;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CinemaResponse(
        UUID id,
        String name,
        LocationResponse location,
        List<RoomResponse> rooms
) {
    public record LocationResponse(BigDecimal latitude, BigDecimal longitude, String address) {
        public static LocationResponse from(Location location) {
            return new LocationResponse(location.getLatitude(), location.getLongitude(), location.getAddress());
        }
    }

    public static CinemaResponse from(Cinema cinema) {
        return new CinemaResponse(
                cinema.getId(),
                cinema.getName(),
                LocationResponse.from(cinema.getLocation()),
                cinema.getRooms().stream().map(RoomResponse::from).toList()
        );
    }
}

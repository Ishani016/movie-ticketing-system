package org.movieTicketing.dto;

import org.movieTicketing.models.Room;

import java.util.UUID;

public record RoomResponse(
        UUID id,
        String name,
        int rows,
        int columns,
        int seatCount
) {
    public static RoomResponse from(Room room) {
        return new RoomResponse(
                room.getId(),
                room.getName(),
                room.getLayout().getRows(),
                room.getLayout().getColumns(),
                room.getLayout().getAllSeats().size()
        );
    }
}

package org.movieTicketing;

import org.junit.jupiter.api.Test;
import org.movieTicketing.models.Genre;
import org.movieTicketing.models.Layout;
import org.movieTicketing.models.Movie;
import org.movieTicketing.models.Room;
import org.movieTicketing.models.Screen;
import org.movieTicketing.models.Seat;
import org.movieTicketing.models.Ticket;
import org.movieTicketing.service.impl.NormalRate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScreeningManagerTest {

    private Screen newScreen() {
        Layout layout = new Layout(1, 2, new NormalRate());
        Room room = new Room(new ArrayList<>(), layout);
        Movie movie = new Movie("Arrival", Genre.SCI_FI, 116L);
        return new Screen(UUID.randomUUID(), movie, LocalDateTime.now(),
                LocalDateTime.now().plusHours(2), room, new BigDecimal("10.00"));
    }

    @Test
    void addTicketIfAvailableSucceedsOnlyOncePerSeat() {
        ScreeningManager manager = new ScreeningManager();
        Screen screen = newScreen();
        Seat seat = screen.getRoom().getLayout().getAllSeats().get(0);
        Ticket first = new Ticket(BigDecimal.TEN, seat, screen);
        Ticket second = new Ticket(BigDecimal.TEN, seat, screen);

        assertTrue(manager.addTicketIfAvailable(screen, first));
        assertFalse(manager.addTicketIfAvailable(screen, second));
    }

    @Test
    void cancelTicketFreesTheSeatAndReportsWhetherOneWasRemoved() {
        ScreeningManager manager = new ScreeningManager();
        Screen screen = newScreen();
        Seat seat = screen.getRoom().getLayout().getAllSeats().get(0);
        manager.addTicketIfAvailable(screen, new Ticket(BigDecimal.TEN, seat, screen));

        assertTrue(manager.cancelTicket(screen, seat));
        assertFalse(manager.cancelTicket(screen, seat)); // already cancelled, nothing to remove
        assertTrue(manager.getAvailableSeats(screen).contains(seat));
    }

    @Test
    void availableSeatsExcludeBookedOnes() {
        ScreeningManager manager = new ScreeningManager();
        Screen screen = newScreen();
        List<Seat> allSeats = screen.getRoom().getLayout().getAllSeats();
        Seat bookedSeat = allSeats.get(0);
        manager.addTicketIfAvailable(screen, new Ticket(BigDecimal.TEN, bookedSeat, screen));

        List<Seat> available = manager.getAvailableSeats(screen);

        assertFalse(available.contains(bookedSeat));
        assertTrue(available.contains(allSeats.get(1)));
    }
}

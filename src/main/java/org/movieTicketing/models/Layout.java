package org.movieTicketing.models;

import lombok.Data;
import org.movieTicketing.service.SeatPricingStrategy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Data
public class Layout {
    private int rows;
    private int columns;
    private Map<String, Seat> seatByNumber;
    private Map<Integer, Map<Integer, Seat>> seatByPosition;

    public Layout(int rows, int columns, SeatPricingStrategy defaultStrategy) {
        this(rows, columns, defaultStrategy, Map.of());
    }

    /**
     * @param defaultStrategy        pricing strategy applied to every seat
     * @param strategyOverridesByRow optional per-row overrides, e.g. back rows priced as Luxe
     */
    public Layout(int rows, int columns, SeatPricingStrategy defaultStrategy,
                  Map<Integer, SeatPricingStrategy> strategyOverridesByRow) {
        Objects.requireNonNull(defaultStrategy, "defaultStrategy must not be null");
        this.rows = rows;
        this.columns = columns;
        this.seatByNumber = new HashMap<>();
        this.seatByPosition = new HashMap<>();
        initialise(defaultStrategy, strategyOverridesByRow == null ? Map.of() : strategyOverridesByRow);
    }

    private void initialise(SeatPricingStrategy defaultStrategy,
                             Map<Integer, SeatPricingStrategy> strategyOverridesByRow) {
        for (int row = 0; row < rows; row++) {
            SeatPricingStrategy strategyForRow = strategyOverridesByRow.getOrDefault(row, defaultStrategy);
            for (int column = 0; column < columns; column++) {
                String seatNumber = row + "-" + column;
                addSeat(new Seat(seatNumber, strategyForRow), row, column);
            }
        }
    }

    private void addSeat(Seat seat, int row, int column) {
        seatByNumber.put(seat.getSeatNumber(), seat);
        seatByPosition.computeIfAbsent(row, k -> new HashMap<>()).put(column, seat);
    }

    public Optional<Seat> getSeatAt(int row, int column) {
        return Optional.ofNullable(seatByPosition.getOrDefault(row, Map.of()).get(column));
    }

    public List<Seat> getAllSeats() {
        return new ArrayList<>(seatByNumber.values());
    }
}

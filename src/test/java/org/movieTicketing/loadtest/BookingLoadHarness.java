package org.movieTicketing.loadtest;

import org.movieTicketing.MovieBookingSystem;
import org.movieTicketing.models.Genre;
import org.movieTicketing.models.Layout;
import org.movieTicketing.models.Movie;
import org.movieTicketing.models.Order;
import org.movieTicketing.models.Room;
import org.movieTicketing.models.Screen;
import org.movieTicketing.models.Seat;
import org.movieTicketing.models.Ticket;
import org.movieTicketing.models.User;
import org.movieTicketing.service.impl.LuxeRate;
import org.movieTicketing.service.impl.NormalRate;
import org.movieTicketing.service.impl.PremiumRate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Ad-hoc in-process load/stress harness for MovieBookingSystem. Not a JUnit
 * test (no @Test methods, class name deliberately avoids Surefire's default
 * *Test.java / Test*.java patterns) so it never runs as part of `mvn test`.
 *
 * Run with:
 *   mvn test-compile exec:java@load-test
 *   mvn test-compile exec:java@load-test -Dthreads=8,16,64 -DdurationSeconds=15
 *
 * What it does:
 *   - Sweeps a list of thread counts (default 1,2,4,8,16,32,64) and reports
 *     throughput, ops/sec/thread, and speedup+efficiency relative to the
 *     first level, so a plateau (the expected outcome, since almost every
 *     write path funnels through a handful of instance-level `synchronized`
 *     methods on ScreeningManager/MovieBookingSystem) is visible as a number,
 *     not just a vibe from the raw totals.
 *   - Runs a fresh MovieBookingSystem per thread-count level for a fixed wall
 *     clock duration, with a mixed, randomized workload: book (50%), cancel
 *     (20%), checkout (10%), pay (10%), read-available-seats (10%).
 *   - Seat inventory is large (many screens x many seats) and continuously
 *     recycled via cancellations, so the system doesn't just fill up and
 *     degenerate into "every booking attempt fails" (a "full room") partway
 *     through the run.
 *   - Tracks latency SEPARATELY per operation+outcome, using one plain long[]
 *     growable buffer per worker thread (no shared queue in the hot path) so
 *     the measurement itself isn't a contention point.
 *   - Buckets every sample into "first 20% of the run" vs "last 20% of the
 *     run" (middle 60% discarded) and reports both. A flat run-wide average
 *     would hide cancelTicket() -> findOrderFor()'s linear scan over the
 *     ever-growing `orders` list; comparing early vs late surfaces it.
 *   - Collects every worker's Future and calls get() after awaitTermination,
 *     so a worker that dies on an unexpected exception fails loudly instead
 *     of silently vanishing from the aggregated stats.
 *   - Verifies a correctness invariant after each level: for every screen,
 *     availableSeats + bookedTickets == total seats in that screen's layout.
 *
 * Simplification: cancel and checkout/pay draw from independent pools of
 * "previously booked" records, so in rare cases a seat already cancelled by
 * one path may still get checked out/paid via the other. That's harmless for
 * a throughput/contention measurement (no exceptions, no corrupted state)
 * but means this is NOT a business-flow correctness test - see
 * MovieBookingSystemTest for that.
 */
public final class BookingLoadHarness {

    private enum OpType { BOOK_SUCCESS, BOOK_FAIL, CANCEL_HIT, CANCEL_MISS, CHECKOUT, PAY, READ }

    private enum Window { EARLY, LATE }

    public static void main(String[] args) throws InterruptedException {
        int[] threadCounts = parseThreadCounts();
        int durationSeconds = parseDurationSeconds();
        int numScreens = 20;
        int rows = 10;
        int columns = 20; // 200 seats/screen * 20 screens = 4000 seats of inventory

        System.out.printf("BookingLoadHarness: screens=%d seats/screen=%d durationPerLevel=%ds levels=%s%n",
                numScreens, rows * columns, durationSeconds, Arrays.toString(threadCounts));
        System.out.println();

        List<Long> throughputs = new ArrayList<>();
        for (int threadCount : threadCounts) {
            long throughput = runLevel(threadCount, durationSeconds, numScreens, rows, columns);
            throughputs.add(throughput);
        }

        System.out.println();
        System.out.println("=== Throughput, speedup & efficiency vs thread count ===");
        System.out.printf("  %-8s %14s %16s %10s %12s%n",
                "threads", "ops/sec", "ops/sec/thread", "speedup", "efficiency");
        long baselineThroughput = throughputs.get(0);
        int baselineThreads = threadCounts[0];
        for (int i = 0; i < threadCounts.length; i++) {
            int threads = threadCounts[i];
            long tp = throughputs.get(i);
            double perThread = tp / (double) threads;
            double speedup = tp / (double) baselineThroughput;
            double idealSpeedup = threads / (double) baselineThreads;
            double efficiencyPct = 100.0 * speedup / idealSpeedup;
            System.out.printf("  %-8d %,14d %,16.0f %9.2fx %11.1f%%%n",
                    threads, tp, perThread, speedup, efficiencyPct);
        }
        System.out.println(
                "efficiency = (throughput speedup) / (thread-count multiple) relative to the first level, " +
                "100% == linear scaling. Falling efficiency means the shared synchronized state in " +
                "ScreeningManager / MovieBookingSystem - not CPU/core count - is the bottleneck.");
    }

    private static long runLevel(int threadCount, int durationSeconds, int numScreens, int rows, int columns)
            throws InterruptedException {
        MovieBookingSystem system = new MovieBookingSystem();
        Movie movie = new Movie("Load Test Movie", Genre.ACTION, 120L);
        system.addMovie(movie);

        Screen[] screens = new Screen[numScreens];
        Seat[][] seatsByScreen = new Seat[numScreens][];
        for (int i = 0; i < numScreens; i++) {
            Layout layout = new Layout(rows, columns, new NormalRate(),
                    Map.of(0, new PremiumRate(), rows - 1, new LuxeRate()));
            Room room = new Room(new ArrayList<>(), layout);
            Screen screen = new Screen(UUID.randomUUID(), movie, LocalDateTime.now(),
                    LocalDateTime.now().plusHours(2), room, new BigDecimal("10.00"));
            system.addScreening(movie, screen);
            screens[i] = screen;
            seatsByScreen[i] = layout.getAllSeats().toArray(new Seat[0]); // precomputed, outside the timed loop
        }

        ConcurrentLinkedQueue<SeatRef> bookedSeats = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<TicketRef> pendingCheckout = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<Order> pendingPayment = new ConcurrentLinkedQueue<>();
        AtomicLong totalOps = new AtomicLong();
        AtomicBoolean stop = new AtomicBoolean(false);
        AtomicLong levelStartRef = new AtomicLong();
        long durationNanos = durationSeconds * 1_000_000_000L;
        long earlyCutoffNanos = (long) (durationNanos * 0.2);
        long lateCutoffNanos = (long) (durationNanos * 0.8);

        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<WorkerResult> results = java.util.Collections.synchronizedList(new ArrayList<>());
        List<Future<?>> futures = new ArrayList<>();

        for (int t = 0; t < threadCount; t++) {
            futures.add(pool.submit(() -> {
                Map<OpType, LongArray> earlyLatencies = newLatencyMap();
                Map<OpType, LongArray> lateLatencies = newLatencyMap();
                User dummyUser = new User("load-test-user", "load-test@example.com");
                ThreadLocalRandom random = ThreadLocalRandom.current();

                readyLatch.countDown();
                await(startLatch);
                long levelStart = levelStartRef.get();

                long localOps = 0;
                while (!stop.get()) {
                    int screenIdx = random.nextInt(numScreens);
                    Screen screen = screens[screenIdx];
                    Seat[] seats = seatsByScreen[screenIdx];
                    Seat seat = seats[random.nextInt(seats.length)];

                    int pick = random.nextInt(100);
                    long start = System.nanoTime();
                    OpType type;
                    if (pick < 50) {
                        try {
                            Ticket ticket = system.bookTicketOptimistically(screen, seat);
                            type = OpType.BOOK_SUCCESS;
                            bookedSeats.add(new SeatRef(screen, seat));
                            pendingCheckout.add(new TicketRef(ticket));
                        } catch (IllegalStateException e) {
                            type = OpType.BOOK_FAIL;
                        }
                    } else if (pick < 70) {
                        SeatRef ref = bookedSeats.poll();
                        boolean cancelled = ref != null && system.cancelTicket(ref.screen, ref.seat);
                        type = cancelled ? OpType.CANCEL_HIT : OpType.CANCEL_MISS;
                    } else if (pick < 80) {
                        TicketRef ref = pendingCheckout.poll();
                        if (ref != null) {
                            Order order = system.checkout(dummyUser, List.of(ref.ticket));
                            pendingPayment.add(order);
                        }
                        type = OpType.CHECKOUT;
                    } else if (pick < 90) {
                        Order order = pendingPayment.poll();
                        if (order != null) {
                            system.payOrder(order, true);
                        }
                        type = OpType.PAY;
                    } else {
                        system.getAvailableSeats(screen);
                        type = OpType.READ;
                    }
                    long latency = System.nanoTime() - start;
                    long elapsedSinceLevelStart = start - levelStart;
                    if (elapsedSinceLevelStart < earlyCutoffNanos) {
                        earlyLatencies.get(type).add(latency);
                    } else if (elapsedSinceLevelStart > lateCutoffNanos) {
                        lateLatencies.get(type).add(latency);
                    } // middle 60% is measured for throughput but not bucketed for the early/late comparison
                    localOps++;
                }
                totalOps.addAndGet(localOps);
                results.add(new WorkerResult(earlyLatencies, lateLatencies));
            }));
        }

        await(readyLatch);
        long levelStart = System.nanoTime();
        levelStartRef.set(levelStart);
        startLatch.countDown();
        Thread.sleep(durationSeconds * 1000L);
        stop.set(true);
        pool.shutdown();
        if (!pool.awaitTermination(30, TimeUnit.SECONDS)) {
            System.err.println("WARNING: workers did not terminate cleanly within 30s grace period");
        }
        long elapsedNanos = System.nanoTime() - levelStart;

        // Surface any worker that died on an unexpected exception instead of silently dropping its stats.
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (ExecutionException e) {
                throw new RuntimeException("A load-test worker thread failed", e.getCause());
            }
        }

        Map<OpType, LongArray> mergedEarly = newLatencyMap();
        Map<OpType, LongArray> mergedLate = newLatencyMap();
        for (WorkerResult r : results) {
            for (OpType type : OpType.values()) {
                mergedEarly.get(type).addAll(r.earlyLatencies.get(type));
                mergedLate.get(type).addAll(r.lateLatencies.get(type));
            }
        }

        long throughput = Math.round(totalOps.get() / (elapsedNanos / 1_000_000_000.0));
        System.out.printf("--- threads=%d  totalOps=%,d  wallClock=%.1fs  throughput=%,d ops/sec  ordersCreated=%,d ---%n",
                threadCount, totalOps.get(), elapsedNanos / 1_000_000_000.0, throughput, system.getOrders().size());
        printEarlyVsLate(mergedEarly, mergedLate);
        verifyConsistency(system, screens);
        System.out.println();

        return throughput;
    }

    private static Map<OpType, LongArray> newLatencyMap() {
        Map<OpType, LongArray> map = new EnumMap<>(OpType.class);
        for (OpType type : OpType.values()) {
            map.put(type, new LongArray());
        }
        return map;
    }

    /**
     * Prints avg/p95 latency per operation for the first 20% vs last 20% of
     * the run, plus the late/early ratio. A ratio near 1.0 means latency was
     * stable; a ratio growing with thread count/run length for CANCEL_HIT
     * indicates findOrderFor()'s linear scan over `orders` degrading as
     * checkouts accumulate.
     */
    private static void printEarlyVsLate(Map<OpType, LongArray> early, Map<OpType, LongArray> late) {
        System.out.printf("  %-14s %9s %9s %12s %12s %12s %12s %10s%n",
                "operation", "early_n", "late_n", "early_avg(us)", "early_p95(us)", "late_avg(us)", "late_p95(us)", "late/early");
        for (OpType type : OpType.values()) {
            long[] earlyData = early.get(type).toSortedArray();
            long[] lateData = late.get(type).toSortedArray();
            if (earlyData.length == 0 && lateData.length == 0) {
                System.out.printf("  %-14s %9d %9d%n", type, 0, 0);
                continue;
            }
            double earlyAvgUs = avgUs(earlyData);
            double lateAvgUs = avgUs(lateData);
            String ratio = (earlyData.length > 0 && lateData.length > 0 && earlyAvgUs > 0)
                    ? String.format("%.2fx", lateAvgUs / earlyAvgUs)
                    : "n/a";
            System.out.printf("  %-14s %9d %9d %12.1f %12.1f %12.1f %12.1f %10s%n",
                    type, earlyData.length, lateData.length, earlyAvgUs, percentileUs(earlyData, 95),
                    lateAvgUs, percentileUs(lateData, 95), ratio);
        }
        // late_n vs early_n for BOOK_SUCCESS/BOOK_FAIL shows whether the room saturated mid-run:
        // if late BOOK_FAIL's share of (success+fail) is much higher than early's, cancellations
        // aren't recycling seats fast enough and the "late" window is measuring a fuller room,
        // not just a longer `orders` list - read the CANCEL_HIT/CHECKOUT ratios with that caveat.
    }

    private static double avgUs(long[] sortedData) {
        return sortedData.length == 0 ? 0.0 : Arrays.stream(sortedData).average().orElse(0) / 1000.0;
    }

    private static double percentileUs(long[] sortedData, double p) {
        if (sortedData.length == 0) {
            return 0.0;
        }
        int index = (int) Math.ceil(p / 100.0 * sortedData.length) - 1;
        return sortedData[Math.max(0, Math.min(index, sortedData.length - 1))] / 1000.0;
    }

    /** Correctness invariant: every seat is either available or booked, never both/neither. */
    private static void verifyConsistency(MovieBookingSystem system, Screen[] screens) {
        boolean allOk = true;
        for (Screen screen : screens) {
            int totalSeats = screen.getRoom().getLayout().getAllSeats().size();
            int available = system.getAvailableSeats(screen).size();
            int booked = system.getTicketsForScreening(screen).size();
            if (available + booked != totalSeats) {
                allOk = false;
                System.out.printf("  INCONSISTENCY screen=%s total=%d available=%d booked=%d%n",
                        screen.getId(), totalSeats, available, booked);
            }
        }
        System.out.println("  consistency check: " + (allOk ? "PASS" : "FAIL"));
    }

    private static int[] parseThreadCounts() {
        String property = System.getProperty("threads");
        if (property != null) {
            return Arrays.stream(property.split(",")).mapToInt(Integer::parseInt).toArray();
        }
        return new int[] {1, 2, 4, 8, 16, 32, 64};
    }

    private static int parseDurationSeconds() {
        String property = System.getProperty("durationSeconds");
        return property != null ? Integer.parseInt(property) : 10;
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private record SeatRef(Screen screen, Seat seat) { }

    private record TicketRef(Ticket ticket) { }

    private static final class WorkerResult {
        final Map<OpType, LongArray> earlyLatencies;
        final Map<OpType, LongArray> lateLatencies;

        WorkerResult(Map<OpType, LongArray> earlyLatencies, Map<OpType, LongArray> lateLatencies) {
            this.earlyLatencies = earlyLatencies;
            this.lateLatencies = lateLatencies;
        }
    }

    /** Minimal growable primitive long array - avoids boxing and shared-queue contention in the hot path. */
    private static final class LongArray {
        private long[] data = new long[256];
        private int size = 0;

        void add(long value) {
            if (size == data.length) {
                data = Arrays.copyOf(data, data.length * 2);
            }
            data[size++] = value;
        }

        void addAll(LongArray other) {
            for (int i = 0; i < other.size; i++) {
                add(other.data[i]);
            }
        }

        long[] toSortedArray() {
            long[] trimmed = Arrays.copyOf(data, size);
            Arrays.sort(trimmed);
            return trimmed;
        }
    }
}

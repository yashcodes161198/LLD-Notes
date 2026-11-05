package parkinglot.concurrency;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

// Side-by-side proof: naive check-then-act double-books a spot, compareAndSet never does.
public class RaceConditionDemo {

    static class NaiveSpot {
        private boolean occupied = false;

        boolean tryOccupy() {
            if (occupied) return false;
            Thread.yield(); // widen the gap between "check" and "act" so the bug shows up
            occupied = true;
            return true;
        }
    }

    static class AtomicSpot {
        private final AtomicBoolean occupied = new AtomicBoolean(false);

        boolean tryOccupy() { return occupied.compareAndSet(false, true); }
    }

    public static void main(String[] args) throws InterruptedException {
        int threads = 50, rounds = 200;
        int naiveDoubleBookings = 0, atomicDoubleBookings = 0;
        for (int r = 0; r < rounds; r++) {
            NaiveSpot naive = new NaiveSpot();
            if (winners(threads, naive::tryOccupy) > 1) naiveDoubleBookings++;
            AtomicSpot atomic = new AtomicSpot();
            if (winners(threads, atomic::tryOccupy) > 1) atomicDoubleBookings++;
        }
        System.out.println("Rounds: " + rounds + ", threads per round: " + threads);
        System.out.println("  NaiveSpot  double-booked in " + naiveDoubleBookings + " rounds");
        System.out.println("  AtomicSpot double-booked in " + atomicDoubleBookings + " rounds (always 0)");
    }

    interface Occupy { boolean tryOccupy(); }

    private static int winners(int n, Occupy spot) throws InterruptedException {
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger wins = new AtomicInteger();
        List<Thread> ts = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Thread t = new Thread(() -> {
                try { go.await(); } catch (InterruptedException e) { return; }
                if (spot.tryOccupy()) wins.incrementAndGet();
            });
            ts.add(t);
            t.start();
        }
        go.countDown();
        for (Thread t : ts) t.join();
        return wins.get();
    }
}

package parkinglot;

import parkinglot.enums.PaymentMode;
import parkinglot.enums.PricingStrategyType;
import parkinglot.enums.VehicleType;
import parkinglot.factory.PricingStrategyFactory;
import parkinglot.factory.VehicleFactory;
import parkinglot.model.*;
import parkinglot.service.ParkingLot;
import parkinglot.utils.DateTimeParser;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Phase 6: Concurrency ===");
        ParkingLot lot = ParkingLot.getInstance();
        EntryGate entryGate = new EntryGate("EG1");
        ExitGate exitGate = new ExitGate("XG1");
        lot.setPricingStrategy(PricingStrategyFactory.get(PricingStrategyType.EVENT_BASED));

        ParkingFloor floor1 = new ParkingFloor("Floor1");
        floor1.addSpot(new ParkingSpot("F1S1", VehicleType.BIKE));
        floor1.addSpot(new ParkingSpot("F1S2", VehicleType.CAR));
        floor1.addSpot(new ParkingSpot("F1S3", VehicleType.TRUCK));
        floor1.addSpot(new ParkingSpot("F1S4", VehicleType.CAR));
        lot.addFloor(floor1);

        // Scenario 1 (from the video): two bikes, one bike spot, two entry threads at the same instant.
        System.out.println("-- Scenario 1: 2 bikes race for 1 bike spot --");
        LocalDateTime entryTime = DateTimeParser.parse("21 May 7:30 AM 2025");
        Vehicle bike1 = VehicleFactory.create("KA01AB1234", VehicleType.BIKE);
        Vehicle bike2 = VehicleFactory.create("KA01AB5678", VehicleType.BIKE);
        int bikeWinners = race(List.of(bike1, bike2), v -> entryGate.parkVehicle(v, entryTime));
        System.out.println("  Bikes parked: " + bikeWinners + " (expected 1)");

        // Scenario 2: 6 cars, 2 car spots.
        System.out.println("-- Scenario 2: 6 cars race for 2 car spots --");
        List<Vehicle> cars = new ArrayList<>();
        for (int i = 0; i < 6; i++) cars.add(VehicleFactory.create("CAR-" + i, VehicleType.CAR));
        int carWinners = race(cars, v -> entryGate.parkVehicle(v, entryTime));
        System.out.println("  Cars parked: " + carWinners + " (expected 2)");

        System.out.println("--------------------------");
        lot.printStatus();
        System.out.println("--------------------------");

        String anyTicket = lot.getActiveTickets().keySet().iterator().next();
        exitGate.unparkVehicle(anyTicket, DateTimeParser.parse("21 May 1:15 PM 2025"), PaymentMode.CARD);
        lot.printStatus();
    }

    interface ParkAction { Ticket park(Vehicle v); }

    // Starts one thread per vehicle and releases them together to maximise contention.
    private static int race(List<Vehicle> vehicles, ParkAction action) throws InterruptedException {
        CountDownLatch startGun = new CountDownLatch(1);
        AtomicInteger parked = new AtomicInteger();
        List<Thread> threads = new ArrayList<>();
        for (Vehicle v : vehicles) {
            Thread t = new Thread(() -> {
                try { startGun.await(); } catch (InterruptedException e) { return; }
                if (action.park(v) != null) parked.incrementAndGet();
            });
            threads.add(t);
            t.start();
        }
        startGun.countDown();
        for (Thread t : threads) t.join();
        return parked.get();
    }
}

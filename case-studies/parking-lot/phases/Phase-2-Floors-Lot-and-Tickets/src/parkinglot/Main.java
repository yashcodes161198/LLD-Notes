package parkinglot;

import parkinglot.enums.VehicleType;
import parkinglot.model.*;
import parkinglot.service.ParkingLot;
import parkinglot.utils.DateTimeParser;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== Phase 2: Floors, ParkingLot (Singleton) & Ticket (Builder) ===");
        ParkingLot lot = ParkingLot.getInstance();
        System.out.println("  Same instance everywhere? " + (lot == ParkingLot.getInstance()));

        ParkingFloor floor1 = new ParkingFloor("Floor1");
        floor1.addSpot(new ParkingSpot("F1S1", VehicleType.BIKE));
        floor1.addSpot(new ParkingSpot("F1S2", VehicleType.CAR));
        ParkingFloor floor2 = new ParkingFloor("Floor2");
        floor2.addSpot(new ParkingSpot("F2S1", VehicleType.CAR));
        lot.addFloor(floor1);
        lot.addFloor(floor2);

        Ticket t1 = lot.parkVehicle(new Car("KA01AB1234"), DateTimeParser.parse("21 May 7:30 AM 2025"));
        Ticket t2 = lot.parkVehicle(new Car("KA02CD5678"), DateTimeParser.parse("21 May 7:45 AM 2025"));
        lot.parkVehicle(new Car("KA03EF0000"), DateTimeParser.parse("21 May 8:00 AM 2025"));

        lot.printStatus();
        lot.unparkVehicle(t1.getTicketId(), DateTimeParser.parse("21 May 1:15 PM 2025"));
        lot.unparkVehicle("T-bogus", DateTimeParser.parse("21 May 1:15 PM 2025"));
        lot.printStatus();
    }
}

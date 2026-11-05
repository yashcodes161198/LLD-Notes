package parkinglot;

import parkinglot.enums.PricingStrategyType;
import parkinglot.enums.VehicleType;
import parkinglot.factory.PricingStrategyFactory;
import parkinglot.factory.VehicleFactory;
import parkinglot.model.*;
import parkinglot.service.ParkingLot;
import parkinglot.utils.DateTimeParser;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== Phase 4: Pricing Strategy ===");
        ParkingLot lot = ParkingLot.getInstance();
        EntryGate entryGate = new EntryGate("EG1");
        ExitGate exitGate = new ExitGate("XG1");

        ParkingFloor floor1 = new ParkingFloor("Floor1");
        floor1.addSpot(new ParkingSpot("F1S1", VehicleType.CAR));
        lot.addFloor(floor1);

        for (PricingStrategyType type : PricingStrategyType.values()) {
            lot.setPricingStrategy(PricingStrategyFactory.get(type)); // swap behaviour at runtime
            Vehicle car = VehicleFactory.create("KA01AB1234", VehicleType.CAR);
            Ticket t = entryGate.parkVehicle(car, DateTimeParser.parse("21 May 7:30 AM 2025"));
            exitGate.unparkVehicle(t.getTicketId(), DateTimeParser.parse("21 May 1:15 PM 2025"));
        }
    }
}

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

public class Main {
    public static void main(String[] args) {
        System.out.println("=== Phase 5: Payment Strategy (full flow) ===");
        ParkingLot lot = ParkingLot.getInstance();
        EntryGate entryGate = new EntryGate("EG1");
        ExitGate exitGate = new ExitGate("XG1");
        lot.setPricingStrategy(PricingStrategyFactory.get(PricingStrategyType.TIME_BASED));

        ParkingFloor floor1 = new ParkingFloor("Floor1");
        floor1.addSpot(new ParkingSpot("F1S1", VehicleType.BIKE));
        floor1.addSpot(new ParkingSpot("F1S2", VehicleType.CAR));
        floor1.addSpot(new ParkingSpot("F1S3", VehicleType.TRUCK));
        floor1.addSpot(new ParkingSpot("F1S4", VehicleType.CAR));
        lot.addFloor(floor1);

        Vehicle car = VehicleFactory.create("KA01AB1234", VehicleType.CAR);
        LocalDateTime entryTime = DateTimeParser.parse("21 May 7:30 AM 2025");
        Ticket ticket = entryGate.parkVehicle(car, entryTime);
        System.out.println("  Payment status at entry: " + ticket.getPaymentStatus());

        System.out.println("--------------------------");
        lot.printStatus();
        System.out.println("--------------------------");

        LocalDateTime exitTime = DateTimeParser.parse("21 May 1:15 PM 2025");
        exitGate.unparkVehicle(ticket.getTicketId(), exitTime, PaymentMode.UPI);
        System.out.println("  Payment status at exit: " + ticket.getPaymentStatus());

        System.out.println("--------------------------");
        lot.printStatus();
    }
}

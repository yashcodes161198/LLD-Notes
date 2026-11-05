package parkinglot;

import parkinglot.enums.VehicleType;
import parkinglot.factory.VehicleFactory;
import parkinglot.model.*;
import parkinglot.service.ParkingLot;
import parkinglot.utils.DateTimeParser;

// Main plays the "Client" from the UML: it only talks to gates and the factory.
public class Main {
    public static void main(String[] args) {
        System.out.println("=== Phase 3: VehicleFactory & Entry/Exit Gates ===");
        ParkingLot lot = ParkingLot.getInstance();
        EntryGate entryGate = new EntryGate("EG1");
        ExitGate exitGate = new ExitGate("XG1");

        ParkingFloor floor1 = new ParkingFloor("Floor1");
        floor1.addSpot(new ParkingSpot("F1S1", VehicleType.BIKE));
        floor1.addSpot(new ParkingSpot("F1S2", VehicleType.CAR));
        floor1.addSpot(new ParkingSpot("F1S3", VehicleType.TRUCK));
        lot.addFloor(floor1);

        Vehicle car = VehicleFactory.create("KA01AB1234", VehicleType.CAR);
        Vehicle truck = VehicleFactory.create("KA09TR0001", VehicleType.TRUCK);

        Ticket carTicket = entryGate.parkVehicle(car, DateTimeParser.parse("21 May 7:30 AM 2025"));
        entryGate.parkVehicle(truck, DateTimeParser.parse("21 May 9:00 AM 2025"));
        lot.printStatus();

        exitGate.unparkVehicle(carTicket.getTicketId(), DateTimeParser.parse("21 May 1:15 PM 2025"));
        lot.printStatus();
    }
}

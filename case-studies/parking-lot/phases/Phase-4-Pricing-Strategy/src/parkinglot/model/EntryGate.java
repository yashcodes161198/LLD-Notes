package parkinglot.model;

import parkinglot.enums.GateType;
import parkinglot.service.ParkingLot;

import java.time.LocalDateTime;

public class EntryGate extends Gate {

    public EntryGate(String id) { super(id); }

    @Override
    public GateType getType() { return GateType.ENTRY; }

    public Ticket parkVehicle(Vehicle vehicle, LocalDateTime entryTime) {
        return ParkingLot.getInstance().parkVehicle(vehicle, entryTime);
    }
}

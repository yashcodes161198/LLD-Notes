package parkinglot.model;

import parkinglot.enums.GateType;
import parkinglot.service.ParkingLot;

import java.time.LocalDateTime;

public class ExitGate extends Gate {

    public ExitGate(String id) { super(id); }

    @Override
    public GateType getType() { return GateType.EXIT; }

    public void unparkVehicle(String ticketId, LocalDateTime exitTime) {
        ParkingLot.getInstance().unparkVehicle(ticketId, exitTime);
    }
}

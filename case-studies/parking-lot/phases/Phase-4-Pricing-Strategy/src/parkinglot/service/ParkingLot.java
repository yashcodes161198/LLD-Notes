package parkinglot.service;

import parkinglot.enums.PricingStrategyType;
import parkinglot.factory.PricingStrategyFactory;
import parkinglot.model.*;
import parkinglot.strategy.pricing.PricingStrategy;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class ParkingLot {
    private static final ParkingLot INSTANCE = new ParkingLot();

    private final Map<String, ParkingFloor> floors = new LinkedHashMap<>();
    private final Map<String, Ticket> activeTickets = new LinkedHashMap<>();
    private PricingStrategy pricingStrategy;

    private ParkingLot() {
        this.pricingStrategy = PricingStrategyFactory.get(PricingStrategyType.TIME_BASED);
    }

    public static ParkingLot getInstance() {
        return INSTANCE;
    }

    public void addFloor(ParkingFloor floor) {
        floors.put(floor.getId(), floor);
    }

    public void setPricingStrategy(PricingStrategy pricingStrategy) {
        this.pricingStrategy = pricingStrategy;
    }

    public Ticket parkVehicle(Vehicle vehicle, LocalDateTime entryTime) {
        for (ParkingFloor floor : floors.values()) {
            ParkingSpot spot = floor.findAvailableSpot(vehicle.getType());
            if (spot != null) {
                String ticketId = "T-" + UUID.randomUUID().toString().substring(0, 8);
                Ticket ticket = Ticket.builder()
                        .ticketId(ticketId)
                        .entryTime(entryTime)
                        .vehicle(vehicle)
                        .floorId(floor.getId())
                        .spotId(spot.getId())
                        .build();
                activeTickets.put(ticketId, ticket);
                System.out.println("  " + vehicle + " parked at " + floor.getId() + "/" + spot.getId()
                        + ". Ticket: " + ticketId);
                return ticket;
            }
        }
        System.out.println("  No spot available for vehicle type: " + vehicle.getType());
        return null;
    }

    public void unparkVehicle(String ticketId, LocalDateTime exitTime) {
        Ticket ticket = activeTickets.get(ticketId);
        if (ticket == null) {
            System.out.println("  Invalid ticket ID.");
            return;
        }
        double fee = pricingStrategy.calculateFee(
                ticket.getVehicle().getType(), ticket.getEntryTime(), exitTime);

        ParkingSpot spot = floors.get(ticket.getFloorId()).getSpots().get(ticket.getSpotId());
        spot.vacate();
        activeTickets.remove(ticketId);
        System.out.println("  " + ticket.getVehicle() + " exited. Fee: Rs." + fee
                + " (" + pricingStrategy.getClass().getSimpleName() + ")");
    }

    public void printStatus() {
        floors.forEach((floorId, floor) -> {
            System.out.println("  Floor: " + floorId);
            floor.getSpots().values().forEach(spot ->
                    System.out.println("    Spot " + spot.getId() + " [" + spot.getAllowedType() + "] - "
                            + (spot.isOccupied() ? "Occupied" : "Free")));
        });
    }

    public Map<String, Ticket> getActiveTickets() { return activeTickets; }
}

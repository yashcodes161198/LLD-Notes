package amazonlocker.hardware;

// The seam to the physical locker. Doors auto-close after ~30s (assumption agreed in the interview).
public interface DoorActuator {
    void open(String compartmentId);
}

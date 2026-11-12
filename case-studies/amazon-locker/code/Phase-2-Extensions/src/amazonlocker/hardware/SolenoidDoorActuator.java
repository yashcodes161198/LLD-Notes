package amazonlocker.hardware;

// Drives the door latch of each compartment.
public class SolenoidDoorActuator implements DoorActuator {
    @Override
    public void open(String compartmentId) {
        // stub: real code would pulse the latch on the controller board for this compartment and throw on a hardware fault.
        System.out.println("[DOOR] open " + compartmentId);
    }
}

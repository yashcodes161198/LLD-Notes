package amazonlocker.locker;

import amazonlocker.model.Size;

public class NoAvailableCompartmentException extends RuntimeException {
    public NoAvailableCompartmentException(Size size) {
        super("no available compartment of size " + size);
    }
}

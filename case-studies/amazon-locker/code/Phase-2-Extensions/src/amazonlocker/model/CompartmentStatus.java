package amazonlocker.model;

// EXT 2 + 3: the occupied boolean became an enum once there were more than two states.
public enum CompartmentStatus { AVAILABLE, RESERVED, OCCUPIED, OUT_OF_SERVICE }

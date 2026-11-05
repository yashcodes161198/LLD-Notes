package parkinglot.model;

import parkinglot.enums.GateType;

public abstract class Gate {
    protected final String id;

    protected Gate(String id) { this.id = id; }

    public String getId() { return id; }

    public abstract GateType getType();
}

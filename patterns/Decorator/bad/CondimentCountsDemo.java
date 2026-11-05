// From the repository root: java patterns/Decorator/bad/CondimentCountsDemo.java
public class CondimentCountsDemo {
    public static void main(String[] args) {
        Beverage order = new DarkRoast(0, 2, 0);
        System.out.printf("%s with double mocha: $%.2f%n", order.getDescription(), order.getCost());
    }
}

abstract class Beverage {
    private final int whipCount;
    private final int mochaCount;
    private final int syrupCount;

    protected Beverage(int whipCount, int mochaCount, int syrupCount) {
        this.whipCount = whipCount;
        this.mochaCount = mochaCount;
        this.syrupCount = syrupCount;
    }

    public abstract String getDescription();

    protected abstract double baseCost();

    public double getCost() {
        return baseCost() + whipCount * 2.00 + mochaCount * 5.00 + syrupCount * 1.00;
    }
}

class DarkRoast extends Beverage {
    public DarkRoast(int whipCount, int mochaCount, int syrupCount) {
        super(whipCount, mochaCount, syrupCount);
    }

    @Override
    public String getDescription() {
        return "Dark Roast";
    }

    @Override
    protected double baseCost() {
        return 10.00;
    }
}

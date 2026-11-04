// From LLD: java Decorator/good/DecoratorDemo.java
public class DecoratorDemo {
    public static void main(String[] args) {
        Beverage order = new DarkRoast();
        order = new Mocha(order);
        order = new Mocha(order);
        print(order);
        print(new Whip(order));

        Beverage anotherOrder = new Soy(new Espresso());
        print(anotherOrder);
    }

    private static void print(Beverage beverage) {
        System.out.printf("%s: $%.2f%n", beverage.getDescription(), beverage.getCost());
    }
}

abstract class Beverage {
    public abstract String getDescription();
    public abstract double getCost();
}

class DarkRoast extends Beverage {
    @Override
    public String getDescription() {
        return "Dark Roast";
    }

    @Override
    public double getCost() {
        return 10.00;
    }
}

class Espresso extends Beverage {
    @Override
    public String getDescription() {
        return "Espresso";
    }

    @Override
    public double getCost() {
        return 8.00;
    }
}

abstract class CondimentDecorator extends Beverage {
    protected final Beverage beverage;

    protected CondimentDecorator(Beverage beverage) {
        this.beverage = java.util.Objects.requireNonNull(beverage);
    }
}

class Mocha extends CondimentDecorator {
    public Mocha(Beverage beverage) {
        super(beverage);
    }

    @Override
    public String getDescription() {
        return beverage.getDescription() + ", Mocha";
    }

    @Override
    public double getCost() {
        return beverage.getCost() + 5.00;
    }
}

class Whip extends CondimentDecorator {
    public Whip(Beverage beverage) {
        super(beverage);
    }

    @Override
    public String getDescription() {
        return beverage.getDescription() + ", Whip";
    }

    @Override
    public double getCost() {
        return beverage.getCost() + 2.00;
    }
}

class Soy extends CondimentDecorator {
    public Soy(Beverage beverage) {
        super(beverage);
    }

    @Override
    public String getDescription() {
        return beverage.getDescription() + ", Soy";
    }

    @Override
    public double getCost() {
        return beverage.getCost() + 3.00;
    }
}

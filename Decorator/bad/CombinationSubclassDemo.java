// From LLD: java Decorator/bad/CombinationSubclassDemo.java
public class CombinationSubclassDemo {
    public static void main(String[] args) {
        Beverage plain = new DarkRoast();
        Beverage withMocha = new DarkRoastWithMocha();
        Beverage withDoubleMocha = new DarkRoastWithDoubleMocha();
        Beverage withSoyAndMocha = new DarkRoastWithSoyAndMocha();

        print(plain);
        print(withMocha);
        print(withDoubleMocha);
        print(withSoyAndMocha);
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

class DarkRoastWithMocha extends Beverage {
    @Override
    public String getDescription() {
        return "Dark Roast, Mocha";
    }

    @Override
    public double getCost() {
        return 10.00 + 5.00;
    }
}

class DarkRoastWithDoubleMocha extends Beverage {
    @Override
    public String getDescription() {
        return "Dark Roast, Mocha, Mocha";
    }

    @Override
    public double getCost() {
        return 10.00 + 5.00 + 5.00;
    }
}

class DarkRoastWithSoyAndMocha extends Beverage {
    @Override
    public String getDescription() {
        return "Dark Roast, Soy, Mocha";
    }

    @Override
    public double getCost() {
        return 10.00 + 3.00 + 5.00;
    }
}

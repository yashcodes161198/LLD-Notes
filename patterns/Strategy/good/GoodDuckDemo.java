// Run from the repository root: java patterns/Strategy/good/GoodDuckDemo.java
public class GoodDuckDemo {
    public static void main(String[] args) {
        Duck mallard = new MallardDuck();
        Duck rubber = new RubberDuck();

        mallard.performFly();
        mallard.performQuack();
        rubber.performFly();
        rubber.performQuack();

        rubber.setFlyBehavior(new FlyRocketPowered());
        rubber.performFly();
    }
}

interface FlyBehavior {
    void fly();
}

class FlyWithWings implements FlyBehavior {
    @Override
    public void fly() {
        System.out.println("Flying with wings");
    }
}

class FlyNoWay implements FlyBehavior {
    @Override
    public void fly() {
        System.out.println("I can't fly");
    }
}

class FlyRocketPowered implements FlyBehavior {
    @Override
    public void fly() {
        System.out.println("Flying with a rocket");
    }
}

interface QuackBehavior {
    void quack();
}

class RealQuack implements QuackBehavior {
    @Override
    public void quack() {
        System.out.println("Quack");
    }
}

class Squeak implements QuackBehavior {
    @Override
    public void quack() {
        System.out.println("Squeak");
    }
}

abstract class Duck {
    private FlyBehavior flyBehavior;
    private QuackBehavior quackBehavior;

    protected Duck(FlyBehavior flyBehavior, QuackBehavior quackBehavior) {
        setFlyBehavior(flyBehavior);
        setQuackBehavior(quackBehavior);
    }

    public void performFly() {
        flyBehavior.fly();
    }

    public void performQuack() {
        quackBehavior.quack();
    }

    public void setFlyBehavior(FlyBehavior flyBehavior) {
        this.flyBehavior = java.util.Objects.requireNonNull(flyBehavior);
    }

    public void setQuackBehavior(QuackBehavior quackBehavior) {
        this.quackBehavior = java.util.Objects.requireNonNull(quackBehavior);
    }

    public void swim() {
        System.out.println("All ducks float");
    }

    public abstract void display();
}

class MallardDuck extends Duck {
    public MallardDuck() {
        super(new FlyWithWings(), new RealQuack());
    }

    @Override
    public void display() {
        System.out.println("I'm a mallard duck");
    }
}

class RubberDuck extends Duck {
    public RubberDuck() {
        super(new FlyNoWay(), new Squeak());
    }

    @Override
    public void display() {
        System.out.println("I'm a rubber duck");
    }
}

// Run from the LLD folder: java Strategy/bad/BadDuckDemo.java
public class BadDuckDemo {
    public static void main(String[] args) {
        Duck mallard = new MallardDuck();
        Duck rubber = new RubberDuck();
        Duck decoy = new DecoyDuck();

        mallard.fly();
        rubber.fly();
        decoy.fly();
    }
}

abstract class Duck {
    // Adding this to Duck accidentally gives every duck the ability to fly.
    public void fly() {
        System.out.println("Flying with wings");
    }
}

class MallardDuck extends Duck {
}

class RubberDuck extends Duck {
    @Override
    public void fly() {
        System.out.println("I can't fly");
    }
}

class DecoyDuck extends Duck {
    @Override
    public void fly() {
        System.out.println("I can't fly");
    }
}

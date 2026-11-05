# Strategy pattern: ducks

From the repository root, run each example with Java 11 or newer:

```powershell
java patterns/Strategy/bad/BadDuckDemo.java
java patterns/Strategy/good/GoodDuckDemo.java
```

`bad` puts `fly()` in the base `Duck`. Every duck inherits it, so each duck that cannot fly must override it. The repeated `"I can't fly"` overrides in `RubberDuck` and `DecoyDuck` show the maintenance problem. It also gives flying ducks only one shared flight implementation.

`good` makes flying and quacking separate behaviors. Each concrete duck supplies its initial behaviors to the `Duck` constructor. `performFly()` and `performQuack()` delegate to those objects. The setters allow a behavior to change while the program runs; the demo gives the rubber duck a rocket flight behavior.

The base class has no universal flying or quacking default: a rubber duck and a mallard need different starting behaviors. A subclass chooses behaviors; it does not override `fly()` or `quack()` on `Duck`. Shared behavior such as `swim()` stays in `Duck`, while the duck-specific appearance stays in `display()`.

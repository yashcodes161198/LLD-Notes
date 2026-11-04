# Decorator pattern: coffee orders

Run these standalone examples from the `LLD` folder with Java 11 or newer:

```powershell
java Decorator/bad/CondimentCountsDemo.java
java Decorator/bad/CombinationSubclassDemo.java
java Decorator/good/DecoratorDemo.java
```

## The two awkward designs

In [CondimentCountsDemo.java](bad/CondimentCountsDemo.java), each `Beverage` stores a count for every condiment. `DarkRoast` supplies its own base price, while the inherited `getCost()` adds the condiments. An order for two mochas is `new DarkRoast(0, 2, 0)`. Counts work better than booleans for repeated condiments, but the three positional arguments are easy to mix up. Adding a condiment means changing the base class's fields, constructor, and pricing method, plus every concrete beverage constructor and every order call site.

In [CombinationSubclassDemo.java](bad/CombinationSubclassDemo.java), each combination gets its own subclass. `DarkRoastWithMocha` and `DarkRoastWithDoubleMocha` are distinct classes. Even if each of `c` condiments can only be present or absent, `d` drink types can require up to `d * 2^c` classes to cover every combination. Allowing arbitrary counts makes the number of combinations unbounded. Repeated inheritance also makes the order of additions awkward and duplicates pricing rules.

## Decorator design

In [DecoratorDemo.java](good/DecoratorDemo.java), `Beverage` is the component type. `DarkRoast` and `Espresso` are concrete components. `CondimentDecorator` **extends `Beverage`** and **holds a `Beverage`**. `Mocha`, `Whip`, and `Soy` are concrete decorators. The common type lets a decorator wrap a plain drink or another decorated drink.

```java
Beverage order = new DarkRoast();
order = new Mocha(order);
order = new Mocha(order); // double mocha
order = new Whip(order);
```

Each decorator asks its **wrapped beverage** for its description and cost, then adds its own part. For example, `Mocha.getCost()` returns `beverage.getCost() + 5.00`. Calling `this.getCost()` there would call `Mocha.getCost()` again forever. The abstract `CondimentDecorator` stores the wrapped beverage; it does not need a separate description or price field. A concrete decorator builds the full description from `beverage.getDescription()`.

This makes adding a new condiment a new decorator class, while existing beverage and condiment classes stay as they are. The examples use simple `double` prices to match the book's teaching example; real payment code should use an exact money representation.

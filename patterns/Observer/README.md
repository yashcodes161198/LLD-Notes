# Observer pattern: Weather-O-Rama

Run the three standalone Java examples from the repository root (Java 11 or newer):

```powershell
java patterns/Observer/bad/BadObserverDemo.java
java patterns/Observer/good/push/PushObserverDemo.java
java patterns/Observer/good/pull/PullObserverDemo.java
```

## Problem

`WeatherData` holds temperature, humidity, and wind speed. Displays need to respond when a measurement changes. These examples use the three measurements you described.

The [bad example](bad/BadObserverDemo.java) names every display inside `WeatherData` and calls each one directly. Adding or removing a display means editing `WeatherData`.

## Roles

| Role | In this example | Responsibility |
| --- | --- | --- |
| Subject interface | `Subject` | Add, remove, and notify observers. |
| Concrete subject | `WeatherData` | Store measurements and an observer list. |
| Observer interface | `Observer` | Define the notification method, `update(...)`. |
| Concrete observers | `WeatherDisplay9000`, `WindAlertDisplay` | React to notifications. |
| Display interface | `DisplayElement` | Define `display()` for displaying output. |

`WeatherDisplay9000` implements both `Observer` and `DisplayElement`: receiving an update and showing it are distinct jobs. It receives a **subject** in its constructor and registers **itself** with `subject.addObserver(this)`. It does not receive an observer as a constructor argument.

## Push version

In [PushObserverDemo.java](good/push/PushObserverDemo.java), the flow is:

1. `setMeasurements(...)` saves the new values. If none changed, it returns.
2. `measurementsChanged()` calls `notifyObservers()`.
3. `notifyObservers()` calls each observer's `update(temperature, humidity, windSpeed)`.
4. Each display stores or uses the values it needs, then calls `display()`.

The subject pushes all three measurements to every observer, even if one only needs wind speed. An observer can unsubscribe via `removeObserver`.

## Pull version

In [PullObserverDemo.java](good/pull/PullObserverDemo.java), `Observer.update()` takes **no measurement arguments**. The observer already holds a `WeatherData` reference from its constructor, so it can call getters such as `weatherData.getTemperature()`.

This example also defers the read: `update()` only marks the display as needing refresh. When `display()` is called, it pulls the latest values. Two weather changes before one display call produce one read of the latest state. Pull changes **who chooses which values to read**; observers are still notified of every change unless you add another mechanism to avoid those notifications.

## Naming and design details

- Call the subject method `notifyObservers()`, not `notifyAll()`: `notifyAll()` already has a different meaning in Java's `Object` class.
- `measurementsChanged()` should notify after values are updated. The assignments belong in `setMeasurements(...)`; assigning `this.temperature = this.temperature` would do nothing.
- The subject knows only the `Observer` interface, so adding a new display does not require changing `WeatherData`.

// From the repository root: java patterns/Observer/good/push/PushObserverDemo.java
package push;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PushObserverDemo {
    public static void main(String[] args) {
        WeatherData weatherData = new WeatherData();
        new WeatherDisplay9000(weatherData);
        WindAlertDisplay windAlert = new WindAlertDisplay(weatherData);

        weatherData.setMeasurements(24, 60, 12);
        weatherData.removeObserver(windAlert);
        weatherData.setMeasurements(25, 58, 15);
    }
}

interface Subject {
    void addObserver(Observer observer);
    void removeObserver(Observer observer);
    void notifyObservers();
}

interface Observer {
    void update(float temperature, float humidity, float windSpeed);
}

interface DisplayElement {
    void display();
}

class WeatherData implements Subject {
    private final List<Observer> observers;
    private float temperature;
    private float humidity;
    private float windSpeed;

    public WeatherData() {
        observers = new ArrayList<>();
    }

    @Override
    public void addObserver(Observer observer) {
        Objects.requireNonNull(observer);
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    @Override
    public void removeObserver(Observer observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers() {
        for (Observer observer : List.copyOf(observers)) {
            observer.update(temperature, humidity, windSpeed);
        }
    }

    public void setMeasurements(float temperature, float humidity, float windSpeed) {
        if (Float.compare(this.temperature, temperature) == 0
                && Float.compare(this.humidity, humidity) == 0
                && Float.compare(this.windSpeed, windSpeed) == 0) {
            return;
        }
        this.temperature = temperature;
        this.humidity = humidity;
        this.windSpeed = windSpeed;
        measurementsChanged();
    }

    private void measurementsChanged() {
        notifyObservers();
    }
}

class WeatherDisplay9000 implements Observer, DisplayElement {
    private float temperature;
    private float humidity;
    private float windSpeed;

    public WeatherDisplay9000(Subject subject) {
        subject.addObserver(this);
    }

    @Override
    public void update(float temperature, float humidity, float windSpeed) {
        this.temperature = temperature;
        this.humidity = humidity;
        this.windSpeed = windSpeed;
        display();
    }

    @Override
    public void display() {
        System.out.println("Weather Display 9000: " + temperature + " C, "
                + humidity + "% humidity, " + windSpeed + " km/h wind");
    }
}

class WindAlertDisplay implements Observer, DisplayElement {
    private float windSpeed;

    public WindAlertDisplay(Subject subject) {
        subject.addObserver(this);
    }

    @Override
    public void update(float temperature, float humidity, float windSpeed) {
        this.windSpeed = windSpeed;
        display();
    }

    @Override
    public void display() {
        System.out.println("Wind alert: " + windSpeed + " km/h");
    }
}

// From LLD: java Observer/good/pull/PullObserverDemo.java
package pull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PullObserverDemo {
    public static void main(String[] args) {
        WeatherData weatherData = new WeatherData();
        WeatherDisplay9000 display = new WeatherDisplay9000(weatherData);

        weatherData.setMeasurements(24, 60, 12);
        weatherData.setMeasurements(25, 58, 15);
        display.display(); // Reads the latest measurements once, when needed.
    }
}

interface Subject {
    void addObserver(Observer observer);
    void removeObserver(Observer observer);
    void notifyObservers();
}

interface Observer {
    void update();
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
            observer.update();
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

    public float getTemperature() {
        return temperature;
    }

    public float getHumidity() {
        return humidity;
    }

    public float getWindSpeed() {
        return windSpeed;
    }
}

class WeatherDisplay9000 implements Observer, DisplayElement {
    private final WeatherData weatherData;
    private boolean needsRefresh = true;
    private float temperature;
    private float humidity;
    private float windSpeed;

    public WeatherDisplay9000(WeatherData weatherData) {
        this.weatherData = Objects.requireNonNull(weatherData);
        weatherData.addObserver(this);
    }

    @Override
    public void update() {
        needsRefresh = true;
    }

    @Override
    public void display() {
        if (needsRefresh) {
            temperature = weatherData.getTemperature();
            humidity = weatherData.getHumidity();
            windSpeed = weatherData.getWindSpeed();
            needsRefresh = false;
            System.out.println("Pulled latest measurements");
        }
        System.out.println("Weather Display 9000: " + temperature + " C, "
                + humidity + "% humidity, " + windSpeed + " km/h wind");
    }
}

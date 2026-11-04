// From LLD: java Observer/bad/BadObserverDemo.java
package bad;

public class BadObserverDemo {
    public static void main(String[] args) {
        WeatherData weatherData = new WeatherData();
        weatherData.setMeasurements(24, 60, 12);
    }
}

class WeatherData {
    private float temperature;
    private float humidity;
    private float windSpeed;
    private final WeatherDisplay9000 display = new WeatherDisplay9000();
    private final WindAlertDisplay windAlert = new WindAlertDisplay();

    public void setMeasurements(float temperature, float humidity, float windSpeed) {
        this.temperature = temperature;
        this.humidity = humidity;
        this.windSpeed = windSpeed;
        measurementsChanged();
    }

    private void measurementsChanged() {
        // Every new display requires another field and another call here.
        display.update(temperature, humidity, windSpeed);
        windAlert.update(temperature, humidity, windSpeed);
    }
}

class WeatherDisplay9000 {
    public void update(float temperature, float humidity, float windSpeed) {
        System.out.println("Weather Display 9000: " + temperature + " C, "
                + humidity + "% humidity, " + windSpeed + " km/h wind");
    }
}

class WindAlertDisplay {
    public void update(float temperature, float humidity, float windSpeed) {
        System.out.println("Wind alert: " + windSpeed + " km/h");
    }
}

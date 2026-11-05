package bookmyshow.model;

public class Movie {
    private final String id;
    private final String title;
    private final int durationMin;
    public Movie(String id, String title, int durationMin) {
        this.id = id;
        this.title = title;
        this.durationMin = durationMin;
    }
    public String getId() { return id; }
    public String getTitle() { return title; }
    public int getDurationMin() { return durationMin; }
}

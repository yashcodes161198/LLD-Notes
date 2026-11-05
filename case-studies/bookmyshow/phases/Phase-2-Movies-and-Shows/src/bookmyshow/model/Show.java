package bookmyshow.model;

import java.util.*;

public class Show {
    private final String id;
    private final Movie movie;
    private final Date start;
    private final Date end;
    private final Screen screen;
    private final Theatre theatre;
    public Show(String id, Movie movie, Date start,
                Screen screen, Theatre theatre) {
        this.id = id;
        this.movie = movie;
        this.start = new Date(start.getTime());
        this.end = new Date(start.getTime()
            + movie.getDurationMin() * 60_000L);
        this.screen = screen;
        this.theatre = theatre;
    }
    public String getId() { return id; }
    public Movie getMovie() { return movie; }
    public Date getStart() { return new Date(start.getTime()); }
    public Date getEnd() { return new Date(end.getTime()); }
    public Screen getScreen() { return screen; }
    public Theatre getTheatre() { return theatre; }
    public List<Seat> getSeats() { return screen.getSeats(); }
}

package bookmyshow.service;

import bookmyshow.model.*;
import bookmyshow.repository.TheatreRepository;

public class TheatreService {
    private final TheatreRepository theatreRepo;
    public TheatreService(TheatreRepository theatreRepo) {
        this.theatreRepo = theatreRepo;
    }
    public Theatre createTheatre(String id, String name) {
        Theatre theatre = new Theatre(id, name);
        theatreRepo.save(theatre);
        return theatre;
    }
    public void addScreen(String theatreId, Screen screen) {
        theatreRepo.get(theatreId).addScreen(screen);
    }
    public void addSeat(String theatreId, String screenId, Seat seat) {
        theatreRepo.get(theatreId).getScreen(screenId).addSeat(seat);
    }
}

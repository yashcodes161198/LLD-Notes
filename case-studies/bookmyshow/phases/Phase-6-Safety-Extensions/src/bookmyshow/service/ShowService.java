package bookmyshow.service;

import bookmyshow.model.*;
import bookmyshow.repository.*;
import java.util.*;

public class ShowService {
    private final ShowRepository showRepo;
    private final TheatreRepository theatreRepo;
    private final MovieRepository movieRepo;
    public ShowService(ShowRepository showRepo,
                       TheatreRepository theatreRepo,
                       MovieRepository movieRepo) {
        this.showRepo = showRepo;
        this.theatreRepo = theatreRepo;
        this.movieRepo = movieRepo;
    }
    public Show createShow(String showId, String movieId,
                           String theatreId, String screenId,
                           Date start) {
        Theatre theatre = theatreRepo.get(theatreId);
        Show show = new Show(showId, movieRepo.get(movieId), start,
            theatre.getScreen(screenId), theatre);
        showRepo.save(show);
        return show;
    }
    public Show getShow(String id) { return showRepo.get(id); }
    public List<Show> getShowsByMovieTitle(String title) {
        return showRepo.getAll().stream()
            .filter(s -> s.getMovie().getTitle().equalsIgnoreCase(title))
            .toList();
    }
}

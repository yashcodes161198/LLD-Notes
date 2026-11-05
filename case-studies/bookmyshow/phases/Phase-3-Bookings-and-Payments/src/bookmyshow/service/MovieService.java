package bookmyshow.service;

import bookmyshow.model.Movie;
import bookmyshow.repository.MovieRepository;

public class MovieService {
    private final MovieRepository movieRepo;
    public MovieService(MovieRepository movieRepo) {
        this.movieRepo = movieRepo;
    }
    public Movie createMovie(String id, String title, int durationMin) {
        Movie movie = new Movie(id, title, durationMin);
        movieRepo.save(movie);
        return movie;
    }
    public Movie getMovie(String id) { return movieRepo.get(id); }
}

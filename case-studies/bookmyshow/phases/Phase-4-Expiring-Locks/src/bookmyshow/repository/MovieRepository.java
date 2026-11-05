package bookmyshow.repository;

import bookmyshow.model.Movie;
import java.util.*;

public class MovieRepository {
    private final Map<String, Movie> map = new LinkedHashMap<>();
    public void save(Movie value) { map.put(value.getId(), value); }
    public Movie get(String id) { return map.get(id); }
    public List<Movie> getAll() {
        return new ArrayList<>(map.values());
    }
}

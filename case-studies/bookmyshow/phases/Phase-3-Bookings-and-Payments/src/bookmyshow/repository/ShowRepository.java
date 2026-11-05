package bookmyshow.repository;

import bookmyshow.model.Show;
import java.util.*;

public class ShowRepository {
    private final Map<String, Show> map = new LinkedHashMap<>();
    public void save(Show value) { map.put(value.getId(), value); }
    public Show get(String id) { return map.get(id); }
    public List<Show> getAll() {
        return new ArrayList<>(map.values());
    }
}

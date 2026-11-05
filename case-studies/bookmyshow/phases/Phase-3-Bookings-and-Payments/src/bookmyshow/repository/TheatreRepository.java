package bookmyshow.repository;

import bookmyshow.model.Theatre;
import java.util.*;

public class TheatreRepository {
    private final Map<String, Theatre> map = new LinkedHashMap<>();
    public void save(Theatre value) { map.put(value.getId(), value); }
    public Theatre get(String id) { return map.get(id); }
    public List<Theatre> getAll() {
        return new ArrayList<>(map.values());
    }
}

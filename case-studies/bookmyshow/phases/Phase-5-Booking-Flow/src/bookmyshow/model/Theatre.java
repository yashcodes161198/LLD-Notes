package bookmyshow.model;

import java.util.*;

public class Theatre {
    private final String id;
    private final String name;
    private final Map<String, Screen> screens = new LinkedHashMap<>();
    public Theatre(String id, String name) {
        this.id = id;
        this.name = name;
    }
    public String getId() { return id; }
    public String getName() { return name; }
    public void addScreen(Screen screen) {
        screens.put(screen.getId(), screen);
    }
    public Screen getScreen(String id) { return screens.get(id); }
}

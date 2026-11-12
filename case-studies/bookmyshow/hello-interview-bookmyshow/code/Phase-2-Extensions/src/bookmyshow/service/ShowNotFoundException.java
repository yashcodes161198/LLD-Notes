package bookmyshow.service;

public class ShowNotFoundException extends RuntimeException {
    public ShowNotFoundException(String showId) {
        super("show not found: " + showId);
    }
}

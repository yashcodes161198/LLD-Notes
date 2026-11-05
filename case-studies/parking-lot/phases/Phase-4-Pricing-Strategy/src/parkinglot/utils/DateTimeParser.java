package parkinglot.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DateTimeParser {
    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("d MMM h:mm a yyyy", Locale.ENGLISH);

    public static LocalDateTime parse(String input) {
        return LocalDateTime.parse(input, FORMAT);
    }
}

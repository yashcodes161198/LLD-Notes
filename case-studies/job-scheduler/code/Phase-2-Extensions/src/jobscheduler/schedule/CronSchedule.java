package jobscheduler.schedule;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// EXT 1: a cron subset, in UTC: "minute hour * * day-of-week", e.g. "0 9 * * MON-FRI".
// Each field is *, a number, a range a-b, or a comma list. Day-of-week: 0-6 or SUN..SAT.
// shortcut: no day-of-month/month fields, steps (*/5) or time zones; use a cron library when those are needed.
public final class CronSchedule implements Schedule {
    private static final Map<String, Integer> DAYS =
            Map.of("SUN", 0, "MON", 1, "TUE", 2, "WED", 3, "THU", 4, "FRI", 5, "SAT", 6);
    private static final int SEARCH_LIMIT_MINUTES = 8 * 24 * 60; // every valid expression matches within a week

    private final String expression;
    private final Set<Integer> minutes;
    private final Set<Integer> hours;
    private final Set<Integer> daysOfWeek;
    private final Instant first;

    public CronSchedule(String expression, Instant notBefore) {
        if (expression == null || notBefore == null) throw new IllegalArgumentException("invalid cron schedule");
        String[] f = expression.trim().split("\\s+");
        if (f.length != 5) throw new IllegalArgumentException("cron needs 5 fields: " + expression);
        if (!f[2].equals("*") || !f[3].equals("*")) {
            throw new IllegalArgumentException("only minute, hour and day-of-week are supported: " + expression);
        }
        this.expression = expression;
        this.minutes = parse(f[0], 0, 59, Map.of());
        this.hours = parse(f[1], 0, 23, Map.of());
        this.daysOfWeek = parse(f[4], 0, 6, DAYS);
        Instant minute = notBefore.truncatedTo(ChronoUnit.MINUTES);
        this.first = firstMatchFrom(minute.equals(notBefore) ? minute : minute.plus(1, ChronoUnit.MINUTES)); // at or after
    }

    @Override public Instant firstSlot() { return first; }

    @Override
    public Instant nextSlotAfter(Instant slot, Instant now) {
        Instant from = slot.isAfter(now) ? slot : now;
        return firstMatchFrom(from.truncatedTo(ChronoUnit.MINUTES).plus(1, ChronoUnit.MINUTES));
    }

    // Minute-by-minute scan: at most ~11.5k steps, simpler than field arithmetic.
    private Instant firstMatchFrom(Instant start) {
        ZonedDateTime t = start.atZone(ZoneOffset.UTC);
        for (int i = 0; i < SEARCH_LIMIT_MINUTES; i++, t = t.plusMinutes(1)) {
            if (minutes.contains(t.getMinute()) && hours.contains(t.getHour())
                    && daysOfWeek.contains(t.getDayOfWeek().getValue() % 7)) {
                return t.toInstant();
            }
        }
        throw new IllegalStateException("no match for " + expression); // unreachable for a parsed expression
    }

    private static Set<Integer> parse(String field, int min, int max, Map<String, Integer> names) {
        Set<Integer> values = new HashSet<>();
        for (String part : field.split(",")) {
            if (part.equals("*")) {
                for (int v = min; v <= max; v++) values.add(v);
                continue;
            }
            String[] range = part.split("-");
            if (range.length > 2) throw new IllegalArgumentException("bad cron field: " + field);
            int lo = value(range[0], names);
            int hi = range.length == 2 ? value(range[1], names) : lo;
            if (lo < min || hi > max || lo > hi) throw new IllegalArgumentException("bad cron field: " + field);
            for (int v = lo; v <= hi; v++) values.add(v);
        }
        return values;
    }

    private static int value(String token, Map<String, Integer> names) {
        Integer named = names.get(token.toUpperCase());
        return named != null ? named : Integer.parseInt(token); // NumberFormatException is an IllegalArgumentException
    }

    @Override
    public String toString() { return "cron(" + expression + ")"; }
}

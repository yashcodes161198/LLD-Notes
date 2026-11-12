package jobscheduler.store;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jobscheduler.model.Job;

// Stub for a jobs table. A real store writes a row (status, scheduledFor, nextRunAt, attempts) and must be durable
// before save() returns. Here the map holds the live objects, which is what a write-through store would contain,
// so it survives a "crash" of the Scheduler object in Main.
public class InMemoryJobStore implements JobStore {
    private final Map<String, Job> rows = new ConcurrentHashMap<>();

    @Override
    public void save(Job job) {
        rows.put(job.getId(), job);
    }

    @Override
    public Collection<Job> loadAll() {
        return List.copyOf(rows.values());
    }
}

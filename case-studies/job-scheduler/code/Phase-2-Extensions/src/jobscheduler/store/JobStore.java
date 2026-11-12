package jobscheduler.store;

import java.util.Collection;

import jobscheduler.model.Job;

// EXT 3: the seam to durable storage. The scheduler saves a job at every state change;
// a restarted scheduler loads them all and calls recover(now).
public interface JobStore {
    void save(Job job);

    Collection<Job> loadAll();
}

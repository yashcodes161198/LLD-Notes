package jobscheduler.scheduler;

// Req 4, 5: cancel / getStatus with an unknown id.
public class JobNotFoundException extends RuntimeException {
    public JobNotFoundException(String jobId) {
        super("job not found: " + jobId);
    }
}

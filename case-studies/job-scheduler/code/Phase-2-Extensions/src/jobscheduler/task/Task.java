package jobscheduler.task;

// The seam to the client's code. Implementations are out of scope.
// Throwing any RuntimeException means "this attempt failed". The key is the same for every retry of one run.
public interface Task {
    void run(String executionKey);
}

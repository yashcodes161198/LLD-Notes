package jobscheduler.task;

// Rebuilds the report for its slot. Idempotent (it overwrites the report stored under the key), so AT_LEAST_ONCE is safe.
public class NightlyReportTask implements Task {
    private final String reportName;

    public NightlyReportTask(String reportName) {
        if (reportName == null || reportName.isBlank()) throw new IllegalArgumentException("invalid report name");
        this.reportName = reportName;
    }

    @Override
    public void run(String executionKey) {
        // stub: real code would query the data and upsert the report row keyed by executionKey; a DB error should throw.
        System.out.println("[REPORT] rebuilt " + reportName + " (" + executionKey + ")");
    }
}

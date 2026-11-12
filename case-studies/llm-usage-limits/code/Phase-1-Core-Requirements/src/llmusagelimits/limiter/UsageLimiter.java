package llmusagelimits.limiter;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import llmusagelimits.backend.InferenceBackend;
import llmusagelimits.model.InferenceRequest;
import llmusagelimits.model.InferenceResult;
import llmusagelimits.model.LimitType;
import llmusagelimits.model.Model;
import llmusagelimits.model.UsageAccount;
import llmusagelimits.model.UsageWindow;

// Orchestrator. handle() = estimate -> reserve (atomic) -> run (no lock) -> settle (finally).
public class UsageLimiter {
    public static final long EXPECTED_REPLY_TOKENS = 100; // "typical reply" used by the estimate (Req 3)

    private final Map<String, UsageAccount> accounts = new ConcurrentHashMap<>();
    private final InferenceBackend backend;
    private final Clock clock;
    private final long fiveHourLimit;
    private final long weeklyLimit;

    public UsageLimiter(InferenceBackend backend, Clock clock, long fiveHourLimit, long weeklyLimit) {
        if (backend == null || clock == null) throw new IllegalArgumentException("backend and clock required");
        if (fiveHourLimit <= 0 || weeklyLimit <= 0) throw new IllegalArgumentException("limits must be positive");
        this.backend = backend;
        this.clock = clock;
        this.fiveHourLimit = fiveHourLimit;
        this.weeklyLimit = weeklyLimit;
    }

    // Req 1, 3, 5.
    public InferenceResult handle(InferenceRequest request) {
        if (request == null) throw new IllegalArgumentException("invalid request");
        UsageAccount account = accounts.computeIfAbsent(request.getUserId(),
                id -> new UsageAccount(fiveHourLimit, weeklyLimit));
        long estimate = estimateUnits(request);

        UsageWindow blocking = account.tryReserve(estimate, clock.instant());
        if (blocking != null) throw new UsageLimitExceededException(blocking.getType(), blocking.getResetAt());

        long actual = 0;
        try {
            InferenceResult result = backend.generate(request.getModel(), request.getMessages()); // outside any lock
            actual = actualUnits(request.getModel(), result);
            return result;
        } finally {
            account.settle(estimate, actual, clock.instant()); // always: a failure must not leak the reservation (TRICKY #2)
        }
    }

    // Req 6. A user we've never seen has the full limit; a read doesn't create an account.
    public long getRemaining(String userId, LimitType type) {
        if (type == null) throw new IllegalArgumentException("invalid limit type");
        UsageAccount account = accounts.get(userId);
        if (account == null) return type == LimitType.FIVE_HOUR ? fiveHourLimit : weeklyLimit;
        return account.remaining(type, clock.instant());
    }

    // Units held by in-flight requests (Req 5).
    public long getReserved(String userId, LimitType type) {
        UsageAccount account = accounts.get(userId);
        return account == null ? 0 : account.reserved(type);
    }

    // Req 3: rough input count (chars / 4) + a typical reply, times the model weight.
    private long estimateUnits(InferenceRequest request) {
        long inputTokens = (request.totalChars() + 3) / 4;
        return (inputTokens + EXPECTED_REPLY_TOKENS) * request.getModel().getWeight();
    }

    // Req 2.
    private long actualUnits(Model model, InferenceResult result) {
        return result.totalTokens() * model.getWeight();
    }
}

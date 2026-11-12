package llmusagelimits.limiter;

import java.time.Clock;
import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import llmusagelimits.backend.InferenceBackend;
import llmusagelimits.estimate.FixedEstimator;
import llmusagelimits.estimate.UsageEstimator;
import llmusagelimits.model.InferenceRequest;
import llmusagelimits.model.InferenceResult;
import llmusagelimits.model.LimitType;
import llmusagelimits.model.Model;
import llmusagelimits.model.UsageAccount;
import llmusagelimits.model.UsageWindow;
import llmusagelimits.model.WindowSpec;
import llmusagelimits.plan.Plan;

// Orchestrator. handle() = enter -> estimate -> reserve (atomic) -> run (no lock) -> settle (finally) -> leave.
public class UsageLimiter {
    public static final long EXPECTED_REPLY_TOKENS = 100; // default "typical reply" (Req 3)

    private final Map<String, UsageAccount> accounts = new ConcurrentHashMap<>();
    private final InferenceBackend backend;
    private final Clock clock;
    private final List<WindowSpec> specs;
    private final UsageEstimator estimator;
    private final int maxInFlight;

    // Phase 1 API: 5-hour + weekly, fixed estimate, no concurrency cap.
    public UsageLimiter(InferenceBackend backend, Clock clock, long fiveHourLimit, long weeklyLimit) {
        this(backend, clock,
                List.of(WindowSpec.allModels(LimitType.FIVE_HOUR, Duration.ofHours(5), fiveHourLimit),
                        WindowSpec.allModels(LimitType.WEEKLY, Duration.ofDays(7), weeklyLimit)),
                new FixedEstimator(EXPECTED_REPLY_TOKENS), Integer.MAX_VALUE);
    }

    public UsageLimiter(InferenceBackend backend, Clock clock, List<WindowSpec> specs,
                        UsageEstimator estimator, int maxInFlight) {
        if (backend == null || clock == null || estimator == null) {
            throw new IllegalArgumentException("backend, clock and estimator required");
        }
        if (specs == null || specs.isEmpty()) throw new IllegalArgumentException("at least one window required");
        Set<LimitType> seen = EnumSet.noneOf(LimitType.class);
        for (WindowSpec spec : specs) {
            if (spec == null || !seen.add(spec.getType())) throw new IllegalArgumentException("duplicate or null window");
        }
        if (maxInFlight <= 0) throw new IllegalArgumentException("maxInFlight must be positive");
        this.backend = backend;
        this.clock = clock;
        this.specs = List.copyOf(specs);
        this.estimator = estimator;
        this.maxInFlight = maxInFlight;
    }

    // Req 1, 3, 5 + EXT 2, 3, 4.
    public InferenceResult handle(InferenceRequest request) {
        if (request == null) throw new IllegalArgumentException("invalid request");
        String userId = request.getUserId();
        Model model = request.getModel();
        UsageAccount account = accountFor(userId);

        if (!account.tryEnter(maxInFlight)) throw new TooManyConcurrentRequestsException(userId, maxInFlight);
        try {
            long estimate = estimateUnits(request);
            UsageWindow blocking = account.tryReserve(model, estimate, clock.instant());
            if (blocking != null) throw new UsageLimitExceededException(blocking.getType(), blocking.getResetAt());

            long actual = 0;
            try {
                InferenceResult result = backend.generate(model, request.getMessages()); // outside any lock
                actual = actualUnits(model, result);
                estimator.record(userId, model, result.getOutputTokens()); // EXT 2
                return result;
            } finally {
                account.settle(model, estimate, actual, clock.instant()); // TRICKY #2
            }
        } finally {
            account.leave(); // also on a limit rejection, or the cap leaks
        }
    }

    // EXT 1: changes the limits, keeps what was used.
    public void setPlan(String userId, Plan plan) {
        if (userId == null || userId.isBlank() || plan == null) throw new IllegalArgumentException("invalid plan change");
        accountFor(userId).applyPlan(plan.getMultiplier());
    }

    // Req 6. A user we've never seen has the full base limit; a read doesn't create an account.
    public long getRemaining(String userId, LimitType type) {
        WindowSpec spec = spec(type);
        UsageAccount account = accounts.get(userId);
        if (account == null) return spec.getBaseLimit() * Plan.FREE.getMultiplier();
        return account.remaining(type, clock.instant());
    }

    // Units held by in-flight requests (Req 5).
    public long getReserved(String userId, LimitType type) {
        spec(type);
        UsageAccount account = accounts.get(userId);
        return account == null ? 0 : account.reserved(type);
    }

    private UsageAccount accountFor(String userId) {
        return accounts.computeIfAbsent(userId, id -> new UsageAccount(specs, Plan.FREE.getMultiplier()));
    }

    private WindowSpec spec(LimitType type) {
        for (WindowSpec spec : specs) {
            if (spec.getType() == type) return spec;
        }
        throw new IllegalArgumentException("no window configured for " + type);
    }

    // Req 3: rough input count (chars / 4) + the estimator's reply size, times the model weight.
    private long estimateUnits(InferenceRequest request) {
        long inputTokens = (request.totalChars() + 3) / 4;
        long reply = estimator.expectedReplyTokens(request.getUserId(), request.getModel());
        return (inputTokens + reply) * request.getModel().getWeight();
    }

    // Req 2.
    private long actualUnits(Model model, InferenceResult result) {
        return result.totalTokens() * model.getWeight();
    }
}

package llmusagelimits.limiter;

import java.time.Instant;

import llmusagelimits.model.LimitType;

// Req 4: names which limit was hit and when it resets.
public class UsageLimitExceededException extends RuntimeException {
    private final LimitType limitType;
    private final Instant resetAt;

    public UsageLimitExceededException(LimitType limitType, Instant resetAt) {
        super(limitType + " limit reached, resets at " + resetAt);
        this.limitType = limitType;
        this.resetAt = resetAt;
    }

    public LimitType getLimitType() { return limitType; }
    public Instant getResetAt() { return resetAt; }
}

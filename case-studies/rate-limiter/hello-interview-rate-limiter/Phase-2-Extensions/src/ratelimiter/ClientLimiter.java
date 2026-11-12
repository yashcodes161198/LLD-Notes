package ratelimiter;

// EXT 1 (Strategy): the one method RateLimiter ever calls. Each algorithm implements it.
// Implementations must make tryConsume atomic for their own client (Req 5).
public interface ClientLimiter {
    Decision tryConsume(long nowNanos);
}

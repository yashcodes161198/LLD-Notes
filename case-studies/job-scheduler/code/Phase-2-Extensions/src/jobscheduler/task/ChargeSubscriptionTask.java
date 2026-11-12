package jobscheduler.task;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

// EXT 3: exactly-once EFFECT. Schedule it AT_LEAST_ONCE and pass the execution key to the payment provider as its
// idempotency key, so a rerun of the same slot (after a crash or a retry) charges nothing.
public class ChargeSubscriptionTask implements Task {
    private final String customerId;
    private final long amountCents;
    private final Set<String> processedKeys = ConcurrentHashMap.newKeySet(); // stub: the provider's idempotency-key table
    private final AtomicInteger charges = new AtomicInteger();

    public ChargeSubscriptionTask(String customerId, long amountCents) {
        if (customerId == null || customerId.isBlank() || amountCents <= 0) throw new IllegalArgumentException("invalid charge");
        this.customerId = customerId;
        this.amountCents = amountCents;
    }

    @Override
    public void run(String executionKey) {
        // stub: real code would call the provider with Idempotency-Key = executionKey; a provider error should throw.
        if (!processedKeys.add(executionKey)) {
            System.out.println("[PAYMENT] " + executionKey + " already charged, ignored");
            return;
        }
        charges.incrementAndGet();
        System.out.println("[PAYMENT] charged " + customerId + " " + amountCents + "c (" + executionKey + ")");
    }

    public int getChargeCount() { return charges.get(); }
}

package jobscheduler.model;

// Req 3. AT_MOST_ONCE: never run twice, a failure is final. AT_LEAST_ONCE: retried, so the task must be idempotent.
public enum DeliveryGuarantee { AT_MOST_ONCE, AT_LEAST_ONCE }

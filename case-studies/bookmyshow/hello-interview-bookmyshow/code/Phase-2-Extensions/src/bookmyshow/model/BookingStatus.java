package bookmyshow.model;

// No EXPIRED: expiry is derived from PENDING + expiresAt (lazy expiry, Req 4).
public enum BookingStatus { PENDING, CONFIRMED, CANCELLED }

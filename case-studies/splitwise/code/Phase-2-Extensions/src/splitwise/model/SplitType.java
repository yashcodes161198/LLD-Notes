package splitwise.model;

// How an expense is divided. Values: EQUAL = none, EXACT = cents, PERCENT = basis points (10000 = 100%),
// SHARES = weights, e.g. 2:1:1 (EXT 1).
public enum SplitType { EQUAL, EXACT, PERCENT, SHARES }

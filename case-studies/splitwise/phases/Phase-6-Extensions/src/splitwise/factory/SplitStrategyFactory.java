package splitwise.factory;

import splitwise.enums.SplitType;
import splitwise.strategy.EqualSplitStrategy;
import splitwise.strategy.ExactSplitStrategy;
import splitwise.strategy.PercentageSplitStrategy;
import splitwise.strategy.SplitStrategy;

public class SplitStrategyFactory {
    public static SplitStrategy getStrategy(SplitType splitType) {
        return switch (splitType) {
            case EQUAL -> new EqualSplitStrategy();
            case PERCENTAGE -> new PercentageSplitStrategy();
            case EXACT -> new ExactSplitStrategy();
        };
    }
}

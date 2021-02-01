package com.github.echohlne.strategy;

import java.io.Serializable;
import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Predicate;

public final class StrategyGenerator implements Serializable {
    private static final long DEFAULT_EXPONENTIAL_LIMIT = 1000L;
    public static Function<Integer, Long> waitExponentialAttempts(double backoffBase) {
        return maxAttempts -> maxAttempts > 0 ? Math.min(DEFAULT_EXPONENTIAL_LIMIT, Math.round(Math.pow(backoffBase, maxAttempts))) : 0L;
    }

    public static Function<Integer, Long> waitConstantlyAttempts(long delay) {
        return custom -> delay;
    }

    public static Function<Integer, Long> waitExponentialAttempts() {
        return StrategyGenerator.waitExponentialAttempts(2);
    }

    public static Predicate<Integer> stopAfterMaxAttempts(int maxAttempts) {
        return usedAttempts -> usedAttempts >= maxAttempts;
    }

    public static Predicate<Exception> retryOnException(Class<? extends Throwable>... includeExceptions) {
        return occurredException -> Arrays.stream(includeExceptions).anyMatch(includeException ->
            // todo occurredException.isInstance(includeException)
            includeException.isInstance(occurredException));
    }

    public static Predicate<Object> retryOnResult(Object... needRetryObjects) {
        return returnedResult -> Arrays.stream(needRetryObjects).anyMatch(needRetryObject ->
            needRetryObject.equals(returnedResult));
    }
}

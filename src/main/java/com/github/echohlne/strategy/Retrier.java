package com.github.echohlne.strategy;

import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Predicate;

public final class Retrier implements Serializable {
    private Predicate<Exception> failedRetryStrategy;
    private Predicate<Object> resultRetryStrategy;
    private Predicate<Integer> stopStrategy;
    private Function<Integer, Long> waitStrategy;

    public Retrier(Builder builder) {
        this.failedRetryStrategy = builder.failedRetryStrategy;
        this.resultRetryStrategy = builder.resultRetryStrategy;
        this.stopStrategy = builder.stopStrategy;
        this.waitStrategy = builder.waitStrategy;
    }

    // todo 两个疑问， 可不可以传参; 没有返回值怎么办
    public <T> T execute(Callable<T> callable) throws Exception {
        int usedAttempts = 0;
        boolean shouldRetry, attemptFailed = false, interrupted;

        T executedResult = null;
        Exception occurredException = null;

        do {
            try {
                usedAttempts++;
                occurredException = null;
                attemptFailed = false;

                executedResult = callable.call();
                attemptFailed = this.resultRetryStrategy.test(executedResult);
            } catch (Exception exception) {
                attemptFailed = this.failedRetryStrategy.test(exception);
                occurredException = exception;
            } finally {
                interrupted = Thread.interrupted() || isInterruptedException(occurredException);
                shouldRetry = !interrupted && attemptFailed && !this.stopStrategy.test(usedAttempts);
                if (shouldRetry) {
                    // todo 怎么算的，有点奇怪
                    long waitMillis = this.waitStrategy.apply(usedAttempts);
                    if (waitMillis > 0) {
                        try {
                            Thread.sleep(waitMillis);
                        } catch (InterruptedException e) {
                            shouldRetry = false;
                            interrupted = true;
                        }
                    }
                }
            }
        } while (shouldRetry);

        if (interrupted) {
            Thread.currentThread().interrupt();
        }

        if (attemptFailed && !interrupted) {
            if (occurredException != null) {
                throw occurredException;
            }
            return executedResult;
        }
        if (occurredException != null) {
            throw occurredException;
        }
        return executedResult;
    }

    private boolean isInterruptedException(Exception occurredException) {
        Throwable currentException = occurredException;
        while(currentException != null && !(currentException instanceof InterruptedException)) {
            currentException = currentException.getCause();
        }
        return currentException != null;
    }

    public static class Builder {
        private Predicate<Exception> failedRetryStrategy = e -> true;
        private Predicate<Object> resultRetryStrategy = result -> false;
        private Predicate<Integer> stopStrategy = usedAttempt -> false;
        private Function<Integer, Long> waitStrategy = attempt -> 0L;

        public Builder withFailedRetryStrategy(Predicate<Exception> failedRetryStrategy) {
            this.failedRetryStrategy = Objects.requireNonNull(failedRetryStrategy);
            return this;
        }

        public Builder withFailedRetryStrategy(Class... failedExceptions) {
            this.failedRetryStrategy = Objects.requireNonNull(StrategyGenerator.retryOnException(failedExceptions));
            return this;
        }

        public Builder withResultRetryStrategy(Predicate<Object> resultRetryStrategy) {
            this.resultRetryStrategy = Objects.requireNonNull(resultRetryStrategy);
            return this;
        }

        public Builder withResultRetryStrategy(Object... needRetryObject) {
            this.resultRetryStrategy = Objects.requireNonNull(StrategyGenerator.retryOnResult(needRetryObject));
            return this;
        }

        public Builder withStopStrategy(Predicate<Integer> stopStrategy) {
            this.stopStrategy = Objects.requireNonNull(stopStrategy);
            return this;
        }

        public Builder withStopStrategy(int maxAttempts) {
            if (maxAttempts < 0) {
                throw new IllegalArgumentException("maxAttempts must be a number >= 0, but got " + maxAttempts);
            }
            this.stopStrategy = StrategyGenerator.stopAfterMaxAttempts(maxAttempts);
            return this;
        }

        public Builder withWaitStrategy(Function<Integer, Long> waitStrategy) {
            this.waitStrategy = Objects.requireNonNull(waitStrategy);
            return this;
        }

        public Retrier build() {
            return new Retrier(this);
        }
    }
}

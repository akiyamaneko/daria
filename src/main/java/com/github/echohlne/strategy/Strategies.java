package com.github.echohlne.strategy;

import java.io.Serializable;
import java.util.function.Function;

/**
 * Consumer<T> => 接收T对象，不返回值
 * Predicate<T> => 接收T对象并返回boolean
 * Function<T, R> => 接收T对象，返回R对象
 * Supplier<T> => 提供T对象(类似工厂) 不接收值
 * UnaryOperator<T> =>接收T对象，返回T对象
 * BinaryOperator<T> => 接收2个T对象，返回T对象
 */
public final class Strategies implements Serializable {
    public static Function<Integer, Long> waitExponential(double backoffTimes) {
        return attempts -> {
            if(attempts > 0) {
                double backoffMillis = Math.pow(backoffTimes, attempts);
                return Math.min(1000L, Math.round(backoffMillis));
            }
            return 0L;
        };
    }
}

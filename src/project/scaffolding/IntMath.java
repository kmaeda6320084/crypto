package project.scaffolding;

import java.math.BigInteger;

import project.test.scaffolding.benchmark.BenchmarkServer;

public final class IntMath {
    private IntMath() {
    }

    public static int ceil(int num, int div) {
        return (num + div - 1) / div;
    }

    // return {d, u, v} that u*m + v*n = d
    public static BigInteger[] extendedEuclidean(BigInteger m, BigInteger n) {
        BenchmarkServer.enter();
        final var result = new BigInteger[3];
        extendedEuclideanImpl(result, m, n);
        BenchmarkServer.leave();
        return result;
    }

    private static void extendedEuclideanImpl(BigInteger[] result, BigInteger m, BigInteger n) {
        BenchmarkServer.increment();
        if (n.equals(BigInteger.ZERO)) {
            result[0] = m;
            result[1] = BigInteger.ONE;
            result[2] = BigInteger.ZERO;
        } else {
            extendedEuclideanImpl(result, n, m.mod(n));
            final var u = result[1];
            final var v = result[2];
            result[1] = v;
            result[2] = u.subtract(m.divide(n).multiply(v));
        }
    }
}
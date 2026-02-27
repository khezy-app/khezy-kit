package io.github.khezyapp.doa;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.openjdk.jmh.annotations.*;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Fork(1)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class AccessorBenchmark {
    private Level1 root;
    private String path = "l2.l3.l4.l5.value";

    // Reflection methods for manual chain
    private Method getL2;
    private Method getL3;
    private Method getL4;
    private Method getL5;
    private Method getValue;

    @Setup
    public void setup() throws Exception {
        root = new Level1(new Level2(new Level3(new Level4(new Level5("TargetValue")))));

        // Prepare reflection chain
        getL2 = Level1.class.getMethod("getL2");
        getL3 = Level2.class.getMethod("getL3");
        getL4 = Level3.class.getMethod("getL4");
        getL5 = Level4.class.getMethod("getL5");
        getValue = Level5.class.getMethod("getValue");

        // Warm up DynamicObjects
        DynamicObjects.get(root, path);
    }

    @Benchmark
    public String testDirectAccess() {
        return root.getL2().getL3().getL4().getL5().getValue();
    }

    @Benchmark
    public Object testStandardReflection() throws Exception {
        Object l2 = getL2.invoke(root);
        Object l3 = getL3.invoke(l2);
        Object l4 = getL4.invoke(l3);
        Object l5 = getL5.invoke(l4);
        return getValue.invoke(l5);
    }

    @Benchmark
    public Object testDynamicObjects() {
        return DynamicObjects.get(root, path);
    }

    // Deep Domain Models
    @Getter
    @AllArgsConstructor
    public static class Level1 {
        private Level2 l2;
    }

    @Getter
    @AllArgsConstructor
    public static class Level2 {
        private Level3 l3;
    }

    @Getter
    @AllArgsConstructor
    public static class Level3 {
        private Level4 l4;
    }

    @Getter
    @AllArgsConstructor
    public static class Level4 {
        private Level5 l5;
    }

    @Getter
    @AllArgsConstructor
    public static class Level5 {
        private String value;
    }
}

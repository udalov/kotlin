// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY
// FULL_JDK

// FILE: SFunction.java
import java.io.Serializable;
import java.util.function.Function;

@FunctionalInterface
public interface SFunction<T, R> extends Function<T, R>, Serializable {}

// FILE: J.java
import java.lang.invoke.*;
import java.lang.reflect.Method;

public class J {
    public String foo() { return null; }

    public static String extractMethodName(SFunction<J, ?> func) {
        try {
            Method method = func.getClass().getDeclaredMethod("writeReplace");
            method.setAccessible(true);
            return ((SerializedLambda) method.invoke(func)).getImplMethodName();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}

// FILE: box.kt
import kotlin.test.assertEquals

fun box(): String {
    assertEquals("foo", J.extractMethodName(J::foo))

    return "OK"
}

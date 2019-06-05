// SKIP_IN_RUNTIME_TEST

package test;

public class Throws {
    static class Nested extends Exception {}

    public Throws() throws Error, AssertionError {}

    void throwsNested() throws Nested {}

    <E extends RuntimeException & Cloneable> void throwsGenericRuntimeException() throws E {}

    <F extends Throwable, G extends F> void throwsGenericThrowable() throws G {}
}

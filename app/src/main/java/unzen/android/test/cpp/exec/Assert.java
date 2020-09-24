package unzen.android.test.cpp.exec;

public class Assert {

    static public void fail(String message) {
        if (message == null) {
            throw new AssertionError();
        }
        throw new AssertionError(message);
    }

    static public void assertTrue(boolean condition, String message) {
        if (!condition) {
            fail(message);
        }
    }

    static public void assertTrue(boolean condition) {
        assertTrue(condition, null);
    }

    static public void assertFalse(boolean condition, String message) {
        assertTrue(!condition, message);
    }

    static public void assertFalse(boolean condition) {
        assertFalse(condition, null);
    }
}

package unzen.android.test.cpp.exec.cppmodule;

public class CppModule {

    static {
        System.loadLibrary("jnifoo");
    }

    static public native String getStringFromJni();
}

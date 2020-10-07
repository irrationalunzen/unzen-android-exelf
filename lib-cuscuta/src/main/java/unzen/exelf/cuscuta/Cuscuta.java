package unzen.exelf.cuscuta;

public class Cuscuta {

    static {
        System.loadLibrary("jnifoo");
    }

    static public native String getStringFromJni();
}

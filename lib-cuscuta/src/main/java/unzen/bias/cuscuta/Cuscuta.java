package unzen.bias.cuscuta;

public class Cuscuta {

    static {
        System.loadLibrary("jnifoo");
    }

    static public native String getStringFromJni();
}

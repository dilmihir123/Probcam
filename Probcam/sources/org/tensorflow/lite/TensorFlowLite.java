package org.tensorflow.lite;

import java.io.PrintStream;

public final class TensorFlowLite {
    private static final String LIBNAME = "tensorflowlite_jni";

    public static native String version();

    private TensorFlowLite() {
    }

    static boolean init() {
        try {
            System.loadLibrary(LIBNAME);
            return true;
        } catch (UnsatisfiedLinkError e) {
            PrintStream printStream = System.err;
            String str = "TensorFlowLite: failed to load native library: ";
            String valueOf = String.valueOf(e.getMessage());
            printStream.println(valueOf.length() != 0 ? str.concat(valueOf) : new String(str));
            return false;
        }
    }

    static {
        init();
    }
}

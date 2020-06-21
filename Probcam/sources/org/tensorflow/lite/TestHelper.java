package org.tensorflow.lite;

public class TestHelper {
    public static void setUseNNAPI(Interpreter interpreter, boolean useNNAPI) {
        if (interpreter == null || interpreter.wrapper == null) {
            throw new IllegalArgumentException("Interpreter has not initialized; Failed to setUseNNAPI.");
        }
        interpreter.wrapper.setUseNNAPI(useNNAPI);
    }
}

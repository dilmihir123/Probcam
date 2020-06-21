package org.tensorflow.lite;

import java.io.File;
import java.nio.MappedByteBuffer;
import java.util.HashMap;
import java.util.Map;
import javax.validation.constraints.NotNull;

public final class Interpreter implements AutoCloseable {
    NativeInterpreterWrapper wrapper;

    public Interpreter(@NotNull File modelFile) {
        if (modelFile != null) {
            this.wrapper = new NativeInterpreterWrapper(modelFile.getAbsolutePath());
        }
    }

    public Interpreter(@NotNull MappedByteBuffer mappedByteBuffer) {
        this.wrapper = new NativeInterpreterWrapper(mappedByteBuffer);
    }

    public void run(@NotNull Object input, @NotNull Object output) {
        Object[] inputs = {input};
        Map<Integer, Object> outputs = new HashMap<>();
        outputs.put(Integer.valueOf(0), output);
        runForMultipleInputsOutputs(inputs, outputs);
    }

    public void runForMultipleInputsOutputs(@NotNull Object[] inputs, @NotNull Map<Integer, Object> outputs) {
        if (this.wrapper == null) {
            throw new IllegalStateException("The Interpreter has already been closed.");
        }
        Tensor[] tensors = this.wrapper.run(inputs);
        if (outputs == null || tensors == null || outputs.size() > tensors.length) {
            throw new IllegalArgumentException("Outputs do not match with model outputs.");
        }
        int size = tensors.length;
        for (Integer idx : outputs.keySet()) {
            if (idx == null || idx.intValue() < 0 || idx.intValue() >= size) {
                throw new IllegalArgumentException(String.format("Invalid index of output %d (should be in range [0, %d))", new Object[]{idx, Integer.valueOf(size)}));
            }
            tensors[idx.intValue()].copyTo(outputs.get(idx));
        }
    }

    public void resizeInput(int idx, @NotNull int[] dims) {
        if (this.wrapper == null) {
            throw new IllegalStateException("The Interpreter has already been closed.");
        }
        this.wrapper.resizeInput(idx, dims);
    }

    public int getInputIndex(String opName) {
        if (this.wrapper != null) {
            return this.wrapper.getInputIndex(opName);
        }
        throw new IllegalStateException("The Interpreter has already been closed.");
    }

    public int getOutputIndex(String opName) {
        if (this.wrapper != null) {
            return this.wrapper.getOutputIndex(opName);
        }
        throw new IllegalStateException("The Interpreter has already been closed.");
    }

    public void close() {
        this.wrapper.close();
        this.wrapper = null;
    }
}

package org.tensorflow.lite;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.util.HashMap;
import java.util.Map;

final class NativeInterpreterWrapper implements AutoCloseable {
    private static final int ERROR_BUFFER_SIZE = 512;
    private long errorHandle;
    private int inputSize;
    private Map<String, Integer> inputsIndexes;
    private long interpreterHandle;
    private MappedByteBuffer modelByteBuffer;
    private long modelHandle;
    private Map<String, Integer> outputsIndexes;

    private static native long createErrorReporter(int i);

    private static native long createInterpreter(long j);

    private static native long createModel(String str, long j);

    private static native long createModelWithBuffer(MappedByteBuffer mappedByteBuffer, long j);

    private static native void delete(long j, long j2, long j3);

    private static native int[] getInputDims(long j, int i, int i2);

    private static native String[] getInputNames(long j);

    private static native String[] getOutputNames(long j);

    private static native void resizeInput(long j, long j2, int i, int[] iArr);

    private static native long[] run(long j, long j2, Object[] objArr, int[] iArr, int[] iArr2, Object[] objArr2);

    private static native void useNNAPI(long j, boolean z);

    NativeInterpreterWrapper(String modelPath) {
        this.errorHandle = createErrorReporter(512);
        this.modelHandle = createModel(modelPath, this.errorHandle);
        this.interpreterHandle = createInterpreter(this.modelHandle);
    }

    NativeInterpreterWrapper(MappedByteBuffer mappedByteBuffer) {
        this.modelByteBuffer = mappedByteBuffer;
        this.errorHandle = createErrorReporter(512);
        this.modelHandle = createModelWithBuffer(this.modelByteBuffer, this.errorHandle);
        this.interpreterHandle = createInterpreter(this.modelHandle);
    }

    public void close() {
        delete(this.errorHandle, this.modelHandle, this.interpreterHandle);
        this.errorHandle = 0;
        this.modelHandle = 0;
        this.interpreterHandle = 0;
        this.modelByteBuffer = null;
        this.inputsIndexes = null;
        this.outputsIndexes = null;
    }

    /* access modifiers changed from: 0000 */
    public Tensor[] run(Object[] inputs) {
        if (inputs == null || inputs.length == 0) {
            throw new IllegalArgumentException("Invalid inputs. Inputs should not be null or empty.");
        }
        int[] dataTypes = new int[inputs.length];
        Object[] sizes = new Object[inputs.length];
        int[] numsOfBytes = new int[inputs.length];
        for (int i = 0; i < inputs.length; i++) {
            DataType dataType = dataTypeOf(inputs[i]);
            dataTypes[i] = dataType.getNumber();
            if (dataType == DataType.BYTEBUFFER) {
                ByteBuffer buffer = inputs[i];
                if (buffer.order() != ByteOrder.nativeOrder()) {
                    throw new IllegalArgumentException("Invalid ByteBuffer. It shoud use ByteOrder.nativeOrder().");
                }
                numsOfBytes[i] = buffer.limit();
                sizes[i] = getInputDims(this.interpreterHandle, i, numsOfBytes[i]);
            } else if (isNonEmptyArray(inputs[i])) {
                int[] dims = shapeOf(inputs[i]);
                sizes[i] = dims;
                numsOfBytes[i] = dataType.elemByteSize() * numElements(dims);
            } else {
                throw new IllegalArgumentException(String.format("%d-th element of the %d inputs is not an array or a ByteBuffer.", new Object[]{Integer.valueOf(i), Integer.valueOf(inputs.length)}));
            }
        }
        long[] outputsHandles = run(this.interpreterHandle, this.errorHandle, sizes, dataTypes, numsOfBytes, inputs);
        if (outputsHandles == null || outputsHandles.length == 0) {
            throw new IllegalStateException("Interpreter has no outputs.");
        }
        Tensor[] outputs = new Tensor[outputsHandles.length];
        for (int i2 = 0; i2 < outputsHandles.length; i2++) {
            outputs[i2] = Tensor.fromHandle(outputsHandles[i2]);
        }
        return outputs;
    }

    /* access modifiers changed from: 0000 */
    public void resizeInput(int idx, int[] dims) {
        resizeInput(this.interpreterHandle, this.errorHandle, idx, dims);
    }

    /* access modifiers changed from: 0000 */
    public void setUseNNAPI(boolean useNNAPI) {
        useNNAPI(this.interpreterHandle, useNNAPI);
    }

    /* access modifiers changed from: 0000 */
    public int getInputIndex(String name) {
        if (this.inputsIndexes == null) {
            String[] names = getInputNames(this.interpreterHandle);
            this.inputsIndexes = new HashMap();
            if (names != null) {
                for (int i = 0; i < names.length; i++) {
                    this.inputsIndexes.put(names[i], Integer.valueOf(i));
                }
            }
        }
        if (this.inputsIndexes.containsKey(name)) {
            return ((Integer) this.inputsIndexes.get(name)).intValue();
        }
        throw new IllegalArgumentException(String.format("%s is not a valid name for any input. The indexes of the inputs are %s", new Object[]{name, this.inputsIndexes.toString()}));
    }

    /* access modifiers changed from: 0000 */
    public int getOutputIndex(String name) {
        if (this.outputsIndexes == null) {
            String[] names = getOutputNames(this.interpreterHandle);
            this.outputsIndexes = new HashMap();
            if (names != null) {
                for (int i = 0; i < names.length; i++) {
                    this.outputsIndexes.put(names[i], Integer.valueOf(i));
                }
            }
        }
        if (this.outputsIndexes.containsKey(name)) {
            return ((Integer) this.outputsIndexes.get(name)).intValue();
        }
        throw new IllegalArgumentException(String.format("%s is not a valid name for any output. The indexes of the outputs are %s", new Object[]{name, this.outputsIndexes.toString()}));
    }

    static int numElements(int[] shape) {
        if (shape == null) {
            return 0;
        }
        int n = 1;
        for (int i : shape) {
            n *= i;
        }
        return n;
    }

    static boolean isNonEmptyArray(Object o) {
        return (o == null || !o.getClass().isArray() || Array.getLength(o) == 0) ? false : true;
    }

    static DataType dataTypeOf(Object o) {
        if (o != null) {
            Class<?> c = o.getClass();
            while (c.isArray()) {
                c = c.getComponentType();
            }
            if (Float.TYPE.equals(c)) {
                return DataType.FLOAT32;
            }
            if (Integer.TYPE.equals(c)) {
                return DataType.INT32;
            }
            if (Byte.TYPE.equals(c)) {
                return DataType.UINT8;
            }
            if (Long.TYPE.equals(c)) {
                return DataType.INT64;
            }
            if (ByteBuffer.class.isInstance(o)) {
                return DataType.BYTEBUFFER;
            }
        }
        String str = "cannot resolve DataType of ";
        String valueOf = String.valueOf(o.getClass().getName());
        throw new IllegalArgumentException(valueOf.length() != 0 ? str.concat(valueOf) : new String(str));
    }

    static int[] shapeOf(Object o) {
        int[] dimensions = new int[numDimensions(o)];
        fillShape(o, 0, dimensions);
        return dimensions;
    }

    static int numDimensions(Object o) {
        if (o == null || !o.getClass().isArray()) {
            return 0;
        }
        if (Array.getLength(o) != 0) {
            return numDimensions(Array.get(o, 0)) + 1;
        }
        throw new IllegalArgumentException("array lengths cannot be 0.");
    }

    static void fillShape(Object o, int dim, int[] shape) {
        if (shape != null && dim != shape.length) {
            int len = Array.getLength(o);
            if (shape[dim] == 0) {
                shape[dim] = len;
            } else if (shape[dim] != len) {
                throw new IllegalArgumentException(String.format("mismatched lengths (%d and %d) in dimension %d", new Object[]{Integer.valueOf(shape[dim]), Integer.valueOf(len), Integer.valueOf(dim)}));
            }
            for (int i = 0; i < len; i++) {
                fillShape(Array.get(o, i), dim + 1, shape);
            }
        }
    }

    static {
        TensorFlowLite.init();
    }
}

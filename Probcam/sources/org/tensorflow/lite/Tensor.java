package org.tensorflow.lite;

import java.util.Arrays;

final class Tensor {
    final DataType dtype;
    final long nativeHandle;
    final int[] shapeCopy;

    private static native int dtype(long j);

    private static native void readMultiDimensionalArray(long j, Object obj);

    private static native int[] shape(long j);

    static Tensor fromHandle(long nativeHandle2) {
        return new Tensor(nativeHandle2);
    }

    /* access modifiers changed from: 0000 */
    public <T> T copyTo(T dst) {
        if (NativeInterpreterWrapper.dataTypeOf(dst) != this.dtype) {
            throw new IllegalArgumentException(String.format("Cannot convert an TensorFlowLite tensor with type %s to a Java object of type %s (which is compatible with the TensorFlowLite type %s)", new Object[]{this.dtype, dst.getClass().getName(), NativeInterpreterWrapper.dataTypeOf(dst)}));
        }
        int[] dstShape = NativeInterpreterWrapper.shapeOf(dst);
        if (!Arrays.equals(dstShape, this.shapeCopy)) {
            throw new IllegalArgumentException(String.format("Shape of output target %s does not match with the shape of the Tensor %s.", new Object[]{Arrays.toString(dstShape), Arrays.toString(this.shapeCopy)}));
        }
        readMultiDimensionalArray(this.nativeHandle, dst);
        return dst;
    }

    private Tensor(long nativeHandle2) {
        this.nativeHandle = nativeHandle2;
        this.dtype = DataType.fromNumber(dtype(nativeHandle2));
        this.shapeCopy = shape(nativeHandle2);
    }

    static {
        TensorFlowLite.init();
    }
}

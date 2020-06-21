package org.tensorflow.lite;

enum DataType {
    FLOAT32(1),
    INT32(2),
    UINT8(3),
    INT64(4),
    BYTEBUFFER(999);
    
    private static final DataType[] values = null;
    private final int value;

    static {
        values = values();
    }

    private DataType(int value2) {
        this.value = value2;
    }

    /* access modifiers changed from: 0000 */
    public int getNumber() {
        return this.value;
    }

    static DataType fromNumber(int c) {
        DataType[] dataTypeArr;
        for (DataType t : values) {
            if (t.value == c) {
                return t;
            }
        }
        String version = TensorFlowLite.version();
        throw new IllegalArgumentException(new StringBuilder(String.valueOf(version).length() + 57).append("DataType ").append(c).append(" is not recognized in Java (version ").append(version).append(")").toString());
    }

    /* access modifiers changed from: 0000 */
    public int elemByteSize() {
        switch (this) {
            case FLOAT32:
            case INT32:
                return 4;
            case UINT8:
                return 1;
            case INT64:
                return 8;
            case BYTEBUFFER:
                return 1;
            default:
                String valueOf = String.valueOf(this);
                throw new IllegalArgumentException(new StringBuilder(String.valueOf(valueOf).length() + 30).append("DataType ").append(valueOf).append(" is not supported yet").toString());
        }
    }
}

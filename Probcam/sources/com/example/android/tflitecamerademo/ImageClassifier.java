package com.example.android.tflitecamerademo;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.util.Log;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import org.tensorflow.lite.Interpreter;

public class ImageClassifier {
    private static final int DIM_BATCH_SIZE = 1;
    static final int DIM_IMG_SIZE_X = 224;
    static final int DIM_IMG_SIZE_Y = 224;
    private static final int DIM_PIXEL_SIZE = 3;
    private static final String LABEL_PATH = "labels.txt";
    private static final String MODEL_PATH = "mobilenet_quantized.tflite";
    private static final int RESULTS_TO_SHOW = 3;
    private static final String TAG = "TfLiteCameraDemo";
    private ByteBuffer imgData = null;
    private int[] intValues = new int[50176];
    private List<String> labelList;
    private byte[][] labelProbArray = null;
    private PriorityQueue<Entry<String, Float>> sortedLabels = new PriorityQueue<>(3, new Comparator<Entry<String, Float>>(this) {
        public int compare(Entry<String, Float> o1, Entry<String, Float> o2) {
            return ((Float) o1.getValue()).compareTo((Float) o2.getValue());
        }
    });
    private Interpreter tflite;

    ImageClassifier(Activity activity) throws IOException {
        this.tflite = new Interpreter(loadModelFile(activity));
        this.labelList = loadLabelList(activity);
        this.imgData = ByteBuffer.allocateDirect(150528);
        this.imgData.order(ByteOrder.nativeOrder());
        this.labelProbArray = (byte[][]) Array.newInstance(Byte.TYPE, new int[]{1, this.labelList.size()});
        Log.d(TAG, "Created a Tensorflow Lite Image Classifier.");
    }

    /* access modifiers changed from: 0000 */
    public String classifyFrame(Bitmap bitmap) {
        if (this.tflite == null) {
            Log.e(TAG, "Image classifier has not been initialized; Skipped.");
            return "Uninitialized Classifier.";
        }
        convertBitmapToByteBuffer(bitmap);
        long startTime = SystemClock.uptimeMillis();
        this.tflite.run(this.imgData, this.labelProbArray);
        long endTime = SystemClock.uptimeMillis();
        String str = TAG;
        String str2 = "Timecost to run model inference: ";
        String valueOf = String.valueOf(Long.toString(endTime - startTime));
        Log.d(str, valueOf.length() != 0 ? str2.concat(valueOf) : new String(str2));
        String textToShow = printTopKLabels();
        String l = Long.toString(endTime - startTime);
        return new StringBuilder(String.valueOf(l).length() + 2 + String.valueOf(textToShow).length()).append(l).append("ms").append(textToShow).toString();
    }

    public void close() {
        this.tflite.close();
        this.tflite = null;
    }

    private List<String> loadLabelList(Activity activity) throws IOException {
        List<String> labelList2 = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(activity.getAssets().open(LABEL_PATH)));
        while (true) {
            String line = reader.readLine();
            if (line != null) {
                labelList2.add(line);
            } else {
                reader.close();
                return labelList2;
            }
        }
    }

    private MappedByteBuffer loadModelFile(Activity activity) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(MODEL_PATH);
        return new FileInputStream(fileDescriptor.getFileDescriptor()).getChannel().map(MapMode.READ_ONLY, fileDescriptor.getStartOffset(), fileDescriptor.getDeclaredLength());
    }

    private void convertBitmapToByteBuffer(Bitmap bitmap) {
        int pixel;
        if (this.imgData != null) {
            this.imgData.rewind();
            bitmap.getPixels(this.intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
            int pixel2 = 0;
            long startTime = SystemClock.uptimeMillis();
            int i = 0;
            while (i < 224) {
                int j = 0;
                while (true) {
                    pixel = pixel2;
                    if (j >= 224) {
                        break;
                    }
                    pixel2 = pixel + 1;
                    int val = this.intValues[pixel];
                    this.imgData.put((byte) ((val >> 16) & 255));
                    this.imgData.put((byte) ((val >> 8) & 255));
                    this.imgData.put((byte) (val & 255));
                    j++;
                }
                i++;
                pixel2 = pixel;
            }
            long endTime = SystemClock.uptimeMillis();
            String str = TAG;
            String str2 = "Timecost to put values into ByteBuffer: ";
            String valueOf = String.valueOf(Long.toString(endTime - startTime));
            Log.d(str, valueOf.length() != 0 ? str2.concat(valueOf) : new String(str2));
        }
    }

    private String printTopKLabels() {
        for (int i = 0; i < this.labelList.size(); i++) {
            this.sortedLabels.add(new SimpleEntry((String) this.labelList.get(i), Float.valueOf(((float) (this.labelProbArray[0][i] & 255)) / 255.0f)));
            if (this.sortedLabels.size() > 3) {
                this.sortedLabels.poll();
            }
        }
        String textToShow = "";
        int size = this.sortedLabels.size();
        for (int i2 = 0; i2 < size; i2++) {
            Entry<String, Float> label = (Entry) this.sortedLabels.poll();
            String str = (String) label.getKey();
            String f = Float.toString(((Float) label.getValue()).floatValue());
            textToShow = new StringBuilder(String.valueOf(str).length() + 2 + String.valueOf(f).length() + String.valueOf(textToShow).length()).append("\n").append(str).append(":").append(f).append(textToShow).toString();
        }
        return textToShow;
    }
}

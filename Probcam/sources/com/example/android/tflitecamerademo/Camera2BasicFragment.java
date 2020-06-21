package com.example.android.tflitecamerademo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Matrix.ScaleToFit;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCaptureSession.CaptureCallback;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraDevice.StateCallback;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureRequest.Builder;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.p000v4.content.ContextCompat;
import android.support.v13.app.FragmentCompat;
import android.support.v13.app.FragmentCompat.OnRequestPermissionsResultCallback;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.google.devtools.build.android.desugar.runtime.ThrowableExtension;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class Camera2BasicFragment extends Fragment implements OnRequestPermissionsResultCallback {
    static final /* synthetic */ boolean $assertionsDisabled = (!Camera2BasicFragment.class.desiredAssertionStatus());
    private static final String FRAGMENT_DIALOG = "dialog";
    private static final String HANDLE_THREAD_NAME = "CameraBackground";
    private static final int MAX_PREVIEW_HEIGHT = 1080;
    private static final int MAX_PREVIEW_WIDTH = 1920;
    private static final int PERMISSIONS_REQUEST_CODE = 1;
    private static final String TAG = "TfLiteCameraDemo";
    /* access modifiers changed from: private */
    public Handler backgroundHandler;
    private HandlerThread backgroundThread;
    /* access modifiers changed from: private */
    public CameraDevice cameraDevice;
    private String cameraId;
    /* access modifiers changed from: private */
    public Semaphore cameraOpenCloseLock = new Semaphore(1);
    /* access modifiers changed from: private */
    public CaptureCallback captureCallback = new CaptureCallback(this) {
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
        }

        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
        }
    };
    /* access modifiers changed from: private */
    public CameraCaptureSession captureSession;
    private boolean checkedPermissions = false;
    private ImageClassifier classifier;
    private ImageReader imageReader;
    /* access modifiers changed from: private */
    public final Object lock = new Object();
    /* access modifiers changed from: private */
    public Runnable periodicClassify = new Runnable() {
        public void run() {
            synchronized (Camera2BasicFragment.this.lock) {
                if (Camera2BasicFragment.this.runClassifier) {
                    Camera2BasicFragment.this.classifyFrame();
                }
            }
            Camera2BasicFragment.this.backgroundHandler.post(Camera2BasicFragment.this.periodicClassify);
        }
    };
    /* access modifiers changed from: private */
    public CaptureRequest previewRequest;
    /* access modifiers changed from: private */
    public Builder previewRequestBuilder;
    private Size previewSize;
    /* access modifiers changed from: private */
    public boolean runClassifier = false;
    private final StateCallback stateCallback = new StateCallback() {
        public void onOpened(@NonNull CameraDevice currentCameraDevice) {
            Camera2BasicFragment.this.cameraOpenCloseLock.release();
            Camera2BasicFragment.this.cameraDevice = currentCameraDevice;
            Camera2BasicFragment.this.createCameraPreviewSession();
        }

        public void onDisconnected(@NonNull CameraDevice currentCameraDevice) {
            Camera2BasicFragment.this.cameraOpenCloseLock.release();
            currentCameraDevice.close();
            Camera2BasicFragment.this.cameraDevice = null;
        }

        public void onError(@NonNull CameraDevice currentCameraDevice, int error) {
            Camera2BasicFragment.this.cameraOpenCloseLock.release();
            currentCameraDevice.close();
            Camera2BasicFragment.this.cameraDevice = null;
            Activity activity = Camera2BasicFragment.this.getActivity();
            if (activity != null) {
                activity.finish();
            }
        }
    };
    private final SurfaceTextureListener surfaceTextureListener = new SurfaceTextureListener() {
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            Camera2BasicFragment.this.openCamera(width, height);
        }

        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
            Camera2BasicFragment.this.configureTransform(width, height);
        }

        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }
    };
    /* access modifiers changed from: private */
    public TextView textView;
    private AutoFitTextureView textureView;

    private static class CompareSizesByArea implements Comparator<Size> {
        private CompareSizesByArea() {
        }

        public int compare(Size lhs, Size rhs) {
            return Long.signum((((long) lhs.getWidth()) * ((long) lhs.getHeight())) - (((long) rhs.getWidth()) * ((long) rhs.getHeight())));
        }
    }

    public static class ErrorDialog extends DialogFragment {
        private static final String ARG_MESSAGE = "message";

        public static ErrorDialog newInstance(String message) {
            ErrorDialog dialog = new ErrorDialog();
            Bundle args = new Bundle();
            args.putString(ARG_MESSAGE, message);
            dialog.setArguments(args);
            return dialog;
        }

        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();
            return new AlertDialog.Builder(activity).setMessage(getArguments().getString(ARG_MESSAGE)).setPositiveButton(17039370, new OnClickListener(this) {
                public void onClick(DialogInterface dialogInterface, int i) {
                    activity.finish();
                }
            }).create();
        }
    }

    /* access modifiers changed from: private */
    public void showToast(final String text) {
        Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    Camera2BasicFragment.this.textView.setText(text);
                }
            });
        }
    }

    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth, int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {
        List<Size> bigEnough = new ArrayList<>();
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight && option.getHeight() == (option.getWidth() * h) / w) {
                if (option.getWidth() < textureViewWidth || option.getHeight() < textureViewHeight) {
                    notBigEnough.add(option);
                } else {
                    bigEnough.add(option);
                }
            }
        }
        if (bigEnough.size() > 0) {
            return (Size) Collections.min(bigEnough, new CompareSizesByArea());
        }
        if (notBigEnough.size() > 0) {
            return (Size) Collections.max(notBigEnough, new CompareSizesByArea());
        }
        Log.e(TAG, "Couldn't find any suitable preview size");
        return choices[0];
    }

    public static Camera2BasicFragment newInstance() {
        return new Camera2BasicFragment();
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(C0195R.layout.fragment_camera2_basic, container, false);
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        this.textureView = (AutoFitTextureView) view.findViewById(C0195R.C0197id.texture);
        this.textView = (TextView) view.findViewById(C0195R.C0197id.text);
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        try {
            this.classifier = new ImageClassifier(getActivity());
        } catch (IOException e) {
            Log.e(TAG, "Failed to initialize an image classifier.");
        }
        startBackgroundThread();
    }

    public void onResume() {
        super.onResume();
        startBackgroundThread();
        if (this.textureView.isAvailable()) {
            openCamera(this.textureView.getWidth(), this.textureView.getHeight());
        } else {
            this.textureView.setSurfaceTextureListener(this.surfaceTextureListener);
        }
    }

    public void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    public void onDestroy() {
        this.classifier.close();
        super.onDestroy();
    }

    private void setUpCameraOutputs(int width, int height) {
        String[] cameraIdList;
        Activity activity = getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService("camera");
        try {
            for (String cameraId2 : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId2);
                Integer facing = (Integer) characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing == null || facing.intValue() != 0) {
                    StreamConfigurationMap map = (StreamConfigurationMap) characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    if (map != null) {
                        Size largest = (Size) Collections.max(Arrays.asList(map.getOutputSizes(256)), new CompareSizesByArea());
                        this.imageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(), 256, 2);
                        int displayRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
                        int sensorOrientation = ((Integer) characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)).intValue();
                        boolean swappedDimensions = false;
                        switch (displayRotation) {
                            case 0:
                            case 2:
                                if (sensorOrientation == 90 || sensorOrientation == 270) {
                                    swappedDimensions = true;
                                    break;
                                }
                            case 1:
                            case 3:
                                if (sensorOrientation == 0 || sensorOrientation == 180) {
                                    swappedDimensions = true;
                                    break;
                                }
                            default:
                                String str = TAG;
                                StringBuilder sb = new StringBuilder(40);
                                Log.e(str, sb.append("Display rotation is invalid: ").append(displayRotation).toString());
                                break;
                        }
                        Point displaySize = new Point();
                        activity.getWindowManager().getDefaultDisplay().getSize(displaySize);
                        int rotatedPreviewWidth = width;
                        int rotatedPreviewHeight = height;
                        int maxPreviewWidth = displaySize.x;
                        int maxPreviewHeight = displaySize.y;
                        if (swappedDimensions) {
                            rotatedPreviewWidth = height;
                            rotatedPreviewHeight = width;
                            maxPreviewWidth = displaySize.y;
                            maxPreviewHeight = displaySize.x;
                        }
                        if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                            maxPreviewWidth = MAX_PREVIEW_WIDTH;
                        }
                        if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                            maxPreviewHeight = MAX_PREVIEW_HEIGHT;
                        }
                        this.previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth, maxPreviewHeight, largest);
                        if (getResources().getConfiguration().orientation == 2) {
                            this.textureView.setAspectRatio(this.previewSize.getWidth(), this.previewSize.getHeight());
                        } else {
                            this.textureView.setAspectRatio(this.previewSize.getHeight(), this.previewSize.getWidth());
                        }
                        this.cameraId = cameraId2;
                        return;
                    }
                }
            }
        } catch (CameraAccessException e) {
            ThrowableExtension.printStackTrace(e);
        } catch (NullPointerException e2) {
            ErrorDialog.newInstance(getString(C0195R.string.camera_error)).show(getChildFragmentManager(), FRAGMENT_DIALOG);
        }
    }

    private String[] getRequiredPermissions() {
        Activity activity = getActivity();
        try {
            String[] ps = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 4096).requestedPermissions;
            if (ps == null || ps.length <= 0) {
                return new String[0];
            }
            return ps;
        } catch (Exception e) {
            return new String[0];
        }
    }

    /* access modifiers changed from: private */
    public void openCamera(int width, int height) {
        if (this.checkedPermissions || allPermissionsGranted()) {
            this.checkedPermissions = true;
            setUpCameraOutputs(width, height);
            configureTransform(width, height);
            CameraManager manager = (CameraManager) getActivity().getSystemService("camera");
            try {
                if (!this.cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                    throw new RuntimeException("Time out waiting to lock camera opening.");
                }
                manager.openCamera(this.cameraId, this.stateCallback, this.backgroundHandler);
            } catch (CameraAccessException e) {
                ThrowableExtension.printStackTrace(e);
            } catch (InterruptedException e2) {
                throw new RuntimeException("Interrupted while trying to lock camera opening.", e2);
            }
        } else {
            FragmentCompat.requestPermissions(this, getRequiredPermissions(), 1);
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : getRequiredPermissions()) {
            if (ContextCompat.checkSelfPermission(getActivity(), permission) != 0) {
                return false;
            }
        }
        return true;
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void closeCamera() {
        try {
            this.cameraOpenCloseLock.acquire();
            if (this.captureSession != null) {
                this.captureSession.close();
                this.captureSession = null;
            }
            if (this.cameraDevice != null) {
                this.cameraDevice.close();
                this.cameraDevice = null;
            }
            if (this.imageReader != null) {
                this.imageReader.close();
                this.imageReader = null;
            }
            this.cameraOpenCloseLock.release();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } catch (Throwable th) {
            this.cameraOpenCloseLock.release();
            throw th;
        }
    }

    private void startBackgroundThread() {
        this.backgroundThread = new HandlerThread(HANDLE_THREAD_NAME);
        this.backgroundThread.start();
        this.backgroundHandler = new Handler(this.backgroundThread.getLooper());
        synchronized (this.lock) {
            this.runClassifier = true;
        }
        this.backgroundHandler.post(this.periodicClassify);
    }

    private void stopBackgroundThread() {
        this.backgroundThread.quitSafely();
        try {
            this.backgroundThread.join();
            this.backgroundThread = null;
            this.backgroundHandler = null;
            synchronized (this.lock) {
                this.runClassifier = false;
            }
        } catch (InterruptedException e) {
            ThrowableExtension.printStackTrace(e);
        }
    }

    /* access modifiers changed from: private */
    public void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = this.textureView.getSurfaceTexture();
            if ($assertionsDisabled || texture != null) {
                texture.setDefaultBufferSize(this.previewSize.getWidth(), this.previewSize.getHeight());
                Surface surface = new Surface(texture);
                this.previewRequestBuilder = this.cameraDevice.createCaptureRequest(1);
                this.previewRequestBuilder.addTarget(surface);
                this.cameraDevice.createCaptureSession(Arrays.asList(new Surface[]{surface}), new CameraCaptureSession.StateCallback() {
                    public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                        if (Camera2BasicFragment.this.cameraDevice != null) {
                            Camera2BasicFragment.this.captureSession = cameraCaptureSession;
                            try {
                                Camera2BasicFragment.this.previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, Integer.valueOf(4));
                                Camera2BasicFragment.this.previewRequest = Camera2BasicFragment.this.previewRequestBuilder.build();
                                Camera2BasicFragment.this.captureSession.setRepeatingRequest(Camera2BasicFragment.this.previewRequest, Camera2BasicFragment.this.captureCallback, Camera2BasicFragment.this.backgroundHandler);
                            } catch (CameraAccessException e) {
                                ThrowableExtension.printStackTrace(e);
                            }
                        }
                    }

                    public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                        Camera2BasicFragment.this.showToast("Failed");
                    }
                }, null);
                return;
            }
            throw new AssertionError();
        } catch (CameraAccessException e) {
            ThrowableExtension.printStackTrace(e);
        }
    }

    /* access modifiers changed from: private */
    public void configureTransform(int viewWidth, int viewHeight) {
        Activity activity = getActivity();
        if (this.textureView != null && this.previewSize != null && activity != null) {
            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            Matrix matrix = new Matrix();
            RectF viewRect = new RectF(0.0f, 0.0f, (float) viewWidth, (float) viewHeight);
            RectF bufferRect = new RectF(0.0f, 0.0f, (float) this.previewSize.getHeight(), (float) this.previewSize.getWidth());
            float centerX = viewRect.centerX();
            float centerY = viewRect.centerY();
            if (1 == rotation || 3 == rotation) {
                bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
                matrix.setRectToRect(viewRect, bufferRect, ScaleToFit.FILL);
                float scale = Math.max(((float) viewHeight) / ((float) this.previewSize.getHeight()), ((float) viewWidth) / ((float) this.previewSize.getWidth()));
                matrix.postScale(scale, scale, centerX, centerY);
                matrix.postRotate((float) ((rotation - 2) * 90), centerX, centerY);
            } else if (2 == rotation) {
                matrix.postRotate(180.0f, centerX, centerY);
            }
            this.textureView.setTransform(matrix);
        }
    }

    /* access modifiers changed from: private */
    public void classifyFrame() {
        if (this.classifier == null || getActivity() == null || this.cameraDevice == null) {
            showToast("Uninitialized Classifier or invalid context.");
            return;
        }
        Bitmap bitmap = this.textureView.getBitmap(224, 224);
        String textToShow = this.classifier.classifyFrame(bitmap);
        bitmap.recycle();
        showToast(textToShow);
    }
}

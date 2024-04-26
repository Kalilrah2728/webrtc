package com.qifan.webrtcsamples.bhuviFile;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Base64;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import com.qifan.webrtcsamples.R;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

public class VidoCallActivity extends AppCompatActivity implements SurfaceHolder.Callback {


    private static final String TAG = "MainActivity";
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCaptureSession;
    private ImageReader mImageReader;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private WebSocketClient webSocketClient;

    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vido_call);
        startStreaming();

        mSurfaceView = findViewById(R.id.cameraPreview);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);

        Button startLiveButton = findViewById(R.id.startLiveButton);

        startLiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }
    private void startStreaming() {
        try {
            webSocketClient = new WebSocketClient(new URI("ws://192.168.1.147:1998")) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    Log.d(TAG, "WebSocket connection opened");
                }

                @Override
                public void onMessage(String message) {
                    Log.d(TAG, "Received message from server: " + message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    Log.d(TAG, "WebSocket connection closed");
                }

                @Override
                public void onError(Exception ex) {
                    Log.e(TAG, "WebSocket error: " + ex.getMessage());
                }
            };
            webSocketClient.connect();

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        openFrontCamera();
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        //closeCamera();
    }

    private void openFrontCamera() {
        CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    manager.openCamera(cameraId, new CameraDevice.StateCallback() {
                        @Override
                        public void onOpened(@NonNull CameraDevice camera) {
                            mCameraDevice = camera;
                            createCameraPreview();
                        }

                        @Override
                        public void onDisconnected(@NonNull CameraDevice camera) {
                            mCameraDevice.close();
                        }

                        @Override
                        public void onError(@NonNull CameraDevice camera, int error) {
                            mCameraDevice.close();
                            mCameraDevice = null;
                        }
                    }, null);
                    break; // Stop after finding the front camera
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private void createCameraPreview() {
        try {
            Surface surface = mSurfaceHolder.getSurface();
            mImageReader = ImageReader.newInstance(mSurfaceView.getWidth(), mSurfaceView.getHeight(), ImageFormat.JPEG, 1);
            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    if (mCameraDevice == null) {
                        return;
                    }

                    mCaptureSession = session;
                    try {
                        mCaptureSession.setRepeatingRequest(createCaptureRequest(), new CameraCaptureSession.CaptureCallback() {
                            @Override
                            public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                                super.onCaptureCompleted(session, request, result);
                                // Extract image data from the ImageReader and send it to the WebSocket server

                                Bitmap bitmap = Bitmap.createBitmap(mSurfaceView.getWidth(), mSurfaceView.getHeight(), Bitmap.Config.ARGB_8888);
                                Canvas canvas = new Canvas(bitmap);
                                mSurfaceView.draw(canvas);

                                // Convert the frame to a byte array
                                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                                byte[] frameBytes = stream.toByteArray();
                                String base64String = Base64.encodeToString(frameBytes, Base64.DEFAULT);
                                webSocketClient.send(base64String);

                            }
                        }, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Log.e(TAG, "Camera capture session configuration failed");
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private CaptureRequest createCaptureRequest() {
        try {
            CaptureRequest.Builder builder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            builder.addTarget(mSurfaceHolder.getSurface());
            return builder.build();
        } catch (CameraAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void closeCamera() {
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        if (mCaptureSession != null) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
        if (mImageReader != null) {
            mImageReader.close();
            mImageReader = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //closeCamera();
        //stopBackgroundThread();
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        if (mBackgroundThread != null) {
            mBackgroundThread.quitSafely();
            try {
                mBackgroundThread.join();
                mBackgroundThread = null;
                mBackgroundHandler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
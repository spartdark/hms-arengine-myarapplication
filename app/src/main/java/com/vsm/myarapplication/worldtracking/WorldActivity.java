package com.vsm.myarapplication.worldtracking;

import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.huawei.hiar.ARConfigBase;
import com.huawei.hiar.AREnginesApk;
import com.huawei.hiar.ARSession;
import com.huawei.hiar.ARWorldTrackingConfig;
import com.huawei.hiar.exceptions.ARCameraNotAvailableException;
import com.huawei.hiar.exceptions.ARUnSupportedConfigurationException;
import com.huawei.hiar.exceptions.ARUnavailableClientSdkTooOldException;
import com.huawei.hiar.exceptions.ARUnavailableServiceApkTooOldException;
import com.huawei.hiar.exceptions.ARUnavailableServiceNotInstalledException;
import com.vsm.myarapplication.GestureEvent;
import com.vsm.myarapplication.R;
import com.vsm.myarapplication.common.ConnectAppMarketActivity;
import com.vsm.myarapplication.common.DisplayRotationManager;
import com.vsm.myarapplication.rendering.WorldRenderManager;

import java.util.concurrent.ArrayBlockingQueue;

public class WorldActivity extends AppCompatActivity {
    private static final String TAG = WorldActivity.class.getSimpleName();

    private static final int MOTIONEVENT_QUEUE_CAPACITY = 2;

    private static final int OPENGLES_VERSION = 2;

    private ARSession mArSession;

    private GLSurfaceView mSurfaceView;

    private WorldRenderManager mWorldRenderManager;

    private GestureDetector mGestureDetector;

    private DisplayRotationManager mDisplayRotationManager;

    private ArrayBlockingQueue<GestureEvent> mQueuedSingleTaps = new ArrayBlockingQueue<>(MOTIONEVENT_QUEUE_CAPACITY);

    private String message = null;

    private boolean isRemindInstall = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_world);

        mSurfaceView = findViewById(R.id.surfaceview);
        mDisplayRotationManager = new DisplayRotationManager(this);
        initGestureDetector();

        mSurfaceView.setPreserveEGLContextOnPause(true);
        mSurfaceView.setEGLContextClientVersion(OPENGLES_VERSION);

        // Set the EGL configuration chooser, including for the number of
        // bits of the color buffer and the number of depth bits.
        mSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);

        mWorldRenderManager = new WorldRenderManager(this, this);
        mWorldRenderManager.setDisplayRotationManage(mDisplayRotationManager);
        mWorldRenderManager.setQueuedSingleTaps(mQueuedSingleTaps);

        mSurfaceView.setRenderer(mWorldRenderManager);
        mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    private void initGestureDetector() {
        mGestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                onGestureEvent(GestureEvent.createSingleTapUpEvent(e));
                return true;
            }

            @Override
            public boolean onDown(MotionEvent e) {
                onGestureEvent(GestureEvent.createDownEvent(e));
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                onGestureEvent(GestureEvent.createScrollEvent(e1, e2, distanceX, distanceY));
                return true;
            }
        });
        mSurfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return mGestureDetector.onTouchEvent(event);
            }
        });
    }

    private void onGestureEvent(GestureEvent e) {
        boolean offerResult = mQueuedSingleTaps.offer(e);
        if (offerResult) {
            Log.d(TAG, "Successfully joined the queue.");
        } else {
            Log.d(TAG, "Failed to join queue.");
        }
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        Exception exception = null;
        message = null;
        if (mArSession == null) {
            try {
                if (!arEngineAbilityCheck()) {
                    finish();
                    return;
                }
                mArSession = new ARSession(this);
                ARWorldTrackingConfig config = new ARWorldTrackingConfig(mArSession);
                config.setFocusMode(ARConfigBase.FocusMode.AUTO_FOCUS);
                config.setSemanticMode(ARWorldTrackingConfig.SEMANTIC_PLANE);
                mArSession.configure(config);
                mWorldRenderManager.setArSession(mArSession);
            } catch (Exception capturedException) {
                exception = capturedException;
                setMessageWhenError(capturedException);
            }
            if (message != null) {
                stopArSession(exception);
                return;
            }
        }
        try {
            mArSession.resume();
        } catch (ARCameraNotAvailableException e) {
            Toast.makeText(this, "Camera open failed, please restart the app", Toast.LENGTH_LONG).show();
            mArSession = null;
            return;
        }
        mDisplayRotationManager.registerDisplayListener();
        mSurfaceView.onResume();
    }

    /**
     * Check whether HUAWEI AR Engine server (com.huawei.arengine.service) is installed on
     * the current device. If not, redirect the user to HUAWEI AppGallery for installation.
     */
    private boolean arEngineAbilityCheck() {
        boolean isInstallArEngineApk = AREnginesApk.isAREngineApkReady(this);
        if (!isInstallArEngineApk && isRemindInstall) {
            Toast.makeText(this, "Please agree to install.", Toast.LENGTH_LONG).show();
            finish();
        }
        Log.d(TAG, "Is Install AR Engine Apk: " + isInstallArEngineApk);
        if (!isInstallArEngineApk) {
            startActivity(new Intent(this, ConnectAppMarketActivity.class));
            isRemindInstall = true;
        }
        return AREnginesApk.isAREngineApkReady(this);
    }

    private void setMessageWhenError(Exception catchException) {
        if (catchException instanceof ARUnavailableServiceNotInstalledException) {
            startActivity(new Intent(this, ConnectAppMarketActivity.class));
        } else if (catchException instanceof ARUnavailableServiceApkTooOldException) {
            message = "Please update HuaweiARService.apk";
        } else if (catchException instanceof ARUnavailableClientSdkTooOldException) {
            message = "Please update this app";
        } else if (catchException instanceof ARUnSupportedConfigurationException) {
            message = "The configuration is not supported by the device!";
        } else {
            message = "exception throw";
        }
    }

    private void stopArSession(Exception exception) {
        Log.i(TAG, "stopArSession start.");
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        Log.e(TAG, "Creating session error", exception);
        if (mArSession != null) {
            mArSession.stop();
            mArSession = null;
        }
        Log.i(TAG, "stopArSession end.");
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "onPause start.");
        super.onPause();
        if (mArSession != null) {
            mDisplayRotationManager.unregisterDisplayListener();
            mSurfaceView.onPause();
            mArSession.pause();
        }
        Log.i(TAG, "onPause end.");
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy start.");
        if (mArSession != null) {
            mArSession.stop();
            mArSession = null;
        }
        super.onDestroy();
        Log.i(TAG, "onDestroy end.");
    }

    @Override
    public void onWindowFocusChanged(boolean isHasFocus) {
        Log.d(TAG, "onWindowFocusChanged");
        super.onWindowFocusChanged(isHasFocus);
        if (isHasFocus) {
            getWindow().getDecorView()
                    .setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }
}
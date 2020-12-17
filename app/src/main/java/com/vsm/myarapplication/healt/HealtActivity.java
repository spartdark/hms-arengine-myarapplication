package com.vsm.myarapplication.healt;

import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.huawei.hiar.ARConfigBase;
import com.huawei.hiar.AREnginesApk;
import com.huawei.hiar.ARFaceTrackingConfig;
import com.huawei.hiar.ARSession;
import com.huawei.hiar.common.FaceHealthCheckState;
import com.huawei.hiar.exceptions.ARCameraNotAvailableException;
import com.huawei.hiar.exceptions.ARUnSupportedConfigurationException;
import com.huawei.hiar.exceptions.ARUnavailableClientSdkTooOldException;
import com.huawei.hiar.exceptions.ARUnavailableServiceApkTooOldException;
import com.huawei.hiar.exceptions.ARUnavailableServiceNotInstalledException;
import com.huawei.hiar.listener.FaceHealthCheckStateEvent;
import com.huawei.hiar.listener.FaceHealthServiceListener;
import com.vsm.myarapplication.R;
import com.vsm.myarapplication.common.ConnectAppMarketActivity;
import com.vsm.myarapplication.common.DisplayRotationManager;

import java.util.EventObject;

public class HealtActivity extends AppCompatActivity {

    private static final String TAG = HealtActivity.class.getSimpleName();

    private static final int MAX_PROGRESS = 100;

    private GLSurfaceView mGlSurfaceView;

    private ARSession mArSession;

    private ARFaceTrackingConfig mArFaceTrackingConfig;

    private String mMessage;

    private boolean isRemindInstall = false;

    private HealthRenderManager mHealthRenderManager;

    private DisplayRotationManager mDisplayRotationManager;

    private ProgressBar mHealthProgressBar;

    private TextView mProgressTips;

    private TextView mHealthCheckStatusTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_healt);
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        mHealthProgressBar = findViewById(R.id.health_progress_bar);
        mGlSurfaceView = findViewById(R.id.healthSurfaceView);
        mProgressTips = findViewById(R.id.process_tips);
        mHealthCheckStatusTextView = findViewById(R.id.health_check_status);
        mDisplayRotationManager = new DisplayRotationManager(this);

        mGlSurfaceView.setPreserveEGLContextOnPause(true);

        // Set the OpenGLES version.
        mGlSurfaceView.setEGLContextClientVersion(2);

        // Set the EGL configuration chooser, including for the
        // number of bits of the color buffer and the number of depth bits.
        mGlSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        mHealthRenderManager = new HealthRenderManager(this, this);
        mHealthRenderManager.setDisplayRotationManage(mDisplayRotationManager);
        TableLayout mHealthParamTable = findViewById(R.id.health_param_table);
        mHealthRenderManager.setHealthParamTable(mHealthParamTable);
        mGlSurfaceView.setRenderer(mHealthRenderManager);
        mGlSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMessage = null;
        if (mArSession == null) {
            try {
                if (!arEngineAbilityCheck()) {
                    finish();
                    return;
                }
                mArSession = new ARSession(this);
                mArFaceTrackingConfig = new ARFaceTrackingConfig(mArSession);
                mArFaceTrackingConfig.setEnableItem(ARConfigBase.ENABLE_HEALTH_DEVICE);
                mArSession.configure(mArFaceTrackingConfig);
                setHealthServiceListener();
            } catch (ARUnavailableServiceNotInstalledException capturedException) {
                startActivity(new Intent(this, ConnectAppMarketActivity.class));
            } catch (ARUnavailableServiceApkTooOldException capturedException) {
                mMessage = "Please update HuaweiARService.apk";
            } catch (ARUnavailableClientSdkTooOldException capturedException) {
                mMessage = "Please update this app";
            } catch (ARUnSupportedConfigurationException capturedException) {
                mMessage = "The configuration is not supported by the device!";
            } catch (Exception capturedException) {
                mMessage = "unknown exception throws!";
            }
            if (mMessage != null) {
                stopArSession();
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
        mHealthRenderManager.setArSession(mArSession);
        mGlSurfaceView.onResume();
    }

    private void stopArSession() {
        Log.i(TAG, "Stop session start.");
        Toast.makeText(this, mMessage, Toast.LENGTH_LONG).show();
        if (mArSession != null) {
            mArSession.stop();
            mArSession = null;
        }
        Log.i(TAG, "Stop session end.");
    }

    /**
     * Check whether HUAWEI AR Engine server (com.huawei.arengine.service) is installed on the current device.
     * If not, redirect the user to HUAWEI AppGallery for installation.
     *
     * @return true:AR Engine ready
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

    @Override
    protected void onPause() {
        Log.i(TAG, "onPause start.");
        super.onPause();
        if (mArSession != null) {
            mDisplayRotationManager.unregisterDisplayListener();
            mGlSurfaceView.onPause();
            mArSession.pause();
            Log.i(TAG, "Session paused!");
        }
        Log.i(TAG, "onPause end.");
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy start.");
        super.onDestroy();
        if (mArSession != null) {
            mArSession.stop();
            mArSession = null;
        }
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

    private void setHealthServiceListener() {
        mArSession.addServiceListener(new FaceHealthServiceListener() {
            @Override
            public void handleEvent(EventObject eventObject) {
                if (!(eventObject instanceof FaceHealthCheckStateEvent)) {
                    return;
                }
                final FaceHealthCheckState faceHealthCheckState =
                        ((FaceHealthCheckStateEvent) eventObject).getFaceHealthCheckState();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mHealthCheckStatusTextView.setText(faceHealthCheckState.toString());
                    }
                });
            }

            @Override
            public void handleProcessProgressEvent(final int progress) {
                mHealthRenderManager.setHealthCheckProgress(progress);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setProgressTips(progress);
                    }
                });
            }
        });
    }

    private void setProgressTips(int progress) {
        String progressTips = "processing";
        if (progress >= MAX_PROGRESS) {
            progressTips = "finish";
        }
        mProgressTips.setText(progressTips);
        mHealthProgressBar.setProgress(progress);
    }
}
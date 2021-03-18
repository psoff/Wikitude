package com.wikitude.samples.tracking.instant;

import com.wikitude.NativeStartupConfiguration;
import com.wikitude.WikitudeSDK;
import com.wikitude.common.CompletionCallback;
import com.wikitude.common.ErrorCallback;
import com.wikitude.common.WikitudeError;
import com.wikitude.common.camera.CameraSettings;
import com.wikitude.common.rendering.InternalRendering;
import com.wikitude.common.rendering.RenderExtension;
import com.wikitude.common.rendering.RenderSettings;
import com.wikitude.nativesdksampleapp.R;
import com.wikitude.samples.WikitudeSDKConstants;
import com.wikitude.samples.rendering.internal.CustomRenderExtension;
import com.wikitude.tracker.InitializationPose;
import com.wikitude.tracker.InstantTarget;
import com.wikitude.tracker.InstantTargetRestorationConfiguration;
import com.wikitude.tracker.InstantTracker;
import com.wikitude.tracker.InstantTrackerConfiguration;
import com.wikitude.tracker.InstantTrackerListener;
import com.wikitude.tracker.InstantTrackingState;
import com.wikitude.tracker.Plane;
import com.wikitude.tracker.TargetCollectionResource;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.File;

public class LoadInstantTargetActivity extends Activity implements InternalRendering, InstantTrackerListener {

    private CustomRenderExtension renderExtension;

    private WikitudeSDK wikitudeSDK;
    private InstantTracker instantTracker;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        wikitudeSDK = new WikitudeSDK(this);

        renderExtension = new CustomRenderExtension();

        final NativeStartupConfiguration config = new NativeStartupConfiguration();
        config.setLicenseKey(WikitudeSDKConstants.WIKITUDE_SDK_KEY);
        config.setCameraPosition(CameraSettings.CameraPosition.BACK);

        wikitudeSDK.onCreate(getApplicationContext(), this, config);

        wikitudeSDK.getCameraManager().setRenderingCorrectedFovChangedListener(renderExtension);

        InstantTrackerConfiguration instantConfig = new InstantTrackerConfiguration();
        instantConfig.setSMARTEnabled(false);

        instantTracker = wikitudeSDK.getTrackerManager().createInstantTracker(this, instantConfig);

        setContentView(R.layout.activity_instant_load_target);

        RelativeLayout root = findViewById(R.id.instant_tracking);
        root.addView(wikitudeSDK.setupWikitudeGLSurfaceView(), 0);

        findViewById(R.id.load_button).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                InstantTargetRestorationConfiguration configuration = new InstantTargetRestorationConfiguration();
                configuration.setInstantTargetExpansionPolicy(InstantTargetRestorationConfiguration.InstantTargetExpansionPolicy.ALLOW_EXPANSION);

                String savedTargetPath = "file://" + new File(getExternalFilesDir(null), "savedTarget.wto").getAbsolutePath();
                TargetCollectionResource savedTarget = wikitudeSDK.getTrackerManager().createTargetCollectionResource(savedTargetPath);

                instantTracker.loadExistingInstantTarget(
                        savedTarget,
                        new CompletionCallback() {
                            @Override
                            public void onCompletion() {
                                Toast.makeText(LoadInstantTargetActivity.this, "Loading was successful.", Toast.LENGTH_SHORT).show();
                            }
                        },
                        new ErrorCallback() {
                            @Override
                            public void onError(@NonNull final WikitudeError error) {
                                Toast.makeText(LoadInstantTargetActivity.this, "Loading failed: " + error.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        },
                        configuration,
                        new Handler(getMainLooper()) // Receive callbacks on main thread.
                );
            }
        });

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onResume() {
        super.onResume();
        wikitudeSDK.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        wikitudeSDK.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        wikitudeSDK.getTrackerManager().destroyInstantTracker(instantTracker);
        wikitudeSDK.onDestroy();
    }

    @Override
    public RenderExtension provideRenderExtension() {
        return renderExtension;
    }

    @Override
    public void onRenderingApiInstanceCreated(final RenderSettings.RenderingAPI renderingAPI) {

    }

    @Override
    public void onStateChanged(final InstantTracker tracker, final InstantTrackingState state) {

    }

    @Override
    public void onStateChangeFailed(final InstantTracker tracker, final InstantTrackingState failedState, final WikitudeError error) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(LoadInstantTargetActivity.this)
                        .setTitle(R.string.instant_tracking_state_failed_title)
                        .setMessage(error.getMessage())
                        .setNeutralButton(android.R.string.ok, null)
                        .show();
            }
        });
    }

    @Override
    public void onInitializationPoseChanged(final InstantTracker tracker, final InitializationPose pose) {

    }

    @Override
    public void onTrackingStarted(final InstantTracker tracker, InstantTarget target) {
        renderExtension.setCurrentlyRecognizedTarget(target);
    }

    @Override
    public void onTracked(final InstantTracker tracker, final InstantTarget target) {
        
    }

    @Override
    public void onTrackingStopped(final InstantTracker tracker) {
        renderExtension.setCurrentlyRecognizedTarget(null);
    }

    @Override
    public void onPlaneRecognized(final InstantTracker tracker, final Plane plane) {}

    @Override
    public void onPlaneLost(final InstantTracker tracker, final Plane plane) {}

    @Override
    public void onPlaneTracked(final InstantTracker tracker, final Plane plane) {}
}

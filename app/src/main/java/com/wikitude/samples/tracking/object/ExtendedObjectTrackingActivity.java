package com.wikitude.samples.tracking.object;

import com.wikitude.NativeStartupConfiguration;
import com.wikitude.WikitudeSDK;
import com.wikitude.common.WikitudeError;
import com.wikitude.common.camera.CameraSettings;
import com.wikitude.common.rendering.RenderExtension;
import com.wikitude.nativesdksampleapp.R;
import com.wikitude.rendering.ExternalRendering;
import com.wikitude.samples.WikitudeSDKConstants;
import com.wikitude.samples.rendering.external.CustomSurfaceView;
import com.wikitude.samples.rendering.external.Driver;
import com.wikitude.samples.rendering.external.GLRenderer;
import com.wikitude.samples.rendering.external.OccluderCube;
import com.wikitude.samples.rendering.external.StrokedCube;
import com.wikitude.samples.util.DropDownAlert;
import com.wikitude.tracker.ObjectTarget;
import com.wikitude.tracker.ObjectTracker;
import com.wikitude.tracker.ObjectTrackerConfiguration;
import com.wikitude.tracker.ObjectTrackerListener;
import com.wikitude.tracker.TargetCollectionResource;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;

public class ExtendedObjectTrackingActivity extends Activity implements ObjectTrackerListener, ExternalRendering {

    private static final String TAG = "ExtendedObjectTracking";

    private WikitudeSDK wikitudeSDK;
    private CustomSurfaceView surfaceView;
    private Driver driver;
    private GLRenderer glRenderer;

    private ObjectTracker objectTracker;

    private Button stopExtendedTrackingButton;
    private DropDownAlert dropDownAlert;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        wikitudeSDK = new WikitudeSDK(this);
        NativeStartupConfiguration startupConfiguration = new NativeStartupConfiguration();
        startupConfiguration.setLicenseKey(WikitudeSDKConstants.WIKITUDE_SDK_KEY);
        startupConfiguration.setCameraPosition(CameraSettings.CameraPosition.BACK);
        startupConfiguration.setCameraResolution(CameraSettings.CameraResolution.AUTO);

        wikitudeSDK.onCreate(getApplicationContext(), this, startupConfiguration);

        final TargetCollectionResource targetCollectionResource = wikitudeSDK.getTrackerManager().createTargetCollectionResource("file:///android_asset/firetruck.wto");
        ObjectTrackerConfiguration trackerConfiguration = new ObjectTrackerConfiguration();
        trackerConfiguration.setExtendedTargets(new String[]{"*"});
        objectTracker = wikitudeSDK.getTrackerManager().createObjectTracker(targetCollectionResource, ExtendedObjectTrackingActivity.this, trackerConfiguration);

        dropDownAlert = new DropDownAlert(this);
        dropDownAlert.setText("Loading Target:");
        dropDownAlert.setTextWeight(1);
        dropDownAlert.show();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onResume() {
        super.onResume();
        wikitudeSDK.onResume();
        surfaceView.onResume();
        driver.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        surfaceView.onPause();
        driver.stop();
        wikitudeSDK.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        wikitudeSDK.onDestroy();
    }

    @Override
    public void onRenderExtensionCreated(final RenderExtension renderExtension) {
        glRenderer = new GLRenderer(renderExtension);
        wikitudeSDK.getCameraManager().setRenderingCorrectedFovChangedListener(glRenderer);
        surfaceView = new CustomSurfaceView(getApplicationContext(), glRenderer);
        driver = new Driver(surfaceView, 30);
        setContentView(R.layout.activity_extended_tracking);
        RelativeLayout root = findViewById(R.id.extended_tracking_layout);
        root.addView(surfaceView, 0);

        stopExtendedTrackingButton = findViewById(R.id.stop_extended_tracking_button);
        stopExtendedTrackingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopExtendedTrackingButton.setVisibility(View.GONE);
                objectTracker.stopExtendedTracking();
            }
        });
    }

    @Override
    public void onTargetsLoaded(ObjectTracker tracker) {
        Log.v(TAG, "Object tracker loaded");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dropDownAlert.setText("Scan Target:");
                dropDownAlert.addImages("firetruck_image.png");
                dropDownAlert.setTextWeight(0.5f);
            }
        });
    }

    @Override
    public void onErrorLoadingTargets(ObjectTracker tracker, WikitudeError error) {
        Log.v(TAG, "Unable to load image tracker. Reason: " + error.getMessage());
    }

    @Override
    public void onObjectRecognized(ObjectTracker tracker, final ObjectTarget target) {
        Log.v(TAG, "Recognized target " + target.getName());
        dropDownAlert.dismiss();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                stopExtendedTrackingButton.setVisibility(View.VISIBLE);
            }
        });

        StrokedCube strokedCube = new StrokedCube();
        OccluderCube occluderCube = new OccluderCube();

        glRenderer.setRenderablesForKey(target.getName(), strokedCube, occluderCube);

        StrokedCube extendedCube = new StrokedCube();
        extendedCube.setColor(0, 0, 1);
        glRenderer.setRenderablesForKey(target.getName() + "extended", extendedCube, null);
    }

    @Override
    public void onObjectTracked(ObjectTracker tracker, final ObjectTarget target) {
        StrokedCube strokedCube = (StrokedCube) glRenderer.getRenderableForKey(target.getName());
        if (strokedCube != null) {
            strokedCube.viewMatrix = target.getViewMatrix();

            strokedCube.setXScale(target.getTargetScale().x);
            strokedCube.setYScale(target.getTargetScale().y);
            strokedCube.setZScale(target.getTargetScale().z);
        }

        OccluderCube occluderCube = (OccluderCube) glRenderer.getOccluderForKey(target.getName());
        if (occluderCube != null) {
            occluderCube.viewMatrix = target.getViewMatrix();

            occluderCube.setXScale(target.getTargetScale().x);
            occluderCube.setYScale(target.getTargetScale().y);
            occluderCube.setZScale(target.getTargetScale().z);
        }

        StrokedCube extendedCube = (StrokedCube) glRenderer.getRenderableForKey(target.getName() + "extended");
        if (extendedCube != null) {
            extendedCube.viewMatrix = target.getViewMatrix();

            extendedCube.setXScale(target.getTargetScale().x * 3);
            extendedCube.setYScale(target.getTargetScale().y * 3);
            extendedCube.setZScale(target.getTargetScale().z * 3);
        }
    }

    @Override
    public void onObjectLost(ObjectTracker tracker, final ObjectTarget target) {
        Log.v(TAG, "Lost target " + target.getName());
        glRenderer.removeRenderablesForKey(target.getName());
        glRenderer.removeRenderablesForKey(target.getName() + "extended");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                EditText trackingQualityIndicator = findViewById(R.id.tracking_quality_indicator);
                trackingQualityIndicator.setVisibility(View.INVISIBLE);
            }
        });
    }

    @Override
    public void onExtendedTrackingQualityChanged(final ObjectTracker tracker, final ObjectTarget target, final int oldTrackingQuality, final int newTrackingQuality) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                EditText trackingQualityIndicator = findViewById(R.id.tracking_quality_indicator);
                switch (newTrackingQuality) {
                    case -1:
                        trackingQualityIndicator.setBackgroundColor(Color.parseColor("#FF3420"));
                        trackingQualityIndicator.setText(R.string.tracking_quality_indicator_bad);
                        break;
                    case 0:
                        trackingQualityIndicator.setBackgroundColor(Color.parseColor("#FFD900"));
                        trackingQualityIndicator.setText(R.string.tracking_quality_indicator_average);
                        break;
                    default:
                        trackingQualityIndicator.setBackgroundColor(Color.parseColor("#6BFF00"));
                        trackingQualityIndicator.setText(R.string.tracking_quality_indicator_good);
                }
                trackingQualityIndicator.setVisibility(View.VISIBLE);
            }
        });
    }
}

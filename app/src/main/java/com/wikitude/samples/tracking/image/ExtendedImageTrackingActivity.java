package com.wikitude.samples.tracking.image;

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
import com.wikitude.samples.rendering.external.GLRendererExtendedTracking;
import com.wikitude.samples.rendering.external.StrokedRectangle;
import com.wikitude.samples.util.DropDownAlert;
import com.wikitude.tracker.ImageTarget;
import com.wikitude.tracker.ImageTracker;
import com.wikitude.tracker.ImageTrackerConfiguration;
import com.wikitude.tracker.ImageTrackerListener;
import com.wikitude.tracker.TargetCollectionResource;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

public class ExtendedImageTrackingActivity extends Activity implements ImageTrackerListener, ExternalRendering {

    private static final String TAG = "ExtendedTracking";

    private WikitudeSDK wikitudeSDK;
    private CustomSurfaceView customSurfaceView;
    private Driver driver;
    private GLRendererExtendedTracking glRenderer;

    private ImageTracker imageTracker;

    private Button stopExtendedTrackingButton;
    private DropDownAlert dropDownAlert;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        wikitudeSDK = new WikitudeSDK(this);
        NativeStartupConfiguration startupConfiguration = new NativeStartupConfiguration();
        startupConfiguration.setLicenseKey(WikitudeSDKConstants.WIKITUDE_SDK_KEY);
        startupConfiguration.setCameraPosition(CameraSettings.CameraPosition.BACK);
        wikitudeSDK.onCreate(getApplicationContext(), this, startupConfiguration);

        ImageTrackerConfiguration trackerConfiguration = new ImageTrackerConfiguration();
        trackerConfiguration.setExtendedTargets(new String[]{"*"});

        final TargetCollectionResource targetCollectionResource = wikitudeSDK.getTrackerManager().createTargetCollectionResource("file:///android_asset/iot_tracker.wtc");
        imageTracker = wikitudeSDK.getTrackerManager().createImageTracker(targetCollectionResource, this, trackerConfiguration);

        dropDownAlert = new DropDownAlert(this);
        dropDownAlert.setText("Scan Target IOT:");
        dropDownAlert.addImages("target_iot.jpg");
        dropDownAlert.setTextWeight(0.5f);
        dropDownAlert.show();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onResume() {
        super.onResume();
        wikitudeSDK.onResume();
        customSurfaceView.onResume();
        driver.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        customSurfaceView.onPause();
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
        glRenderer = new GLRendererExtendedTracking(renderExtension);
        wikitudeSDK.getCameraManager().setRenderingCorrectedFovChangedListener(glRenderer);
        customSurfaceView = new CustomSurfaceView(getApplicationContext(), glRenderer);
        driver = new Driver(customSurfaceView, 30);

        FrameLayout viewHolder = new FrameLayout(getApplicationContext());
        setContentView(viewHolder);

        viewHolder.addView(customSurfaceView);

        LayoutInflater inflater = LayoutInflater.from(getApplicationContext());
        RelativeLayout trackingQualityIndicator = (RelativeLayout) inflater.inflate(R.layout.activity_extended_tracking, null);
        viewHolder.addView(trackingQualityIndicator);

        stopExtendedTrackingButton = findViewById(R.id.stop_extended_tracking_button);
        stopExtendedTrackingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopExtendedTrackingButton.setVisibility(View.GONE);
                imageTracker.stopExtendedTracking();
            }
        });
    }

    @Override
    public void onTargetsLoaded(ImageTracker tracker) {
        Log.v(TAG, "Image tracker loaded");
    }

    @Override
    public void onErrorLoadingTargets(ImageTracker tracker, WikitudeError error) {
        Log.v(TAG, "Unable to load image tracker. Reason: " + error.getMessage());
    }

    @Override
    public void onImageRecognized(ImageTracker tracker, final ImageTarget target) {
        Log.v(TAG, "Recognized target " + target.getName());
        dropDownAlert.dismiss();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                stopExtendedTrackingButton.setVisibility(View.VISIBLE);
            }
        });

        StrokedRectangle strokedRectangle = new StrokedRectangle(StrokedRectangle.Type.STANDARD);
        glRenderer.setRenderablesForKey(target.getName() + target.getUniqueId(), strokedRectangle, null);

        StrokedRectangle extendedRectangle = new StrokedRectangle(StrokedRectangle.Type.EXTENDED);
        extendedRectangle.setColor(0, 0, 1);
        glRenderer.setRenderablesForKey(target.getName() + target.getUniqueId() + "extended", extendedRectangle, null);

    }

    @Override
    public void onImageTracked(ImageTracker tracker, final ImageTarget target) {
        StrokedRectangle strokedRectangle = (StrokedRectangle) glRenderer.getRenderableForKey(target.getName() + target.getUniqueId());
        StrokedRectangle extendedRectangle = (StrokedRectangle) glRenderer.getRenderableForKey(target.getName() + target.getUniqueId() + "extended");

        if (strokedRectangle != null) {
            strokedRectangle.viewMatrix = target.getViewMatrix();

            strokedRectangle.setXScale(target.getTargetScale().x);
            strokedRectangle.setYScale(target.getTargetScale().y);
        }
        if (extendedRectangle != null) {
            extendedRectangle.viewMatrix = target.getViewMatrix();

            extendedRectangle.setXScale(target.getTargetScale().x * 3);
            extendedRectangle.setYScale(target.getTargetScale().y * 3);
        }
    }

    @Override
    public void onImageLost(ImageTracker tracker, final ImageTarget target) {
        Log.v(TAG, "Lost target " + target.getName());
        glRenderer.removeRenderablesForKey(target.getName() + target.getUniqueId());
        glRenderer.removeRenderablesForKey(target.getName() + target.getUniqueId() + "extended");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                EditText trackingQualityIndicator = findViewById(R.id.tracking_quality_indicator);
                trackingQualityIndicator.setVisibility(View.INVISIBLE);
            }
        });
    }

    @Override
    public void onExtendedTrackingQualityChanged(ImageTracker tracker, final ImageTarget target, final int oldTrackingQuality, final int newTrackingQuality) {
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

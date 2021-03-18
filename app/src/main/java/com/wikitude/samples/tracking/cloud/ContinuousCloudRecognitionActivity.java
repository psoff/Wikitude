package com.wikitude.samples.tracking.cloud;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import android.widget.Toast;
import com.wikitude.WikitudeSDK;
import com.wikitude.NativeStartupConfiguration;
import com.wikitude.common.WikitudeError;
import com.wikitude.common.camera.CameraSettings;
import com.wikitude.common.rendering.RenderExtension;
import com.wikitude.nativesdksampleapp.R;
import com.wikitude.rendering.ExternalRendering;
import com.wikitude.samples.WikitudeSDKConstants;
import com.wikitude.samples.rendering.external.CustomSurfaceView;
import com.wikitude.samples.rendering.external.Driver;
import com.wikitude.samples.rendering.external.GLRenderer;
import com.wikitude.samples.rendering.external.StrokedRectangle;
import com.wikitude.samples.util.DropDownAlert;
import com.wikitude.tracker.ImageTarget;
import com.wikitude.tracker.ImageTracker;
import com.wikitude.tracker.ImageTrackerListener;
import com.wikitude.tracker.CloudRecognitionService;
import com.wikitude.tracker.CloudRecognitionServiceResponse;
import com.wikitude.tracker.ContinuousCloudRecognitionServiceInterruptionListener;
import com.wikitude.tracker.CloudRecognitionServiceInitializationCallback;
import com.wikitude.tracker.CloudRecognitionServiceListener;

public class ContinuousCloudRecognitionActivity extends Activity implements ImageTrackerListener, ExternalRendering {

    private static final String TAG = "ContinuousCloudTracking";

    private WikitudeSDK wikitudeSDK;
    private CustomSurfaceView customSurfaceView;
    private Driver driver;
    private GLRenderer glRenderer;

    private CloudRecognitionService cloudRecognitionService;
    private boolean continuousRecognitionEnabled = false;
    private int recognitionInterval = 1000;

    private Button toggleButton;
    private DropDownAlert dropDownAlert;
    private EditText targetInformationTextField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        wikitudeSDK = new WikitudeSDK(this);
        NativeStartupConfiguration startupConfiguration = new NativeStartupConfiguration();
        startupConfiguration.setLicenseKey(WikitudeSDKConstants.WIKITUDE_SDK_KEY);
        startupConfiguration.setCameraPosition(CameraSettings.CameraPosition.BACK);

        wikitudeSDK.onCreate(getApplicationContext(), this, startupConfiguration);

        cloudRecognitionService = wikitudeSDK.getTrackerManager().createCloudRecognitionService("b277eeadc6183ab57a83b07682b3ceba", "B1QL5CTCZ", "54e4b9fe6134bb74351b2aa3",
                new CloudRecognitionServiceInitializationCallback() {
                    @Override
                    public void onError(WikitudeError error) {
                        Log.e(TAG, "Cloud Recognition Service failed to initialize. Reason: " + error.getMessage());
                    }

                    @Override
                    public void onInitialized() {
                        wikitudeSDK.getTrackerManager().createImageTracker(cloudRecognitionService, ContinuousCloudRecognitionActivity.this, null);
                    }
                }, null);

        dropDownAlert = new DropDownAlert(this);
        dropDownAlert.setText("Scan:");
        dropDownAlert.addImages("austria.jpg", "brazil.jpg", "france.jpg", "germany.jpg", "italy.jpg");
        dropDownAlert.setTextWeight(0.2f);
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
        glRenderer = new GLRenderer(renderExtension);
        wikitudeSDK.getCameraManager().setRenderingCorrectedFovChangedListener(glRenderer);
        customSurfaceView = new CustomSurfaceView(getApplicationContext(), glRenderer);
        driver = new Driver(customSurfaceView, 30);

        FrameLayout viewHolder = new FrameLayout(getApplicationContext());
        setContentView(viewHolder);

        viewHolder.addView(customSurfaceView);

        LayoutInflater inflater = LayoutInflater.from(getApplicationContext());
        LinearLayout controls = (LinearLayout) inflater.inflate(R.layout.activity_continuous_cloud_tracking, null);
        viewHolder.addView(controls);

        targetInformationTextField = findViewById(R.id.continuous_tracking_info_field);

        toggleButton = findViewById(R.id.continuous_recognition_cloud_tracking_start_button);
        toggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                toggleContinuousRecognition();
            }
        });
    }

    private void toggleContinuousRecognition() {
        if (continuousRecognitionEnabled) {
            stopContinuousRecognition();
        } else {
            startContinuousRecognition();
        }
    }

    private void setButtonText(final int resId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                toggleButton.setText(resId);
            }
        });
    }

    private void stopContinuousRecognition() {
        continuousRecognitionEnabled = false;
        cloudRecognitionService.stopContinuousRecognition();
        setButtonText(R.string.continuous_recognition_cloud_tracking_start_button);
    }

    private void startContinuousRecognition() {
        continuousRecognitionEnabled = true;
        setButtonText(R.string.continuous_recognition_cloud_tracking_stop_button);
        targetInformationTextField.setVisibility(View.INVISIBLE);
        cloudRecognitionService.startContinuousRecognition(recognitionInterval, new ContinuousCloudRecognitionServiceInterruptionListener() {
            @Override
            public void onInterruption(int preferredInterval) {
                recognitionInterval = preferredInterval;
            }
        }, new CloudRecognitionServiceListener() {
            @Override
            public void onResponse(final CloudRecognitionServiceResponse response) {
                if (response.isRecognized()) {
                    if (continuousRecognitionEnabled) {
                        stopContinuousRecognition();
                    }

                    // This needs to be copied since access to the response is invalid after the end of the scope
                    final String targetName = response.getTargetInformationsObject().getName();
                    dropDownAlert.dismiss();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            targetInformationTextField.setText(targetName, TextView.BufferType.NORMAL);
                            targetInformationTextField.setVisibility(View.VISIBLE);
                        }
                    });
                }
            }

            @Override
            public void onError(final WikitudeError error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(ContinuousCloudRecognitionActivity.this, "Recognition failed - " + error.getDescription(), Toast.LENGTH_LONG).show();
                    }
                });
                stopContinuousRecognition();
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

        StrokedRectangle strokedRectangle = new StrokedRectangle(StrokedRectangle.Type.STANDARD);
        glRenderer.setRenderablesForKey(target.getName() + target.getUniqueId(), strokedRectangle, null);
    }

    @Override
    public void onImageTracked(ImageTracker tracker, final ImageTarget target) {
        StrokedRectangle strokedRectangle = (StrokedRectangle) glRenderer.getRenderableForKey(target.getName() + target.getUniqueId());

        if (strokedRectangle != null) {
            strokedRectangle.viewMatrix = target.getViewMatrix();

            strokedRectangle.setXScale(target.getTargetScale().x);
            strokedRectangle.setYScale(target.getTargetScale().y);
        }
    }

    @Override
    public void onImageLost(final ImageTracker tracker, final ImageTarget target) {
        Log.v(TAG, "Lost target " + target.getName());
        glRenderer.removeRenderablesForKey(target.getName() + target.getUniqueId());
    }

    @Override
    public void onExtendedTrackingQualityChanged(final ImageTracker tracker, final ImageTarget target, final int oldTrackingQuality, final int newTrackingQuality) {

    }
}

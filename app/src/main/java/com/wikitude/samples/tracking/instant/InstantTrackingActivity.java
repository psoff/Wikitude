package com.wikitude.samples.tracking.instant;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
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
import com.wikitude.tracker.InstantTracker;
import com.wikitude.tracker.InstantTrackerListener;
import com.wikitude.tracker.InstantTarget;
import com.wikitude.tracker.InitializationPose;
import com.wikitude.tracker.InstantTrackingState;
import com.wikitude.tracker.IsPlatformAssistedTrackingAvailableCallback;
import com.wikitude.tracker.Plane;
import com.wikitude.tracker.SmartAvailability;

public class InstantTrackingActivity extends Activity implements InstantTrackerListener, ExternalRendering {

    private static final String TAG = "InstantTracking";

    private WikitudeSDK wikitudeSDK;
    private CustomSurfaceView customSurfaceView;
    private Driver driver;
    private GLRenderer glRenderer;

    private InstantTracker instantTracker;

    private InstantTrackingState currentTrackingState = InstantTrackingState.Initializing;

    private LinearLayout heightSettingsLayout;
    private Button changeStateButton;
    private DropDownAlert dropDownAlert;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        wikitudeSDK = new WikitudeSDK(this);
        NativeStartupConfiguration startupConfiguration = new NativeStartupConfiguration();
        startupConfiguration.setLicenseKey(WikitudeSDKConstants.WIKITUDE_SDK_KEY);
        startupConfiguration.setCameraPosition(CameraSettings.CameraPosition.BACK);

        wikitudeSDK.onCreate(getApplicationContext(), this, startupConfiguration);

        wikitudeSDK.getTrackerManager().isPlatformAssistedTrackingSupported(new IsPlatformAssistedTrackingAvailableCallback() {
            @Override
            public void onAvailabilityChanged(SmartAvailability availability) {
                switch (availability) {
                    case INDETERMINATE_QUERY_FAILED:
                        /* user may try again here */
                        dropDownAlert.setText(getString(R.string.instant_tracking_running_non_assisted_support_unknown));

                        instantTracker = wikitudeSDK.getTrackerManager().createInstantTracker(InstantTrackingActivity.this, null);
                        break;
                    case CHECKING_QUERY_ONGOING:
                        /* be patient; do nothing */
                        break;
                    case UNSUPPORTED:
                        dropDownAlert.setText(getString(R.string.instant_tracking_running_non_assisted));

                        instantTracker = wikitudeSDK.getTrackerManager().createInstantTracker(InstantTrackingActivity.this, null);
                        break;
                    case SUPPORTED_UPDATE_REQUIRED:
                    case SUPPORTED:
                        if (instantTracker == null) {
                            dropDownAlert.setText(
                                    getString(R.string.instant_tracking_running_assisted) + "\n" +
                                            getString(R.string.instant_tracking_running_assisted_description)
                            );

                            instantTracker = wikitudeSDK.getTrackerManager().createInstantTracker(InstantTrackingActivity.this, null);
                        }
                        break;
                }
            }
        });

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
    public void onRenderExtensionCreated(final RenderExtension renderExtension_) {
        glRenderer = new GLRenderer(renderExtension_);
        wikitudeSDK.getCameraManager().setRenderingCorrectedFovChangedListener(glRenderer);
        customSurfaceView = new CustomSurfaceView(getApplicationContext(), glRenderer);
        driver = new Driver(customSurfaceView, 30);
        setContentView(customSurfaceView);

        final FrameLayout viewHolder = new FrameLayout(getApplicationContext());
        setContentView(viewHolder);

        viewHolder.addView(customSurfaceView);

        LayoutInflater inflater = LayoutInflater.from(getApplicationContext());
        final RelativeLayout controls = (RelativeLayout) inflater.inflate(R.layout.activity_instant_tracking, null);
        viewHolder.addView(controls);

        heightSettingsLayout = findViewById(R.id.heightSettingsLayout);
        dropDownAlert = new DropDownAlert(InstantTrackingActivity.this);
        dropDownAlert.setText(getString(R.string.instant_tracking_assisted_decision_pending));
        heightSettingsLayout.post(new Runnable() {
            @Override
            public void run() {
                dropDownAlert.setMarginTop(heightSettingsLayout.getHeight());
                dropDownAlert.setTextWeight(1);
                dropDownAlert.show(viewHolder);
                controls.bringToFront();
            }
        });

        changeStateButton = findViewById(R.id.on_change_tracker_state);
        changeStateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {

                    if (instantTracker == null) {
                        Toast.makeText(InstantTrackingActivity.this, "Tracker is not available yet", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (currentTrackingState == InstantTrackingState.Initializing) {
                        instantTracker.setState(InstantTrackingState.Tracking);
                    } else {
                        instantTracker.setState(InstantTrackingState.Initializing);
                    }
            }
        });

        final TextView heightBox = findViewById(R.id.heightTextView);
        heightBox.setText(String.format( "%.2f", 1.4f ));

        final SeekBar heightSlider = findViewById(R.id.heightSeekBar);
        heightSlider.setMax(190);
        heightSlider.setProgress(130);
        heightSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                if (instantTracker == null) {
                    Toast.makeText(InstantTrackingActivity.this, "Tracker is not available yet", Toast.LENGTH_SHORT).show();
                    return;
                }
                float height = (progress + 10) / 100.f;
                instantTracker.setDeviceHeightAboveGround(height);
                heightBox.setText(String.format( "%.2f", height ));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

    }

    @Override
    public void onStateChanged(InstantTracker tracker, final InstantTrackingState state) {
        currentTrackingState = state;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (state == InstantTrackingState.Tracking) {
                    // deviceHeightAboveGround may not be called during tracking
                    heightSettingsLayout.setVisibility(View.GONE);
                    dropDownAlert.setMarginTop(0);
                    changeStateButton.setText(R.string.instant_tracking_button_start_initialization);
                    getRectangle().setColor(1.0f, 0.58f, 0.16f);
                } else {
                    heightSettingsLayout.setVisibility(View.VISIBLE);
                    dropDownAlert.setMarginTop(heightSettingsLayout.getHeight());
                    changeStateButton.setText(R.string.instant_tracking_button_start_tracking);
                }
            }
        });
    }

    @Override
    public void onStateChangeFailed(final InstantTracker tracker, final InstantTrackingState failedState, final WikitudeError error) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(InstantTrackingActivity.this)
                        .setTitle(R.string.instant_tracking_state_failed_title)
                        .setMessage(error.getMessage())
                        .setNeutralButton(android.R.string.ok, null)
                        .show();
            }
        });
    }

    @Override
    public void onInitializationPoseChanged(InstantTracker tracker, InitializationPose pose) {
        StrokedRectangle strokedRectangle = getRectangle();
        strokedRectangle.viewMatrix = pose.getViewMatrix();

        if (instantTracker.canStartTracking()) {
            strokedRectangle.setColor(0.0f, 1.0f, 0.0f);
        } else {
            strokedRectangle.setColor(1.0f, 0.0f, 0.0f);
        }
    }

    @Override
    public void onTrackingStarted(InstantTracker tracker, InstantTarget target) {
        StrokedRectangle strokedRectangle = getRectangle();
        strokedRectangle.viewMatrix = target.getViewMatrix();
    }

    @Override
    public void onTracked(InstantTracker tracker, InstantTarget target) {
        StrokedRectangle strokedRectangle = getRectangle();
        strokedRectangle.viewMatrix = target.getViewMatrix();
    }

    @Override
    public void onTrackingStopped(InstantTracker tracker) {
        glRenderer.removeRenderablesForKey("");
    }

    @Override
    public void onPlaneRecognized(final InstantTracker tracker, final Plane plane) {}

    @Override
    public void onPlaneLost(final InstantTracker tracker, final Plane plane) {}

    @Override
    public void onPlaneTracked(final InstantTracker tracker, final Plane plane) {}

    @NonNull
    private StrokedRectangle getRectangle() {
        StrokedRectangle strokedRectangle = (StrokedRectangle) glRenderer.getRenderableForKey("");
        if (strokedRectangle == null) {
            strokedRectangle = new StrokedRectangle();
            glRenderer.setRenderablesForKey("", strokedRectangle, null);
        }
        return strokedRectangle;
    }
}

package com.wikitude.samples.tracking.instant;

import com.wikitude.NativeStartupConfiguration;
import com.wikitude.WikitudeSDK;
import com.wikitude.common.WikitudeError;
import com.wikitude.common.camera.CameraSettings;
import com.wikitude.common.rendering.RenderExtension;
import com.wikitude.common.util.Vector2;
import com.wikitude.common.util.Vector3;
import com.wikitude.nativesdksampleapp.R;
import com.wikitude.rendering.ExternalRendering;
import com.wikitude.samples.WikitudeSDKConstants;
import com.wikitude.samples.rendering.external.CustomSurfaceView;
import com.wikitude.samples.rendering.external.Driver;
import com.wikitude.samples.rendering.external.GLRenderer;
import com.wikitude.samples.rendering.external.PlanePolygon;
import com.wikitude.samples.rendering.external.Renderable;
import com.wikitude.samples.rendering.external.StrokedCube;
import com.wikitude.samples.rendering.external.StrokedRectangle;
import com.wikitude.tracker.InitializationPose;
import com.wikitude.tracker.InstantTarget;
import com.wikitude.tracker.InstantTracker;
import com.wikitude.tracker.InstantTrackerConfiguration;
import com.wikitude.tracker.InstantTrackerListener;
import com.wikitude.tracker.InstantTrackingState;
import com.wikitude.tracker.Plane;
import com.wikitude.tracker.PlaneDetectionConfiguration;
import com.wikitude.tracker.PlaneFilter;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class PlaneDetectionActivity extends Activity implements InstantTrackerListener, ExternalRendering {

    private static final String TAG = "InstantTracking";

    private WikitudeSDK wikitudeSDK;
    private CustomSurfaceView customSurfaceView;
    private Driver driver;
    private GLRenderer glRenderer;

    private InstantTracker instantTracker;

    private InstantTrackingState currentTrackingState = InstantTrackingState.Initializing;

    private LinearLayout heightSettingsLayout;
    private Button changeStateButton;
    private int cubeID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        wikitudeSDK = new WikitudeSDK(this);
        NativeStartupConfiguration startupConfiguration = new NativeStartupConfiguration();
        startupConfiguration.setLicenseKey(WikitudeSDKConstants.WIKITUDE_SDK_KEY);
        startupConfiguration.setCameraPosition(CameraSettings.CameraPosition.BACK);

        wikitudeSDK.onCreate(getApplicationContext(), this, startupConfiguration);

        PlaneDetectionConfiguration planeDetectionConfiguration = new PlaneDetectionConfiguration();
        planeDetectionConfiguration.setPlaneFilter(PlaneFilter.ANY);
        planeDetectionConfiguration.enableConvexHull();

        InstantTrackerConfiguration trackerConfiguration = new InstantTrackerConfiguration();
        trackerConfiguration.setSMARTEnabled(false);
        trackerConfiguration.enablePlaneDetection(planeDetectionConfiguration);

        instantTracker = wikitudeSDK.getTrackerManager().createInstantTracker(PlaneDetectionActivity.this, trackerConfiguration);

        this.customSurfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {

                    if (instantTracker == null) {
                        Toast.makeText(PlaneDetectionActivity.this, "Tracker is not available yet", Toast.LENGTH_SHORT).show();
                        return true;
                    }

                    if (currentTrackingState != InstantTrackingState.Tracking) {
                        new AlertDialog.Builder(PlaneDetectionActivity.this)
                                .setTitle(R.string.instant_tracking_wrong_state_title)
                                .setMessage(R.string.instant_tracking_wrong_state_interaction_message)
                                .setNeutralButton(android.R.string.ok, null)
                                .show();
                        return true;
                    }

                    Vector2<Float> screenCoordinates = new Vector2<>();
                    screenCoordinates.x = event.getX();
                    screenCoordinates.y = event.getY();

                    instantTracker.convertScreenCoordinateToPointCloudCoordinate(screenCoordinates, new InstantTracker.ScenePickingCallback() {
                        @Override
                        public void onCompletion(boolean success, Vector3<Float> result) {
                            if (success) {
                                StrokedCube strokedCube = new StrokedCube();
                                strokedCube.setXScale(0.05f);
                                strokedCube.setYScale(0.05f);
                                strokedCube.setZScale(0.05f);
                                strokedCube.setXTranslate(result.x);
                                strokedCube.setYTranslate(result.y);
                                strokedCube.setZTranslate(result.z);
                                glRenderer.setRenderablesForKey("cube-" + cubeID++, strokedCube, null);
                            } else {
                                Toast.makeText(PlaneDetectionActivity.this, "No point found at the touched position.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }, new Handler(getMainLooper()));
                }

                return true;
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
        changeStateButton = findViewById(R.id.on_change_tracker_state);
        changeStateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {

                if (instantTracker == null) {
                    Toast.makeText(PlaneDetectionActivity.this, "Tracker is not available yet", Toast.LENGTH_SHORT).show();
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

        final SeekBar heightSlider = findViewById(R.id.heightSeekBar);
        heightSlider.setMax(190);
        heightSlider.setProgress(90);
        heightSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                if (instantTracker == null) {
                    Toast.makeText(PlaneDetectionActivity.this, "Tracker is not available yet", Toast.LENGTH_SHORT).show();
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
        Log.v(TAG, "onStateChanged");
        currentTrackingState = state;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (state == InstantTrackingState.Tracking) {
                    // deviceHeightAboveGround may not be called during tracking
                    heightSettingsLayout.setVisibility(View.GONE);
                    changeStateButton.setText(R.string.instant_tracking_button_start_initialization);
                } else {
                    heightSettingsLayout.setVisibility(View.VISIBLE);
                    changeStateButton.setText(R.string.instant_tracking_button_start_tracking);
                }
            }
        });

        if (currentTrackingState == InstantTrackingState.Initializing) {
            for (int i = 0; i < cubeID; ++i) {
                glRenderer.removeRenderablesForKey("cube-" + i);
            }
            cubeID = 0;
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    getRectangle().setColor(1.0f, 0.58f, 0.16f);
                }
            });
        }
    }

    @Override
    public void onStateChangeFailed(final InstantTracker tracker, final InstantTrackingState failedState, final WikitudeError error) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(PlaneDetectionActivity.this)
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

        for (int i = 0; i < cubeID; ++i) {
            Renderable strokedCube = glRenderer.getRenderableForKey("cube-" + i);
            if (strokedCube != null) {
                strokedCube.viewMatrix = target.getViewMatrix();
            }
        }
    }

    @Override
    public void onTracked(InstantTracker tracker, InstantTarget target) {
        StrokedRectangle strokedRectangle = getRectangle();
        strokedRectangle.viewMatrix = target.getViewMatrix();

        for (int i = 0; i < cubeID; ++i) {
            Renderable strokedCube = glRenderer.getRenderableForKey("cube-" + i);
            if (strokedCube != null) {
                strokedCube.viewMatrix = target.getViewMatrix();
            }
        }
    }

    @Override
    public void onTrackingStopped(InstantTracker tracker) {
        glRenderer.removeRenderablesForKey("");

        for (int i = 0; i < cubeID; ++i) {
            Renderable strokedCube = glRenderer.getRenderableForKey("cube-" + i);
            if (strokedCube != null) {
                strokedCube.viewMatrix = null;
            }
        }
    }

    @Override
    public void onPlaneRecognized(final InstantTracker tracker, final Plane plane) {
        PlanePolygon planePolygon = new PlanePolygon();
        planePolygon.setPoints(plane.getConvexHull());
        planePolygon.viewMatrix = plane.getViewMatrix();
        switch (plane.getPlaneType()) {
            case HORIZONTAL_UPWARD:
                planePolygon.setColor(0, 0, 1);
                break;
            case VERTICAL:
                planePolygon.setColor(0.93f, 0.13f, 0.78f);
                break;
            case ARBITRARY:
                planePolygon.setColor(0.25f, 0.93f, 0.13f);
                break;
        }

        glRenderer.setRenderablesForKey("plane-" + plane.getUniqueId(), planePolygon, null);
    }

    @Override
    public void onPlaneLost(final InstantTracker tracker, final Plane plane) {
        glRenderer.removeRenderablesForKey("plane-" + plane.getUniqueId());
    }

    @Override
    public void onPlaneTracked(final InstantTracker tracker, final Plane plane) {
        PlanePolygon planePolygon = (PlanePolygon) glRenderer.getRenderableForKey("plane-" + plane.getUniqueId());
        planePolygon.viewMatrix = plane.getViewMatrix();
        planePolygon.setPoints(plane.getConvexHull());
    }

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

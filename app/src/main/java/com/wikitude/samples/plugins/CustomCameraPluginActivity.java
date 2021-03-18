package com.wikitude.samples.plugins;

import com.wikitude.NativeStartupConfiguration;
import com.wikitude.WikitudeSDK;
import com.wikitude.common.ErrorCallback;
import com.wikitude.common.WikitudeError;
import com.wikitude.common.rendering.RenderExtension;
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
import com.wikitude.tracker.TargetCollectionResource;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.WindowManager;

public class CustomCameraPluginActivity extends Activity implements ImageTrackerListener, ExternalRendering {

    static {
        System.loadLibrary("wikitudePlugins");
    }

    private static final String TAG = "CustomCameraInputPlugin";

    private WikitudeSDK wikitudeSDK;
    private FrameInputPluginModule inputModule;
    private boolean onPauseCalled = false;

    private CustomSurfaceView customSurfaceView;
    private Driver driver;
    private GLRenderer glRenderer;

    private DropDownAlert dropDownAlert;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // new instance of the WikitudeSDK with ExternalRendering
        wikitudeSDK = new WikitudeSDK(this);

        // creating configuration for the SDK
        NativeStartupConfiguration startupConfiguration = new NativeStartupConfiguration();
        startupConfiguration.setLicenseKey(WikitudeSDKConstants.WIKITUDE_SDK_KEY);

        // wikitude SDK will be created with the given configuration
        wikitudeSDK.onCreate(getApplicationContext(), this, startupConfiguration);

        // creating a new TargetCollectionResource from a .wtc file containing information about the image to track
        final TargetCollectionResource targetCollectionResource = wikitudeSDK.getTrackerManager().createTargetCollectionResource("file:///android_asset/magazine.wtc");
        wikitudeSDK.getTrackerManager().createImageTracker(targetCollectionResource, CustomCameraPluginActivity.this, null);

        // sets this activity in the plugin
        initNative();

        // register Plugin in the wikitude SDK and in the jniRegistration.cpp
        wikitudeSDK.getPluginManager().registerNativePlugins("wikitudePlugins", "customcamera", new ErrorCallback() {
            @Override
            public void onError(@NonNull WikitudeError error) {
                Log.v(TAG, "Plugin failed to load. Reason: " + error.getMessage());
            }
        });

        // alert showing which target image to scan
        dropDownAlert = new DropDownAlert(this);
        dropDownAlert.setText("Scan Target #1 (surfer) or #2 (biker):");
        dropDownAlert.addImages("surfer.png", "bike.png");
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
        glRenderer = new GLRenderer(renderExtension);
        wikitudeSDK.getCameraManager().setRenderingCorrectedFovChangedListener(glRenderer);
        customSurfaceView = new CustomSurfaceView(getApplicationContext(), glRenderer);
        driver = new Driver(customSurfaceView, 30);

        setContentView(customSurfaceView);
    }

    @Override
    public void onErrorLoadingTargets(ImageTracker tracker, WikitudeError error) {
        Log.v(TAG, "Unable to load image tracker. Reason: " + error.getMessage());
    }

    @Override
    public void onTargetsLoaded(ImageTracker tracker) {
        Log.v(TAG, "Image tracker loaded");
    }

    @Override
    public void onImageRecognized(ImageTracker tracker, final ImageTarget target) {
        Log.v(TAG, "Recognized target " + target.getName());
    }

    @Override
    public void onImageTracked(ImageTracker tracker, final ImageTarget target) {

    }

    @Override
    public void onImageLost(ImageTracker tracker, final ImageTarget target) {
        Log.v(TAG, "Lost target " + target.getName());
    }

    @Override
    public void onExtendedTrackingQualityChanged(ImageTracker tracker, final ImageTarget target, final int oldTrackingQuality, final int newTrackingQuality) {

    }

    /**
     * Called from c++ on initialization of the Plugin.
     */
    public void onInputPluginInitialized() {
        inputModule = new FrameInputPluginModule(this, getInputModuleHandle());
    }

    /**
     * Called from c++ onCameraReleased of the CameraFrameInputPluginModule.
     */
    public void onSDKCameraReleased() {
        inputModule.start();
    }

    /**
     * Called from c++ on pause of the Plugin.
     */
    public void onInputPluginPaused() {
        inputModule.stop();
        onPauseCalled = true;
    }

    /**
     * Called from c++ on resume of the Plugin.
     */
    public void onInputPluginResumed() {
        if (onPauseCalled) {
            inputModule.start();
        }
    }

    /**
     * Called from c++ on destroy of the Plugin.
     */
    public void onInputPluginDestroyed() {

    }

    private native void initNative();
    private native long getInputModuleHandle();
}

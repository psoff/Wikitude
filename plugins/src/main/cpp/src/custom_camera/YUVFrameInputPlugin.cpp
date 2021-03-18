//
//  YUVFrameInputPlugin.cpp
//
//  Created by Daniel Guttenberg on 28.05.2018.
//  Copyright © 2018 Wikitude. All rights reserved.
//


#include "YUVFrameInputPlugin.h"

#include <android/log.h>

#include <CameraParameters.hpp>
#include <PluginParameterCollection.hpp>
#include <RuntimeParameters.hpp>

#include "FrameInputPluginModule.hpp"
#include "jniHelper.h"
#include "OpenGLESScanningEffectRenderingPluginModule.hpp"


YUVFrameInputPlugin* YUVFrameInputPlugin::instance;
jobject yuvFramePluginActivity;

YUVFrameInputPlugin::YUVFrameInputPlugin() :
        Plugin("customcamera"){
    YUVFrameInputPlugin::instance = this;

    setCameraFrameInputPluginModule(std::make_unique<FrameInputPluginModule>(false /*request rendering*/, [&]{
        callInitializedJNIMethod(_sdkCameraReleasedMethodId);
    }));
}

YUVFrameInputPlugin::~YUVFrameInputPlugin() {
    JavaVMResource vm(pluginJavaVM);
    vm.env->DeleteGlobalRef(yuvFramePluginActivity);
}

/**
 * Will be called once after your Plugin was successfully added to the Wikitude Engine. Initialize your plugin here.
 */
void YUVFrameInputPlugin::initialize(const std::string& temporaryDirectory_, wikitude::sdk::PluginParameterCollection& pluginParameterCollection_) {
    JavaVMResource vm(pluginJavaVM);
    jclass customCameraPluginActivityClass = vm.env->GetObjectClass(yuvFramePluginActivity);
    _pluginInitializedMethodId = vm.env->GetMethodID(customCameraPluginActivityClass, "onInputPluginInitialized", "()V");
    _sdkCameraReleasedMethodId = vm.env->GetMethodID(customCameraPluginActivityClass, "onSDKCameraReleased", "()V");
    _pluginPausedMethodId = vm.env->GetMethodID(customCameraPluginActivityClass, "onInputPluginPaused", "()V");
    _pluginResumedMethodId = vm.env->GetMethodID(customCameraPluginActivityClass, "onInputPluginResumed", "()V");
    _pluginDestroyedMethodId = vm.env->GetMethodID(customCameraPluginActivityClass, "onInputPluginDestroyed", "()V");


    callInitializedJNIMethod(_pluginInitializedMethodId);

    wikitude::sdk::RuntimeParameters& runtimeParameters = pluginParameterCollection_.getRuntimeParameters();

    std::unique_ptr<OpenGLESScanningEffectRenderingPluginModule> renderingModule(std::make_unique<OpenGLESScanningEffectRenderingPluginModule>(pluginParameterCollection_.getTrackingParameters()));    
    renderingModule->cameraToSurfaceAngleChanged(runtimeParameters.getCameraToSurfaceAngle());
    setOpenGLESRenderingPluginModule(std::move(renderingModule));

    runtimeParameters.addCameraToSurfaceAngleChangedHandler(reinterpret_cast<std::uintptr_t>(this), [&](float cameraToSurfaceAngle_) {
        static_cast<OpenGLESScanningEffectRenderingPluginModule*>(getOpenGLESRenderingPluginModule())->cameraToSurfaceAngleChanged(cameraToSurfaceAngle_);
    });

    runtimeParameters.addCameraToSurfaceScalingChangedHandler(reinterpret_cast<std::uintptr_t>(this), [&](wikitude::sdk::Scale2D<float> cameraToSurfaceScaling_) {
        static_cast<OpenGLESScanningEffectRenderingPluginModule*>(getOpenGLESRenderingPluginModule())->cameraToSurfaceScalingChanged(cameraToSurfaceScaling_);
    });

    runtimeParameters.addSurfaceSizeChangedHandler(reinterpret_cast<std::uintptr_t>(this), [&](wikitude::sdk::Size<int> surfaceSize_) {
        static_cast<OpenGLESScanningEffectRenderingPluginModule*>(getOpenGLESRenderingPluginModule())->cameraFrameSizeChanged(surfaceSize_);
    });

    wikitude::sdk::CameraParameters& cameraParameters = pluginParameterCollection_.getCameraParameters();
    cameraParameters.addCameraToSurfaceCorrectedFieldOfViewChangedHandler(reinterpret_cast<std::uintptr_t>(this), [&](float correctedFieldOfView_) {
        static_cast<OpenGLESScanningEffectRenderingPluginModule*>(getOpenGLESRenderingPluginModule())->cameraToSurfaceCorrectedFieldOfViewChanged(correctedFieldOfView_);
    });

}

/**
 * Will be called every time the Wikitude Engine pauses.
 */
void YUVFrameInputPlugin::pause() {
    iterateEnabledPluginModules([](wikitude::sdk::PluginModule& pluginModule_) {
        pluginModule_.pause();
    });
    callInitializedJNIMethod(_pluginPausedMethodId);
}

/**
 * Will be called when the Wikitude Engine starts for the first time and after every pause.
 *
 * @param pausedTime_ the duration of the pause in milliseconds
 */
void YUVFrameInputPlugin::resume(unsigned int pausedTime_) {
    iterateEnabledPluginModules([&](wikitude::sdk::PluginModule& pluginModule_) {
        pluginModule_.resume(pausedTime_);
    });
    callInitializedJNIMethod(_pluginResumedMethodId);
}

/**
 * Will be called when the Wikitude Engine shuts down.
 */
void YUVFrameInputPlugin::destroy() {
    callInitializedJNIMethod(_pluginDestroyedMethodId);
}

/**
 * Will be called after every image recognition cycle.
 *
 * @param recognizedTargets_ list of recognized targets, empty if no target was recognized
 */
void YUVFrameInputPlugin::update(const wikitude::sdk::RecognizedTargetsBucket& recognizedTargetsBucket_) {
    static_cast<OpenGLESScanningEffectRenderingPluginModule*>(getOpenGLESRenderingPluginModule())->update(recognizedTargetsBucket_);
}

void YUVFrameInputPlugin::cameraFrameAvailable(wikitude::sdk::ManagedCameraFrame& managedCameraFrame_) {
    static_cast<OpenGLESScanningEffectRenderingPluginModule*>(getOpenGLESRenderingPluginModule())->cameraFrameAvailable(managedCameraFrame_);
}

void YUVFrameInputPlugin::callInitializedJNIMethod(jmethodID methodId_) {
    JavaVMResource vm(pluginJavaVM);
    vm.env->CallVoidMethod(yuvFramePluginActivity, methodId_);
}

/**
 * Initialize c++->java connection.
 */
extern "C" JNIEXPORT void JNICALL
Java_com_wikitude_samples_plugins_CustomCameraPluginActivity_initNative(JNIEnv* env, jobject obj) {
    env->GetJavaVM(&pluginJavaVM);
    yuvFramePluginActivity = env->NewGlobalRef(obj);
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_wikitude_samples_plugins_CustomCameraPluginActivity_getInputModuleHandle(JNIEnv*, jobject) {
    return reinterpret_cast<jlong>(YUVFrameInputPlugin::instance->getCameraFrameInputPluginModule());
}

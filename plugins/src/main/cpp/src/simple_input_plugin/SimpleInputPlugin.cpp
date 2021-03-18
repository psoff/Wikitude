//
//  SimpleInputPlugin.cpp
//
//  Created by Alexander Bendl on 02/03/17.
//  Copyright © 2017 Wikitude. All rights reserved.
//

#include "SimpleInputPlugin.h"

#include "FrameInputPluginModule.hpp"
#include "jniHelper.h"


SimpleInputPlugin* SimpleInputPlugin::instance;
jobject simpleInputPluginActivity;

SimpleInputPlugin::SimpleInputPlugin() :
        Plugin("plugin.input.yuv.simple"){
    SimpleInputPlugin::instance = this;

    setCameraFrameInputPluginModule(std::make_unique<FrameInputPluginModule>(true /*reguest rendering*/, [&]{
        callInitializedJNIMethod(_sdkCameraReleasedMethodId);
    }));
}

SimpleInputPlugin::~SimpleInputPlugin() {
    JavaVMResource vm(pluginJavaVM);
    vm.env->DeleteGlobalRef(simpleInputPluginActivity);
}

/**
 * Will be called once after your Plugin was successfully added to the Wikitude Engine. Initialize your plugin here.
 */
void SimpleInputPlugin::initialize(const std::string& temporaryDirectory_, wikitude::sdk::PluginParameterCollection& pluginParameterCollection_) {
    JavaVMResource vm(pluginJavaVM);
    jclass simpleInputPluginActivityClass = vm.env->GetObjectClass(simpleInputPluginActivity);
    _pluginInitializedMethodId = vm.env->GetMethodID(simpleInputPluginActivityClass, "onInputPluginInitialized", "()V");
    _sdkCameraReleasedMethodId = vm.env->GetMethodID(simpleInputPluginActivityClass, "onSDKCameraReleased", "()V");
    _pluginPausedMethodId = vm.env->GetMethodID(simpleInputPluginActivityClass, "onInputPluginPaused", "()V");
    _pluginResumedMethodId = vm.env->GetMethodID(simpleInputPluginActivityClass, "onInputPluginResumed", "()V");
    _pluginDestroyedMethodId = vm.env->GetMethodID(simpleInputPluginActivityClass, "onInputPluginDestroyed", "()V");


    callInitializedJNIMethod(_pluginInitializedMethodId);
}

/**
 * Will be called every time the Wikitude Engine pauses.
 */
void SimpleInputPlugin::pause() {
    callInitializedJNIMethod(_pluginPausedMethodId);
}

/**
 * Will be called when the Wikitude Engine starts for the first time and after every pause.
 *
 * @param pausedTime_ the duration of the pause in milliseconds
 */
void SimpleInputPlugin::resume(unsigned int pausedTime_) {
    callInitializedJNIMethod(_pluginResumedMethodId);
}

/**
 * Will be called when the Wikitude Engine shuts down.
 */
void SimpleInputPlugin::destroy() {
    callInitializedJNIMethod(_pluginDestroyedMethodId);
}

/**
 * Will be called after every image recognition cycle.
 *
 * @param recognizedTargets_ list of recognized targets, empty if no target was recognized
 */
void SimpleInputPlugin::update(const wikitude::sdk::RecognizedTargetsBucket& recognizedTargetsBucket_) {
    /* Intentionally Left Blank */
}

void SimpleInputPlugin::cameraFrameAvailable(wikitude::sdk::ManagedCameraFrame& managedCameraFrame_) {
    /* Intentionally Left Blank */
}

void SimpleInputPlugin::callInitializedJNIMethod(jmethodID methodId_) {
    JavaVMResource vm(pluginJavaVM);
    vm.env->CallVoidMethod(simpleInputPluginActivity, methodId_);
}

/**
 * Initialize c++->java connection.
 */
extern "C" JNIEXPORT void JNICALL
Java_com_wikitude_samples_plugins_SimpleInputPluginActivity_initNative(JNIEnv* env, jobject obj) {
    env->GetJavaVM(&pluginJavaVM);
    simpleInputPluginActivity = env->NewGlobalRef(obj);
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_wikitude_samples_plugins_SimpleInputPluginActivity_getInputModuleHandle(JNIEnv*, jobject) {
    return reinterpret_cast<jlong>(SimpleInputPlugin::instance->getCameraFrameInputPluginModule());
}
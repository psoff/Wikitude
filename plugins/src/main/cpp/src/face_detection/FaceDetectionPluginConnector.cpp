//
//  FaceDetectionPluginConnector.cpp
//  Native Examples
//
//  Created by Andreas Schacherbauer on 05/08/15.
//  Copyright (c) 2015 Wikitude. All rights reserved.
//

#include "FaceDetectionPluginConnector.h"

#include "jniHelper.h"


jobject faceDetectionActivityObj;

extern "C" JNIEXPORT void JNICALL
Java_com_wikitude_samples_plugins_FaceDetectionPluginActivity_initNative(JNIEnv* env, jobject obj, jstring databasePath_) {
    env->GetJavaVM(&pluginJavaVM);
    faceDetectionActivityObj = env->NewGlobalRef(obj);
    FaceDetectionPlugin::_databasePath = env->GetStringUTFChars(databasePath_, NULL);
}

FaceDetectionPluginConnector::FaceDetectionPluginConnector() {
    faceDetectionActivityObj = nullptr;
}

FaceDetectionPluginConnector::~FaceDetectionPluginConnector() {
    JavaVMResource vm(pluginJavaVM);
    vm.env->DeleteGlobalRef(faceDetectionActivityObj);
}

void FaceDetectionPluginConnector::faceDetected(const float* modelViewMatrix) {
    JavaVMResource vm(pluginJavaVM);
    jclass clazz = vm.env->FindClass("com/wikitude/samples/plugins/FaceDetectionPluginActivity");
    _faceDetectedId = vm.env->GetMethodID(clazz, "onFaceDetected", "([F)V");
    jfloatArray jModelViewMatrix = vm.env->NewFloatArray(16);
    vm.env->SetFloatArrayRegion(jModelViewMatrix, 0, 16, modelViewMatrix);
    vm.env->CallVoidMethod(faceDetectionActivityObj, _faceDetectedId, jModelViewMatrix);
}

void FaceDetectionPluginConnector::faceLost() {
    JavaVMResource vm(pluginJavaVM);
    jclass clazz = vm.env->FindClass("com/wikitude/samples/plugins/FaceDetectionPluginActivity");
    _faceLostId = vm.env->GetMethodID(clazz, "onFaceLost", "()V");
    vm.env->CallVoidMethod(faceDetectionActivityObj, _faceLostId);
}

void FaceDetectionPluginConnector::projectionMatrixChanged(const float* projectionMatrix) {
    if (faceDetectionActivityObj) {
        JavaVMResource vm(pluginJavaVM);
        jclass clazz = vm.env->FindClass("com/wikitude/samples/plugins/FaceDetectionPluginActivity");
        _projectionMatrixChangedId = vm.env->GetMethodID(clazz, "onProjectionMatrixChanged", "([F)V");
        jfloatArray jProjectionMatrix = vm.env->NewFloatArray(16);
        vm.env->SetFloatArrayRegion(jProjectionMatrix, 0, 16, projectionMatrix);
        vm.env->CallVoidMethod(faceDetectionActivityObj, _projectionMatrixChangedId, jProjectionMatrix);
    }
}

//
//  FaceDetectionPluginConnector.h
//  Native Examples
//
//  Created by Andreas Schacherbauer on 05/08/15.
//  Copyright (c) 2015 Wikitude. All rights reserved.
//

#ifndef FaceDetectionPluginConnector_h
#define FaceDetectionPluginConnector_h

#include <jni.h>

#include "FaceDetectionPlugin.h"


extern JavaVM* pluginJavaVM;
extern jobject faceDetectionActivityObj;

class FaceDetectionPluginConnector : public FaceDetectionPluginObserver {

public:
    FaceDetectionPluginConnector();
    virtual ~FaceDetectionPluginConnector();

    virtual void faceDetected(const float* modelViewMatrix);
    virtual void faceLost();

    virtual void projectionMatrixChanged(const float* projectionMatrix);

private:
    jmethodID _faceDetectedId;
    jmethodID _faceLostId;
    jmethodID _projectionMatrixChangedId;
};

#endif /* FaceDetectionPluginConnector_h */

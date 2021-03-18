//
//  FaceDetectionPlugin.h
//  Native Examples
//
//  Created by Alami Yacine on 29/07/15.
//  Copyright (c) 2015 Wikitude. All rights reserved.
//

#ifndef FaceDetectionPlugin_h
#define FaceDetectionPlugin_h

#include <mutex>
#include <vector>

#include <jni.h>

#include <opencv.hpp>

#include <Plugin.hpp>
#include <RuntimeParameters.hpp>


class FaceDetectionPluginObserver {
public:
    virtual ~FaceDetectionPluginObserver() {}

    virtual void faceDetected(const float* modelViewMatrix) = 0;
    virtual void faceLost() = 0;
    virtual void projectionMatrixChanged(const float* projectionMatrix) = 0;
};

class FaceDetectionPlugin : public wikitude::sdk::Plugin {

public:

    FaceDetectionPlugin(int cameraFrameWidth_, int cameraFrameHeight_, FaceDetectionPluginObserver* observer_);

    virtual ~FaceDetectionPlugin();

    void initialize(const std::string& temporaryDirectory_, wikitude::sdk::PluginParameterCollection& pluginParameterCollection_) override;
    void destroy() override;
    void cameraFrameAvailable(wikitude::sdk::ManagedCameraFrame& managedCameraFrame_) override;
    void update(const wikitude::sdk::RecognizedTargetsBucket& recognizedTargetsBucket_) override;

    void cameraToSurfaceAngleChanged(float cameraToSurfaceAngle_);

public:
    static FaceDetectionPlugin* instance;
    static std::string _databasePath;

protected:
    void convertFaceRectToModelViewMatrix(cv::Mat& frame_, cv::Rect& faceRect_, float cameraToSurfaceAngle_);
    void calculateProjection(float cameraToSurfaceAngle_, float left, float right, float bottom, float top, float near, float far);


protected:

    bool                            _isDatabaseLoaded;
    cv::Mat                         _grayFrame;
    int                             _cameraFrameWidth;
    int                             _cameraFrameHeight;
    float                           _aspectRatio;
    FaceDetectionPluginObserver*    _observer;

    wikitude::sdk::RuntimeParameters* _runtimeParameters;

    float       _cameraToSurfaceAngle;
    std::mutex  _cameraToSurfaceAngleMutex;

    cv::CascadeClassifier _cascadeDetector;
    std::vector<cv::Rect> _result;

    float _modelViewMatrix[16];
    float _projectionMatrix[16];
};

#endif /* FaceDetectionPlugin_h */

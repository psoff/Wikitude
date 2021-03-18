//
//  FaceDetectionPlugin.cpp
//  Native Examples
//
//  Created by Alami Yacine on 29/07/15.
//  Copyright (c) 2015 Wikitude. All rights reserved.
//

#include "FaceDetectionPlugin.h"

#include "jniHelper.h"

#include <PluginParameterCollection.hpp>


FaceDetectionPlugin* FaceDetectionPlugin::instance;
std::string FaceDetectionPlugin::_databasePath;

FaceDetectionPlugin::FaceDetectionPlugin(int cameraFrameWidth_, int cameraFrameHeight_, FaceDetectionPluginObserver* observer_) :
        Plugin("com.wikitude.android.FaceDetectionPlugin"),
        _isDatabaseLoaded(false),
        _grayFrame(cameraFrameHeight_, cameraFrameWidth_, CV_8UC1),
        _cameraFrameWidth(cameraFrameWidth_),
        _cameraFrameHeight(cameraFrameHeight_),
        _aspectRatio((float) cameraFrameHeight_ / (float) cameraFrameWidth_),
        _observer(observer_) {
    FaceDetectionPlugin::instance = this;
}

FaceDetectionPlugin::~FaceDetectionPlugin() {
    delete _observer;
}

void FaceDetectionPlugin::initialize(const std::string& temporaryDirectory_, wikitude::sdk::PluginParameterCollection& pluginParameterCollection_) {
    wikitude::sdk::RuntimeParameters& runtimeParameters = pluginParameterCollection_.getRuntimeParameters();
    runtimeParameters.addCameraToSurfaceAngleChangedHandler(reinterpret_cast<std::uintptr_t>(this), std::bind(&FaceDetectionPlugin::cameraToSurfaceAngleChanged, this, std::placeholders::_1));
    _runtimeParameters = &runtimeParameters;
}

void FaceDetectionPlugin::destroy() {
    /* Intentionally Left Blank */
}

void FaceDetectionPlugin::cameraFrameAvailable(wikitude::sdk::ManagedCameraFrame& cameraFrame_) {
    if (!_isDatabaseLoaded) {
        _isDatabaseLoaded = _cascadeDetector.load(_databasePath);
        if (!_isDatabaseLoaded) {
            return;
        }
    }

    wikitude::sdk::Size<int> frameSize = cameraFrame_.getColorMetadata().getPixelSize();
    const wikitude::sdk::CameraFramePlane& luminancePlane = cameraFrame_.get()[0];

    if (_grayFrame.rows != frameSize.height || _grayFrame.cols != luminancePlane.getRowStride()) {
        /*
         * The luminance data may contain row strides on android. This means that there can be a distance
         * between rows in memory. The row stride is the distance between the start of two consecutive rows
         * in bytes. When the row stride equals the frame width the data is continuous.
         *
         * OpenCV has a "step" parameter for the matrix creation which is the row stride in our case.
         * Without this OpenCV could not detect faces since it would expect continuous data which would
         * make the image unusable.
         */
        _grayFrame = cv::Mat(frameSize.height, luminancePlane.getRowStride(), CV_8UC1, const_cast<void *>(luminancePlane.getData()), luminancePlane.getRowStride());
    } else {
        std::memcpy(_grayFrame.data, luminancePlane.getData(), luminancePlane.getDataSize());
    }

    cv::Mat smallImg = cv::Mat(frameSize.height * 0.5f, luminancePlane.getRowStride() * 0.5f, CV_8UC1, luminancePlane.getRowStride() * 0.5f);

    cv::resize(_grayFrame, smallImg, smallImg.size(), CV_INTER_AREA);

    /* Depending on the device orientation, the camera frame needs to be rotated in order to detect faces in it */

    float currentCameraToSurfaceAngle;
    { // auto release scope
        std::unique_lock<std::mutex> l(_cameraToSurfaceAngleMutex);

        currentCameraToSurfaceAngle = _cameraToSurfaceAngle;
    }
    if (currentCameraToSurfaceAngle == 90) {
        cv::transpose(smallImg, smallImg);
        cv::flip(smallImg, smallImg, 1);
    } else if (currentCameraToSurfaceAngle == 180) {
        cv::flip(smallImg, smallImg, 0);
    } else if (currentCameraToSurfaceAngle == 270) {
        cv::transpose(smallImg, smallImg);
        cv::flip(smallImg, smallImg, -1);
    } else if (currentCameraToSurfaceAngle == 0) {
        // nop for landscape right
    }

    cv::Rect crop = cv::Rect(smallImg.cols / 4, smallImg.rows / 4, smallImg.cols / 2, smallImg.rows / 2);

    cv::Mat croppedImg = smallImg(crop);


    _result.clear();
    _cascadeDetector.detectMultiScale(croppedImg, _result, 1.1, 2, 0, cv::Size(20, 20));

    if (_result.size()) {
        convertFaceRectToModelViewMatrix(croppedImg, _result.at(0), currentCameraToSurfaceAngle);
        _observer->faceDetected(_modelViewMatrix);
    } else {
        _observer->faceLost();
    }
}

void FaceDetectionPlugin::update(const wikitude::sdk::RecognizedTargetsBucket& recognizedTargetsBucket_) {
    /* Intentionally Left Blank */
}

void FaceDetectionPlugin::cameraToSurfaceAngleChanged(float cameraToSurfaceAngle_) {

    { // auto release scope
        std::unique_lock<std::mutex> l(_cameraToSurfaceAngleMutex);

        _cameraToSurfaceAngle = cameraToSurfaceAngle_;
    }

    calculateProjection(_cameraToSurfaceAngle, -1.f, 1.f, -1.f, 1.f, 0.f, 500.f);
    _observer->projectionMatrixChanged(_projectionMatrix);
}

void FaceDetectionPlugin::convertFaceRectToModelViewMatrix(cv::Mat& frame_, cv::Rect& faceRect_, float cameraToSurfaceAngle_) {

    float centeredX = (float) faceRect_.x + (faceRect_.width * .5f);
    float centeredY = (float) faceRect_.y + (faceRect_.height * .5f);

    float xtmp = centeredX * 4.0f;
    float ytmp = centeredY * 4.0f;

    float x = 0.f;
    float y = 0.f;
    float z = 0.f;

    float scaleX = 0.0f;
    float scaleY = 0.0f;

    wikitude::sdk::Scale2D<float> cameraScaling = _runtimeParameters->getCameraToSurfaceScaling();


    if (cameraToSurfaceAngle_ == 90 || cameraToSurfaceAngle_ == 270) {
        y = (ytmp / _cameraFrameWidth) - 0.5f;
        scaleX = ((float) faceRect_.width / (float) frame_.rows * cameraScaling.x);
        scaleY = ((float) faceRect_.height / (float) frame_.cols);
    } else {
        y = (ytmp / _cameraFrameWidth) - 0.5f * _aspectRatio;
        scaleX = ((float) faceRect_.width / (float) frame_.cols);
        scaleY = ((float) faceRect_.height / (float) frame_.rows * cameraScaling.y);
    }

    if (cameraToSurfaceAngle_ == 90) {
        x = (xtmp / _cameraFrameWidth) - 0.5f * _aspectRatio;
    } else if (cameraToSurfaceAngle_ == 180) {
        x = 0.5f - (xtmp / _cameraFrameWidth);
    } else if (cameraToSurfaceAngle_ == 270) {
        x = 0.5f * _aspectRatio - (xtmp / _cameraFrameWidth);
    } else {
        x = (xtmp / _cameraFrameWidth) - 0.5f;
    }


    _modelViewMatrix[0] = scaleX;   _modelViewMatrix[4] = 0;        _modelViewMatrix[8]  = 0;       _modelViewMatrix[12] = x;
    _modelViewMatrix[1] = 0;        _modelViewMatrix[5] = scaleY;   _modelViewMatrix[9]  = 0;       _modelViewMatrix[13] = -y;
    _modelViewMatrix[2] = 0;        _modelViewMatrix[6] = 0;        _modelViewMatrix[10] = 1;       _modelViewMatrix[14] = z;
    _modelViewMatrix[3] = 0;        _modelViewMatrix[7] = 0;        _modelViewMatrix[11] = 0;       _modelViewMatrix[15] = 1;
}

void FaceDetectionPlugin::calculateProjection(float cameraToSurfaceAngle_, float left, float right, float bottom, float top, float near, float far) {

    if (cameraToSurfaceAngle_ == 90 || cameraToSurfaceAngle_ == 270) {
        left *= _aspectRatio;
        right *= _aspectRatio;
    } else {
        top *= _aspectRatio;
        bottom *= _aspectRatio;
    }
    _projectionMatrix[0] = 2 / (right - left);	_projectionMatrix[4] = 0;                   _projectionMatrix[8]  = 0;                      _projectionMatrix[12] = -(right + left) / (right - left);
    _projectionMatrix[1] = 0;                   _projectionMatrix[5] = 2 / (top - bottom);	_projectionMatrix[9]  = 0;                      _projectionMatrix[13] = -(top + bottom) / (top - bottom);
    _projectionMatrix[2] = 0;                   _projectionMatrix[6] = 0;                   _projectionMatrix[10] = 2 / (far - near);       _projectionMatrix[14] = -(far + near) / (far - near);
    _projectionMatrix[3] = 0;                   _projectionMatrix[7] = 0;                   _projectionMatrix[11] = 0;                      _projectionMatrix[15] = 1;
}

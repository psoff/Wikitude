//
//  BarcodePlugin.cpp
//  DevApplication
//
//  Created by Andreas Schacherbauer on 15/05/15.
//  Copyright (c) 2015 Wikitude. All rights reserved.
//

#include "BarcodePlugin.h"

#include "jniHelper.h"


jobject barcodeActivityObj;

extern "C" JNIEXPORT void JNICALL
Java_com_wikitude_samples_plugins_BarcodePluginActivity_initNative(JNIEnv* env, jobject obj) {
    env->GetJavaVM(&pluginJavaVM);
    barcodeActivityObj = env->NewGlobalRef(obj);
}

BarcodePlugin::BarcodePlugin(unsigned int cameraFrameWidth, unsigned int cameraFrameHeight) :
        Plugin("com.wikitude.android.barcodePlugin"),
        _worldNeedsUpdate(0),
        _image(cameraFrameWidth, cameraFrameHeight, "Y800", nullptr, 0),
        _jniInitialized(false) {
}

BarcodePlugin::~BarcodePlugin() {
    JavaVMResource vm(pluginJavaVM);
    vm.env->DeleteGlobalRef(barcodeActivityObj);
}

void BarcodePlugin::initialize(const std::string& temporaryDirectory_, wikitude::sdk::PluginParameterCollection& pluginParameterCollection_) {
    _imageScanner.set_config(zbar::ZBAR_NONE, zbar::ZBAR_CFG_ENABLE, 1);
}

void BarcodePlugin::destroy() {
    _image.set_data(nullptr, 0);
}

void BarcodePlugin::cameraFrameAvailable(wikitude::sdk::ManagedCameraFrame& cameraFrame_) {
    if (!_jniInitialized) {
        JavaVMResource vm(pluginJavaVM);
        jclass clazz = vm.env->FindClass("com/wikitude/samples/plugins/BarcodePluginActivity");
        _methodId = vm.env->GetMethodID(clazz, "onBarcodeDetected", "(Ljava/lang/String;)V");
        _jniInitialized = true;
    }

    const wikitude::sdk::CameraFramePlane& luminancePlane = cameraFrame_.get()[0];
    const wikitude::sdk::Size<int>& size = cameraFrame_.getColorMetadata().getPixelSize();

    int n;
    if (luminancePlane.getRowStride() == size.width) {
        _image.set_data(luminancePlane.getData(), luminancePlane.getDataSize());
        n = _imageScanner.scan(_image);
    } else {
        /*
         * The luminance data may contain row strides on android. This means that there can be a distance
         * between rows in memory. The row stride is the distance between the start of two consecutive rows
         * in bytes. When the row stride equals the frame width the data is continuous.
         *
         * Since zbar can't handle strided data we need to remove the strides before handing it over to zbar.
         * This is done by copying data with the width of the frame of each row into continuous memory and
         * giving that to zbar.
         */
        unsigned int luminanceSize = static_cast<unsigned int>(size.width * size.height);
        unsigned char luminanceData[luminanceSize];
        for (int row = 0; row < size.height; ++row) {
            std::memcpy(luminanceData + row * size.width, static_cast<const unsigned char*>(luminancePlane.getData()) + row * luminancePlane.getRowStride(), static_cast<size_t>(size.width));
        }
        _image.set_data(luminanceData, luminanceSize);
        n = _imageScanner.scan(_image);
    }

    if (n != _worldNeedsUpdate) {
        if (n) {
            JavaVMResource vm(pluginJavaVM);
            zbar::Image::SymbolIterator symbol = _image.symbol_begin();
            jstring codeContent = vm.env->NewStringUTF(symbol->get_data().c_str());
            vm.env->CallVoidMethod(barcodeActivityObj, _methodId, codeContent);

        }
    }

    _worldNeedsUpdate = n;
}

void BarcodePlugin::update(const wikitude::sdk::RecognizedTargetsBucket& recognizedTargetsBucket_) {
    /* Intentionally Left Blank */
}

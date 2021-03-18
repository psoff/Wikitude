//
//  BarCodePlugin.h
//  DevApplication
//
//  Created by Andreas Schacherbauer on 15/05/15.
//  Copyright (c) 2015 Wikitude. All rights reserved.
//

#ifndef BarCodePlugin_h
#define BarCodePlugin_h

#include <jni.h>

#include <zbar.h>

#include <Plugin.hpp>


extern JavaVM* pluginJavaVM;

class BarcodePlugin : public wikitude::sdk::Plugin {
public:
    BarcodePlugin(unsigned int cameraFrameWidth, unsigned int cameraFrameHeight);
    virtual ~BarcodePlugin();

    void initialize(const std::string& temporaryDirectory_, wikitude::sdk::PluginParameterCollection& pluginParameterCollection_) override;
    void destroy() override;

    void cameraFrameAvailable(wikitude::sdk::ManagedCameraFrame& managedCameraFrame_) override;
    void update(const wikitude::sdk::RecognizedTargetsBucket& recognizedTargetsBucket_) override;

protected:
    int _worldNeedsUpdate;

    zbar::Image         _image;
    zbar::ImageScanner  _imageScanner;

private:
    jmethodID   _methodId;
    bool        _jniInitialized;
};

#endif /*BarCodePlugin_h */

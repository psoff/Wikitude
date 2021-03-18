//
//  SimpleInputPlugin.h
//
//  Created by Alexander Bendl on 02/03/17.
//  Copyright © 2017 Wikitude. All rights reserved.
//

#ifndef SimpleInputPlugin_h
#define SimpleInputPlugin_h

#include <jni.h>

#include <Plugin.hpp>

extern JavaVM* pluginJavaVM;


class SimpleInputPlugin : public wikitude::sdk::Plugin {
public:
    SimpleInputPlugin();

    virtual ~SimpleInputPlugin();

    // from Plugin
    void initialize(const std::string& temporaryDirectory_, wikitude::sdk::PluginParameterCollection& pluginParameterCollection_) override;
    void pause() override;
    void resume(unsigned int pausedTime_) override;
    void destroy() override;
    void update(const wikitude::sdk::RecognizedTargetsBucket& recognizedTargetsBucket_) override;
    void cameraFrameAvailable(wikitude::sdk::ManagedCameraFrame& managedCameraFrame_) override;

protected:
    void callInitializedJNIMethod(jmethodID methodId_);

public:
    static SimpleInputPlugin* instance;

private:
    jmethodID _pluginInitializedMethodId;
    jmethodID _sdkCameraReleasedMethodId;
    jmethodID _pluginPausedMethodId;
    jmethodID _pluginResumedMethodId;
    jmethodID _pluginDestroyedMethodId;
};

#endif /* SimpleInputPlugin_h */

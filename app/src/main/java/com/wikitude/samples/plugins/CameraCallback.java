package com.wikitude.samples.plugins;

import java.nio.ByteBuffer;

public interface CameraCallback {

    void notifyNewCameraFrameYUV420888(ByteBuffer luminanceData, ByteBuffer chromaBlueData, ByteBuffer chromaRedData, int rowStrideLuminance, int pixelStrideChroma, int rowStrideChroma);

    void notifyNewCameraFrameNV21(byte[] data);

    void fieldOfViewChanged(float fov);

    void cameraReleased();
}

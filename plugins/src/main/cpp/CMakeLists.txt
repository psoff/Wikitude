cmake_minimum_required(VERSION 3.6)

add_library(lib_iconv STATIC IMPORTED)
set_target_properties(lib_iconv PROPERTIES IMPORTED_LOCATION ${CMAKE_CURRENT_LIST_DIR}/lib/${ANDROID_ABI}/libiconv.a)

add_library(lib_zbar STATIC IMPORTED)
set_target_properties(lib_zbar PROPERTIES IMPORTED_LOCATION ${CMAKE_CURRENT_LIST_DIR}/lib/${ANDROID_ABI}/libzbar.a)

add_library(lib_opencv_core SHARED IMPORTED)
set_target_properties(lib_opencv_core PROPERTIES IMPORTED_LOCATION ${CMAKE_CURRENT_LIST_DIR}/lib/${ANDROID_ABI}/libopencv_core.so)

add_library(lib_opencv_imgproc SHARED IMPORTED)
set_target_properties(lib_opencv_imgproc PROPERTIES IMPORTED_LOCATION ${CMAKE_CURRENT_LIST_DIR}/lib/${ANDROID_ABI}/libopencv_imgproc.so)

add_library(lib_opencv_objdetect SHARED IMPORTED)
set_target_properties(lib_opencv_objdetect PROPERTIES IMPORTED_LOCATION ${CMAKE_CURRENT_LIST_DIR}/lib/${ANDROID_ABI}/libopencv_objdetect.so)

add_library(lib_tbb SHARED IMPORTED)
set_target_properties(lib_tbb PROPERTIES IMPORTED_LOCATION ${CMAKE_CURRENT_LIST_DIR}/lib/${ANDROID_ABI}/libtbb.so)

add_library(lib_nativeSDK SHARED IMPORTED)
set_target_properties(lib_nativeSDK PROPERTIES IMPORTED_LOCATION ${WIKITUDE_NATIVE_PATH}/${ANDROID_ABI}/libnativesdk.so)

add_library(wikitudePlugins SHARED
    ${CMAKE_CURRENT_LIST_DIR}/src/jniHelper.cpp
    ${CMAKE_CURRENT_LIST_DIR}/src/JniRegistration.cpp
    ${CMAKE_CURRENT_LIST_DIR}/src/barcode/BarcodePlugin.cpp
    ${CMAKE_CURRENT_LIST_DIR}/src/face_detection/FaceDetectionPlugin.cpp
    ${CMAKE_CURRENT_LIST_DIR}/src/face_detection/FaceDetectionPluginConnector.cpp
    ${CMAKE_CURRENT_LIST_DIR}/src/simple_input_plugin/SimpleInputPlugin.cpp
    ${CMAKE_CURRENT_LIST_DIR}/src/FrameInputPluginModule.cpp
    ${CMAKE_CURRENT_LIST_DIR}/src/custom_camera/YUVFrameInputPlugin.cpp
    ${CMAKE_CURRENT_LIST_DIR}/src/custom_camera/YUVFrameShaderSourceObject.cpp
    ${CMAKE_CURRENT_LIST_DIR}/src/custom_camera/OpenGLESScanningEffectRenderingPluginModule.cpp
)

set_target_properties(wikitudePlugins PROPERTIES LIBRARY_OUTPUT_DIRECTORY "${PLUGIN_OUTPUT_PATH}/${ANDROID_ABI}")

target_include_directories(wikitudePlugins
    PRIVATE
    ${CMAKE_CURRENT_LIST_DIR}/include
    ${CMAKE_CURRENT_LIST_DIR}/include/opencv2
    ${CMAKE_CURRENT_LIST_DIR}/include/opencv2/core
    ${CMAKE_CURRENT_LIST_DIR}/include/opencv2/imgproc
    ${CMAKE_CURRENT_LIST_DIR}/include/opencv2/objdetect
    ${CMAKE_CURRENT_LIST_DIR}/include/wikitude
    ${CMAKE_CURRENT_LIST_DIR}/include/zbar
    /Users/alexanderbendl/Development/Architect/sdk-common-code/continuous_integration/../include/external/CommonCode
    /Users/alexanderbendl/Development/universal/core/continuous_integration/../include/external

    ${CMAKE_CURRENT_LIST_DIR}/src
    ${CMAKE_CURRENT_LIST_DIR}/src/barcode
    ${CMAKE_CURRENT_LIST_DIR}/src/face_detection
    ${CMAKE_CURRENT_LIST_DIR}/src/simple_input_plugin
    ${CMAKE_CURRENT_LIST_DIR}/src/custom_camera
)

target_link_libraries(wikitudePlugins
    lib_nativeSDK
    lib_zbar
    lib_iconv
    lib_opencv_objdetect
    lib_opencv_imgproc
    lib_opencv_core
    lib_tbb
    log
    GLESv2
    z
)

add_custom_command(TARGET wikitudePlugins PRE_BUILD
    COMMAND "${CMAKE_COMMAND}" -E
    copy
    "${ANDROID_NDK}/sources/cxx-stl/llvm-libc++/libs/${ANDROID_ABI}/libc++_shared.so"
    "${CMAKE_CURRENT_LIST_DIR}/../jniLibs/${ANDROID_ABI}/libc++_shared.so"
)

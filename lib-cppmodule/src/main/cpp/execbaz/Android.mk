LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE := execbaz

LOCAL_SRC_FILES :=  \
	execbaz.cpp

include $(BUILD_EXECUTABLE)

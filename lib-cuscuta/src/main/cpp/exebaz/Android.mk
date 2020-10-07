LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE := exebaz

LOCAL_SRC_FILES :=  \
	exebaz.cpp

include $(BUILD_EXECUTABLE)

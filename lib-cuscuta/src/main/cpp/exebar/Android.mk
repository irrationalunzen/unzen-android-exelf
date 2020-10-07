LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE := exebar

LOCAL_SRC_FILES :=  \
	exebar.cpp

include $(BUILD_EXECUTABLE)

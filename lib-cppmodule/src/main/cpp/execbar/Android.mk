LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE := execbar

LOCAL_SRC_FILES :=  \
	execbar.cpp

include $(BUILD_EXECUTABLE)

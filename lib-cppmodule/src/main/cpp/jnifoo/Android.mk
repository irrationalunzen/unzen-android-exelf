LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE := jnifoo

LOCAL_SRC_FILES := \
	jnifoo.cpp

include $(BUILD_SHARED_LIBRARY)

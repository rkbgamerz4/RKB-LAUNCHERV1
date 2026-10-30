# RKB LAUNCHER: Discord Social SDK (Rich Presence) JNI wrapper.
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := discord_partner_sdk
LOCAL_SRC_FILES := prebuilt/$(TARGET_ARCH_ABI)/libdiscord_partner_sdk.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := rkb_discord
LOCAL_SRC_FILES := rkb_discord.cpp
LOCAL_C_INCLUDES := $(LOCAL_PATH)/include
LOCAL_CPPFLAGS := -std=c++17 -fexceptions -frtti
LOCAL_LDLIBS := -llog
LOCAL_SHARED_LIBRARIES := discord_partner_sdk
include $(BUILD_SHARED_LIBRARY)

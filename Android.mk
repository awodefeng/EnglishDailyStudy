LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-subdir-java-files)

SRC_ROOT := java/com/xiaoxun/englishdailystudy/

LOCAL_PACKAGE_NAME := XunEnglishDailyStudy

#LOCAL_OVERRIDES_PACKAGES := Settings

LOCAL_CERTIFICATE := platform
LOCAL_JAVA_LIBRARIES :=framework

#LOCAL_PRIVILEGED_MODULE := true

LOCAL_STATIC_JAVA_LIBRARIES := \
    android-support-annotations \
    android-support-v4 \
    android-support-v7-appcompat \
    android-support-v7-recyclerview \
    Explibrary

LOCAL_SDK_VERSION := current

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res \
		frameworks/support/v7/appcompat/res

LOCAL_AAPT_FLAGS := --auto-add-overlay \
    --extra-packages android.support.v7.appcompat

include $(BUILD_PACKAGE)

include $(CLEAR_VARS)
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := Explibrary:libs/xun_explibrary.jar
include $(BUILD_MULTI_PREBUILT)

include $(call all-makefiles-under,$(LOCAL_PATH))



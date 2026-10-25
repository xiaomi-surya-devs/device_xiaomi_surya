#
# Copyright (C) 2021-2025 The LineageOS Project
#
# SPDX-License-Identifier: Apache-2.0
#

# Inherit from those products. Most specific first.
$(call inherit-product, $(SRC_TARGET_DIR)/product/core_64_bit.mk)
TARGET_SUPPORTS_OMX_SERVICE := false
$(call inherit-product, $(SRC_TARGET_DIR)/product/full_base_telephony.mk)

# Inherit from surya device
$(call inherit-product, device/xiaomi/surya/device.mk)

# Inherit some common Shinkai stuff.
$(call inherit-product, vendor/custom/config/common_full_phone.mk)
TARGET_SCREEN_WIDTH := 1080
SHINKAI_MAINTAINER := kylieeXD

PRODUCT_NAME := shinkai_surya
PRODUCT_DEVICE := surya
PRODUCT_BRAND := POCO
PRODUCT_MODEL := M2007J20CG
PRODUCT_MANUFACTURER := Xiaomi

PRODUCT_GMS_CLIENTID_BASE := android-xiaomi

PRODUCT_BUILD_PROP_OVERRIDES += \
    BuildDesc="surya_eea-user 12 RKQ1.211019.001 V14.0.5.0.SJGEUXM release-keys" \
    BuildFingerprint=POCO/surya_eea/surya:12/RKQ1.211019.001/V14.0.5.0.SJGEUXM:user/release-keys

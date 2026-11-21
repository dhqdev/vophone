# vophone: big, simple phone interface for elderly users, built into the ROM.
#
# Add this line at the end of device/<vendor>/<codename>/lineage_<codename>.mk
# (rom/build-rom.sh does it for you):
#   $(call inherit-product, vendor/vophone/vophone.mk)

# The app itself (replaces the stock launcher and phone app, see Android.bp)
PRODUCT_PACKAGES += \
    Vophone

# vophone is the default phone app; screen timeout and lock screen defaults
PRODUCT_PACKAGE_OVERLAYS += \
    vendor/vophone/overlay

# vophone's runtime permissions are granted on first boot (no permission screens for the user),
# and its one privileged permission (installing its own updates) is allowed
PRODUCT_COPY_FILES += \
    vendor/vophone/default-permissions-vophone.xml:$(TARGET_COPY_OUT_PRODUCT)/etc/default-permissions/default-permissions-vophone.xml \
    vendor/vophone/privapp-permissions-vophone.xml:$(TARGET_COPY_OUT_PRODUCT)/etc/permissions/privapp-permissions-vophone.xml

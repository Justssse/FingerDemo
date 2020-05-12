#define LOG_TAG "android.hardware.finger@1.0-service"
#include <android/hardware/finger/1.0/IFinger.h>
#include <hidl/LegacySupport.h>

using android::hardware::finger::V1_0::IFinger;
using android::hardware::defaultPassthroughServiceImplementation;

int main() {
    return defaultPassthroughServiceImplementation<IFinger>();
}

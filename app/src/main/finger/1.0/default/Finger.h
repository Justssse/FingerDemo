#ifndef ANDROID_HARDWARE_FINGER_V1_0_FINGER_H
#define ANDROID_HARDWARE_FINGER_V1_0_FINGER_H

#include <android/hardware/finger/1.0/IFinger.h>
#include <hidl/MQDescriptor.h>
#include <hidl/Status.h>

namespace android {
namespace hardware {
namespace finger {
namespace V1_0 {
namespace implementation {

using ::android::hardware::hidl_array;
using ::android::hardware::hidl_memory;
using ::android::hardware::hidl_string;
using ::android::hardware::hidl_vec;
using ::android::hardware::Return;
using ::android::hardware::Void;
using ::android::sp;

struct Finger : public IFinger {
    // Methods from IFinger follow.
    Return<void> test(const hidl_string& name, test_cb _hidl_cb) override;
    Return<void> touchSensor() override;

    // Methods from ::android::hidl::base::V1_0::IBase follow.

};

// FIXME: most likely delete, this is only for passthrough implementations
extern "C" IFinger* HIDL_FETCH_IFinger(const char* name);

}  // namespace implementation
}  // namespace V1_0
}  // namespace finger
}  // namespace hardware
}  // namespace android

#endif  // ANDROID_HARDWARE_FINGER_V1_0_FINGER_H

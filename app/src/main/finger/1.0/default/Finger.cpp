#include "Finger.h"

namespace android {
namespace hardware {
namespace finger {
namespace V1_0 {
namespace implementation {

// Methods from IFinger follow.
Return<void> Finger::test(const hidl_string& name, test_cb _hidl_cb) {
    // TODO implement
    char buf[100];
    ::memset(buf, 0x00, 100);
    ::snprintf(buf, 100, "Hello World, %s", name.c_str());
    hidl_string result(buf);

    _hidl_cb(result);
    return Void();
}

Return<void> Finger::touchSensor() {
    // TODO implement
    return Void();
}


// Methods from ::android::hidl::base::V1_0::IBase follow.

IFinger* HIDL_FETCH_IFinger(const char* /* name */) {
   return new Finger();
}

}  // namespace implementation
}  // namespace V1_0
}  // namespace finger
}  // namespace hardware
}  // namespace android

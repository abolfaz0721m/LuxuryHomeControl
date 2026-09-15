#include <jni.h>
#include <cstdint>
#include <cmath>
#include <algorithm>

namespace {

// Soft-knee saturation curve. Below `threshold` (as a fraction of full
// scale) the signal passes through unchanged; above it, the signal is
// compressed smoothly toward +/-1.0 using tanh instead of being chopped off
// abruptly. This is what lets manual gain go well above unity (loud!)
// without turning into harsh, crackly digital clipping.
constexpr float kThreshold = 0.85f;

inline float softClip(float x) {
    const float sign = x < 0.0f ? -1.0f : 1.0f;
    const float ax = std::fabs(x);
    if (ax <= kThreshold) {
        return x;
    }
    const float excess = (ax - kThreshold) / (1.0f - kThreshold);
    const float compressed = kThreshold + (1.0f - kThreshold) * std::tanh(excess);
    return sign * compressed;
}

}  // namespace

// Legacy hard-clip gain function, kept for compatibility.
extern "C"
JNIEXPORT void JNICALL
Java_com_example_wirelessmic_NativeGain_applyGain(
        JNIEnv *env,
        jobject /* this */,
        jshortArray buffer,
        jint length,
        jfloat gain) {

    jshort *samples = env->GetShortArrayElements(buffer, nullptr);
    if (samples == nullptr) {
        return;
    }

    if (gain != 1.0f) {
        for (jint i = 0; i < length; i++) {
            int32_t amplified = static_cast<int32_t>(samples[i] * gain);
            amplified = std::min(amplified, static_cast<int32_t>(INT16_MAX));
            amplified = std::max(amplified, static_cast<int32_t>(INT16_MIN));
            samples[i] = static_cast<jshort>(amplified);
        }
    }

    env->ReleaseShortArrayElements(buffer, samples, 0);
}

// Gain + soft limiter. Applies [gain] (can be well above 1.0, e.g. up to 8x)
// to each PCM16 sample, then runs the result through the soft-knee curve
// above so loud passages saturate smoothly instead of clipping harshly.
// This runs on every audio frame (hundreds of times/sec), so it's kept
// allocation-free and branch-light.
extern "C"
JNIEXPORT void JNICALL
Java_com_example_wirelessmic_NativeGain_applyGainWithLimiter(
        JNIEnv *env,
        jobject /* this */,
        jshortArray buffer,
        jint length,
        jfloat gain) {

    jshort *samples = env->GetShortArrayElements(buffer, nullptr);
    if (samples == nullptr) {
        return;
    }

    constexpr float kFullScale = 32767.0f;

    for (jint i = 0; i < length; i++) {
        // Normalize to [-1, 1], apply gain, then soft-clip.
        float normalized = (static_cast<float>(samples[i]) / kFullScale) * gain;
        float shaped = softClip(normalized);
        float outSample = shaped * kFullScale;

        outSample = std::min(outSample, kFullScale);
        outSample = std::max(outSample, -kFullScale - 1.0f);
        samples[i] = static_cast<jshort>(outSample);
    }

    env->ReleaseShortArrayElements(buffer, samples, 0);
}

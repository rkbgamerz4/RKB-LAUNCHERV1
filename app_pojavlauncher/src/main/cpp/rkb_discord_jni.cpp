/**
 * RKB Launcher — thin JNI bridge to Discord Social SDK (cdiscord.h)
 * Client ID: 1555829146490241095
 */
#include <jni.h>
#include <android/log.h>
#include <string>
#include <mutex>
#include <cstring>
#include <cstdlib>

#include "cdiscord.h"

#define LOG_TAG "RKB-DiscordJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static constexpr uint64_t kClientId = 1555829146490241095ULL;

static std::mutex gMutex;
static Discord_Client gClient;
static bool gInited = false;
static bool gReady = false;
static std::string gPendingDetails;
static std::string gPendingState;

static Discord_String makeString(const std::string& s) {
    Discord_String out{};
    out.size = s.size();
    if (out.size == 0) {
        out.ptr = nullptr;
        return out;
    }
    // SDK typically copies; keep buffer stable for call duration
    out.ptr = reinterpret_cast<uint8_t*>(const_cast<char*>(s.data()));
    return out;
}

static void applyPresenceLocked(const std::string& details, const std::string& state) {
    if (!gInited) return;

    Discord_Activity activity{};
    Discord_Activity_Init(&activity);

    Discord_ActivityTypes type = Discord_ActivityTypes_Playing;
    Discord_Activity_SetType(&activity, type);

    // Name comes from Discord application; details/state are custom lines
    std::string d = details.empty() ? "Browsing launcher" : details;
    std::string st = state.empty() ? "RKB Launcher" : state;
    Discord_String ds = makeString(d);
    Discord_String ss = makeString(st);
    Discord_Activity_SetDetails(&activity, &ds);
    Discord_Activity_SetState(&activity, &ss);

    uint64_t appId = kClientId;
    Discord_Activity_SetApplicationId(&activity, &appId);

    Discord_Client_UpdateRichPresence(
        &gClient, &activity,
        [](Discord_ClientResult* result, void* /*userData*/) {
            if (result == nullptr) {
                LOGW("UpdateRichPresence: null result");
                return;
            }
            Discord_ErrorType t = Discord_ClientResult_Type(result);
            if (t == Discord_ErrorType_None) {
                LOGI("UpdateRichPresence OK");
            } else {
                Discord_String msg{};
                Discord_ClientResult_ToString(result, &msg);
                LOGW("UpdateRichPresence err type=%d", (int)t);
            }
        },
        nullptr, nullptr);

    Discord_Activity_Drop(&activity);
    LOGI("Presence requested: %s | %s", d.c_str(), st.c_str());
}

static void onStatusChanged(Discord_Client_Status status, Discord_Client_Error error,
                            int32_t /*httpDetail*/, void* /*userData*/) {
    LOGI("Client status=%d error=%d", (int)status, (int)error);
    std::lock_guard<std::mutex> lock(gMutex);
    gReady = (status == Discord_Client_Status_Ready);
    if (gReady && !gPendingDetails.empty()) {
        applyPresenceLocked(gPendingDetails, gPendingState);
    }
}

extern "C" JNIEXPORT jboolean JNICALL
Java_net_kdt_pojavlaunch_rkb_discord_DiscordNative_nativeInit(JNIEnv*, jclass) {
    std::lock_guard<std::mutex> lock(gMutex);
    if (gInited) return JNI_TRUE;

    memset(&gClient, 0, sizeof(gClient));
    Discord_Client_Init(&gClient);

    Discord_Client_SetStatusChangedCallback(
        &gClient, onStatusChanged, nullptr, nullptr);

    Discord_Client_Connect(&gClient);
    gInited = true;
    LOGI("Discord_Client_Init + Connect done (clientId=%llu)",
         (unsigned long long)kClientId);
    return JNI_TRUE;
}

extern "C" JNIEXPORT void JNICALL
Java_net_kdt_pojavlaunch_rkb_discord_DiscordNative_nativeShutdown(JNIEnv*, jclass) {
    std::lock_guard<std::mutex> lock(gMutex);
    if (!gInited) return;
    Discord_Client_ClearRichPresence(&gClient);
    Discord_Client_Disconnect(&gClient);
    Discord_Client_Drop(&gClient);
    gInited = false;
    gReady = false;
    LOGI("Discord client shut down");
}

extern "C" JNIEXPORT void JNICALL
Java_net_kdt_pojavlaunch_rkb_discord_DiscordNative_nativeRunCallbacks(JNIEnv*, jclass) {
    Discord_RunCallbacks();
}

extern "C" JNIEXPORT void JNICALL
Java_net_kdt_pojavlaunch_rkb_discord_DiscordNative_nativeSetPresence(
        JNIEnv* env, jclass, jstring jDetails, jstring jState) {
    const char* d = jDetails ? env->GetStringUTFChars(jDetails, nullptr) : "";
    const char* s = jState ? env->GetStringUTFChars(jState, nullptr) : "";
    std::string details = d ? d : "";
    std::string state = s ? s : "";
    if (jDetails) env->ReleaseStringUTFChars(jDetails, d);
    if (jState) env->ReleaseStringUTFChars(jState, s);

    std::lock_guard<std::mutex> lock(gMutex);
    gPendingDetails = details;
    gPendingState = state;
    if (!gInited) {
        LOGW("nativeSetPresence: client not inited");
        return;
    }
    applyPresenceLocked(details, state);
}

extern "C" JNIEXPORT void JNICALL
Java_net_kdt_pojavlaunch_rkb_discord_DiscordNative_nativeClearPresence(JNIEnv*, jclass) {
    std::lock_guard<std::mutex> lock(gMutex);
    gPendingDetails.clear();
    gPendingState.clear();
    if (gInited) {
        Discord_Client_ClearRichPresence(&gClient);
        LOGI("ClearRichPresence");
    }
}

extern "C" JNIEXPORT void JNICALL
Java_net_kdt_pojavlaunch_rkb_discord_DiscordNative_nativeUpdateToken(
        JNIEnv* env, jclass, jstring jToken) {
    if (!jToken) return;
    const char* t = env->GetStringUTFChars(jToken, nullptr);
    std::string token = t ? t : "";
    env->ReleaseStringUTFChars(jToken, t);

    std::lock_guard<std::mutex> lock(gMutex);
    if (!gInited || token.empty()) return;

    Discord_String ts = makeString(token);
    Discord_Client_UpdateToken(
        &gClient,
        Discord_AuthorizationTokenType_Bearer,
        ts,
        [](Discord_ClientResult* result, void*) {
            Discord_ErrorType err = result ? Discord_ClientResult_Type(result)
                                           : Discord_ErrorType_None;
            LOGI("UpdateToken result type=%d", (int)err);
        },
        nullptr, nullptr);
    LOGI("UpdateToken submitted (len=%zu)", token.size());
}

extern "C" JNIEXPORT jboolean JNICALL
Java_net_kdt_pojavlaunch_rkb_discord_DiscordNative_nativeIsReady(JNIEnv*, jclass) {
    std::lock_guard<std::mutex> lock(gMutex);
    return gReady ? JNI_TRUE : JNI_FALSE;
}

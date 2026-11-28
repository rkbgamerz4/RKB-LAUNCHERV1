// RKB LAUNCHER - JNI wrapper around the Discord Social SDK (Rich Presence only).
// Presence is sent WITHOUT authorization: the SDK hands it to the local Discord app
// (Client::UpdateRichPresence while not connected). No OAuth, no tokens, no Client::Connect.
#include <jni.h>
#include <android/log.h>
#include <memory>
#include <string>
// The SDK header only declares the C++ wrapper; exactly one translation unit must
// define this macro to compile the wrapper bodies (they call the exported C API).
#define DISCORDPP_IMPLEMENTATION
#include "discordpp.h"

#define TAG "RkbDiscord"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static JavaVM* g_vm = nullptr;
static jclass g_class = nullptr;
static jmethodID g_onEvent = nullptr;
static std::shared_ptr<discordpp::Client> g_client;
static uint64_t g_appId = 0;

static JNIEnv* getEnv() {
    JNIEnv* env = nullptr;
    if (g_vm == nullptr) return nullptr;
    if (g_vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        if (g_vm->AttachCurrentThread(&env, nullptr) != JNI_OK) return nullptr;
    }
    return env;
}

static void emit(const std::string& type, const std::string& a, const std::string& b) {
    JNIEnv* env = getEnv();
    if (env == nullptr || g_class == nullptr || g_onEvent == nullptr) return;
    jstring jt = env->NewStringUTF(type.c_str());
    jstring ja = env->NewStringUTF(a.c_str());
    jstring jb = env->NewStringUTF(b.c_str());
    env->CallStaticVoidMethod(g_class, g_onEvent, jt, ja, jb);
    env->DeleteLocalRef(jt);
    env->DeleteLocalRef(ja);
    env->DeleteLocalRef(jb);
}

static std::string toStd(JNIEnv* env, jstring s) {
    if (s == nullptr) return "";
    const char* c = env->GetStringUTFChars(s, nullptr);
    std::string out(c ? c : "");
    if (c) env->ReleaseStringUTFChars(s, c);
    return out;
}

extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
    g_vm = vm;
    JNIEnv* env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) return JNI_ERR;
    jclass local = env->FindClass("com/movtery/zalithlauncher/utils/RkbDiscord");
    if (local == nullptr) { env->ExceptionClear(); LOGE("RkbDiscord class not found"); return JNI_VERSION_1_6; }
    g_class = reinterpret_cast<jclass>(env->NewGlobalRef(local));
    g_onEvent = env->GetStaticMethodID(g_class, "onNativeEvent",
                                       "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
    if (g_onEvent == nullptr) { env->ExceptionClear(); LOGE("onNativeEvent not found"); }
    return JNI_VERSION_1_6;
}

extern "C" JNIEXPORT void JNICALL
Java_com_movtery_zalithlauncher_utils_RkbDiscord_nativeInit(JNIEnv*, jclass, jlong appId) {
    g_appId = static_cast<uint64_t>(appId);
    g_client = std::make_shared<discordpp::Client>();
    g_client->SetApplicationId(g_appId);
}

// Reports whether the Discord app is installed ("installed" event, "true"/"false").
extern "C" JNIEXPORT void JNICALL
Java_com_movtery_zalithlauncher_utils_RkbDiscord_nativeCheckInstalled(JNIEnv*, jclass) {
    if (!g_client) return;
    g_client->IsDiscordAppInstalled([](bool installed) {
        emit("installed", installed ? "true" : "false", "");
    });
}

extern "C" JNIEXPORT void JNICALL
Java_com_movtery_zalithlauncher_utils_RkbDiscord_nativeSetPresence(JNIEnv* env, jclass,
                                                                   jstring details, jstring state, jlong startSeconds) {
    if (!g_client) return;
    discordpp::Activity activity;
    activity.SetType(discordpp::ActivityTypes::Playing);
    activity.SetName("RKB LAUNCHER");
    activity.SetDetails(toStd(env, details));
    activity.SetState(toStd(env, state));
    if (startSeconds > 0) {
        discordpp::ActivityTimestamps ts;
        ts.SetStart(static_cast<uint64_t>(startSeconds) * 1000ULL);
        activity.SetTimestamps(ts);
    }
    g_client->UpdateRichPresence(activity, [](discordpp::ClientResult r) {
        if (r.Successful()) emit("presence", "ok", "");
        else emit("error", "presence: " + r.Error(), "");
    });
}

extern "C" JNIEXPORT void JNICALL
Java_com_movtery_zalithlauncher_utils_RkbDiscord_nativeRunCallbacks(JNIEnv*, jclass) {
    discordpp::RunCallbacks();
}

extern "C" JNIEXPORT void JNICALL
Java_com_movtery_zalithlauncher_utils_RkbDiscord_nativeClear(JNIEnv*, jclass) {
    if (g_client) g_client->ClearRichPresence();
}

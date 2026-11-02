// RKB LAUNCHER - JNI wrapper around the Discord Social SDK (Rich Presence only).
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

// Never pass tokens to the log; only the Java side stores them.
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
    g_client->SetStatusChangedCallback(
        [](discordpp::Client::Status status, discordpp::Client::Error error, int32_t detail) {
            emit("status", discordpp::Client::StatusToString(status),
                 error == discordpp::Client::Error::None ? "" : std::to_string(detail));
        });
}

extern "C" JNIEXPORT void JNICALL
Java_com_movtery_zalithlauncher_utils_RkbDiscord_nativeAuthorize(JNIEnv*, jclass) {
    if (!g_client) return;
    auto client = g_client;
    uint64_t appId = g_appId;
    auto verifier = client->CreateAuthorizationCodeVerifier();
    discordpp::AuthorizationArgs args{};
    args.SetClientId(appId);
    args.SetScopes(discordpp::Client::GetDefaultPresenceScopes());
    args.SetCodeChallenge(verifier.Challenge());
    client->Authorize(args, [client, verifier, appId](discordpp::ClientResult result,
                                                      std::string code, std::string redirectUri) {
        if (!result.Successful()) { emit("error", "authorize: " + result.Error(), ""); return; }
        client->GetToken(appId, code, verifier.Verifier(), redirectUri,
            [client](discordpp::ClientResult r, std::string accessToken, std::string refreshToken,
                     discordpp::AuthorizationTokenType, int32_t, std::string) {
                if (!r.Successful()) { emit("error", "token: " + r.Error(), ""); return; }
                emit("token", accessToken, refreshToken);
                client->UpdateToken(discordpp::AuthorizationTokenType::Bearer, accessToken,
                    [client](discordpp::ClientResult r2) {
                        if (r2.Successful()) client->Connect();
                        else emit("error", "update token: " + r2.Error(), "");
                    });
            });
    });
}

extern "C" JNIEXPORT void JNICALL
Java_com_movtery_zalithlauncher_utils_RkbDiscord_nativeConnectWithToken(JNIEnv* env, jclass, jstring token) {
    if (!g_client) return;
    auto client = g_client;
    client->UpdateToken(discordpp::AuthorizationTokenType::Bearer, toStd(env, token),
        [client](discordpp::ClientResult r) {
            if (r.Successful()) client->Connect();
            else emit("error", "saved login expired: " + r.Error(), "");
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
        if (!r.Successful()) emit("error", "presence: " + r.Error(), "");
    });
}

extern "C" JNIEXPORT void JNICALL
Java_com_movtery_zalithlauncher_utils_RkbDiscord_nativeRunCallbacks(JNIEnv*, jclass) {
    discordpp::RunCallbacks();
}

extern "C" JNIEXPORT void JNICALL
Java_com_movtery_zalithlauncher_utils_RkbDiscord_nativeDisconnect(JNIEnv*, jclass) {
    if (g_client) { g_client->ClearRichPresence(); g_client->Disconnect(); }
}

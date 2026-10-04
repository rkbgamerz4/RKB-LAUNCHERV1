/**
 * RKB Launcher — JNI → Discord Social SDK
 * Client ID: 1555829146490241095
 */
#include <jni.h>
#include <android/log.h>
#include <pthread.h>
#include <string.h>
#include <stdlib.h>
#include "cdiscord.h"

#define LOG_TAG "RKB-DiscordJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

static const uint64_t kClientId = 1555829146490241095ULL;

static pthread_mutex_t gMutex = PTHREAD_MUTEX_INITIALIZER;
static Discord_Client gClient;
static int gInited = 0;
static int gReady = 0;
static char gPendingDetails[256];
static char gPendingState[128];

static Discord_String makeStr(const char* s) {
    Discord_String out;
    out.ptr = (uint8_t*)(void*)s;
    out.size = s ? strlen(s) : 0;
    return out;
}

static void onUpdatePresence(Discord_ClientResult* result, void*) {
    LOGI("UpdateRichPresence type=%d", result ? (int)Discord_ClientResult_Type(result) : -1);
}

static void onUpdateToken(Discord_ClientResult* result, void*) {
    LOGI("UpdateToken type=%d", result ? (int)Discord_ClientResult_Type(result) : -1);
}

static void applyPresenceLocked(const char* details, const char* state) {
    if (!gInited) return;

    Discord_Activity activity;
    memset(&activity, 0, sizeof(activity));
    Discord_Activity_Init(&activity);
    Discord_Activity_SetType(&activity, Discord_ActivityTypes_Playing);

    char dbuf[256];
    char sbuf[128];
    strncpy(dbuf, (details && details[0]) ? details : "Browsing launcher", sizeof(dbuf) - 1);
    dbuf[sizeof(dbuf) - 1] = 0;
    strncpy(sbuf, (state && state[0]) ? state : "RKB Launcher", sizeof(sbuf) - 1);
    sbuf[sizeof(sbuf) - 1] = 0;

    Discord_String ds = makeStr(dbuf);
    Discord_String ss = makeStr(sbuf);
    Discord_Activity_SetDetails(&activity, &ds);
    Discord_Activity_SetState(&activity, &ss);
    uint64_t appId = kClientId;
    Discord_Activity_SetApplicationId(&activity, &appId);

    Discord_Client_UpdateRichPresence(&gClient, &activity, onUpdatePresence, NULL, NULL);
    Discord_Activity_Drop(&activity);
    LOGI("Presence: %s | %s", dbuf, sbuf);
}

static void onStatusChanged(Discord_Client_Status status, Discord_Client_Error error,
                            int32_t, void*) {
    LOGI("status=%d error=%d", (int)status, (int)error);
    pthread_mutex_lock(&gMutex);
    gReady = (status == Discord_Client_Status_Ready) ? 1 : 0;
    if (gReady && gPendingDetails[0]) {
        applyPresenceLocked(gPendingDetails, gPendingState);
    }
    pthread_mutex_unlock(&gMutex);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_net_kdt_pojavlaunch_rkb_discord_DiscordNative_nativeInit(JNIEnv*, jclass) {
    pthread_mutex_lock(&gMutex);
    if (gInited) {
        pthread_mutex_unlock(&gMutex);
        return JNI_TRUE;
    }
    memset(&gClient, 0, sizeof(gClient));
    gPendingDetails[0] = 0;
    gPendingState[0] = 0;
    Discord_Client_Init(&gClient);
    Discord_Client_SetStatusChangedCallback(&gClient, onStatusChanged, NULL, NULL);
    Discord_Client_Connect(&gClient);
    gInited = 1;
    pthread_mutex_unlock(&gMutex);
    LOGI("Client Init+Connect");
    return JNI_TRUE;
}

extern "C" JNIEXPORT void JNICALL
Java_net_kdt_pojavlaunch_rkb_discord_DiscordNative_nativeShutdown(JNIEnv*, jclass) {
    pthread_mutex_lock(&gMutex);
    if (gInited) {
        Discord_Client_ClearRichPresence(&gClient);
        Discord_Client_Disconnect(&gClient);
        Discord_Client_Drop(&gClient);
        gInited = 0;
        gReady = 0;
    }
    pthread_mutex_unlock(&gMutex);
}

extern "C" JNIEXPORT void JNICALL
Java_net_kdt_pojavlaunch_rkb_discord_DiscordNative_nativeRunCallbacks(JNIEnv*, jclass) {
    Discord_RunCallbacks();
}

extern "C" JNIEXPORT void JNICALL
Java_net_kdt_pojavlaunch_rkb_discord_DiscordNative_nativeSetPresence(
        JNIEnv* env, jclass, jstring jDetails, jstring jState) {
    const char* d = jDetails ? env->GetStringUTFChars(jDetails, NULL) : "";
    const char* s = jState ? env->GetStringUTFChars(jState, NULL) : "";
    pthread_mutex_lock(&gMutex);
    strncpy(gPendingDetails, d ? d : "", sizeof(gPendingDetails) - 1);
    gPendingDetails[sizeof(gPendingDetails) - 1] = 0;
    strncpy(gPendingState, s ? s : "", sizeof(gPendingState) - 1);
    gPendingState[sizeof(gPendingState) - 1] = 0;
    if (gInited) applyPresenceLocked(gPendingDetails, gPendingState);
    pthread_mutex_unlock(&gMutex);
    if (jDetails && d) env->ReleaseStringUTFChars(jDetails, d);
    if (jState && s) env->ReleaseStringUTFChars(jState, s);
}

extern "C" JNIEXPORT void JNICALL
Java_net_kdt_pojavlaunch_rkb_discord_DiscordNative_nativeClearPresence(JNIEnv*, jclass) {
    pthread_mutex_lock(&gMutex);
    gPendingDetails[0] = 0;
    gPendingState[0] = 0;
    if (gInited) Discord_Client_ClearRichPresence(&gClient);
    pthread_mutex_unlock(&gMutex);
}

extern "C" JNIEXPORT void JNICALL
Java_net_kdt_pojavlaunch_rkb_discord_DiscordNative_nativeUpdateToken(
        JNIEnv* env, jclass, jstring jToken) {
    if (!jToken) return;
    const char* t = env->GetStringUTFChars(jToken, NULL);
    if (!t) return;
    char tokenBuf[512];
    strncpy(tokenBuf, t, sizeof(tokenBuf) - 1);
    tokenBuf[sizeof(tokenBuf) - 1] = 0;
    env->ReleaseStringUTFChars(jToken, t);

    pthread_mutex_lock(&gMutex);
    if (gInited && tokenBuf[0]) {
        Discord_String ts = makeStr(tokenBuf);
        Discord_Client_UpdateToken(&gClient, Discord_AuthorizationTokenType_Bearer, ts,
                                   onUpdateToken, NULL, NULL);
        LOGI("UpdateToken len=%d", (int)strlen(tokenBuf));
    }
    pthread_mutex_unlock(&gMutex);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_net_kdt_pojavlaunch_rkb_discord_DiscordNative_nativeIsReady(JNIEnv*, jclass) {
    pthread_mutex_lock(&gMutex);
    jboolean r = gReady ? JNI_TRUE : JNI_FALSE;
    pthread_mutex_unlock(&gMutex);
    return r;
}

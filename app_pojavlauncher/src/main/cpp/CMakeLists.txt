cmake_minimum_required(VERSION 3.22.1)
project(rkb_discord_jni)

add_library(rkb_discord_jni SHARED rkb_discord_jni.cpp)

target_include_directories(rkb_discord_jni PRIVATE
    ${CMAKE_SOURCE_DIR}/discord/include
)

# Prebuilt Social SDK from jniLibs
add_library(discord_partner_sdk SHARED IMPORTED)
set_target_properties(discord_partner_sdk PROPERTIES
    IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libdiscord_partner_sdk.so
)

find_library(log-lib log)
find_library(android-lib android)

target_link_libraries(rkb_discord_jni
    discord_partner_sdk
    ${log-lib}
    ${android-lib}
)

target_compile_features(rkb_discord_jni PRIVATE cxx_std_17)

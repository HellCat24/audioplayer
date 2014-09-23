
#include <string.h>
#include <jni.h>
#include "gsm.h"
#include <android/log.h>

#include <stdlib.h>

#define  LOG_TAG    "GSMjni"
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG  , LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO   , LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN   , LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR  , LOG_TAG, __VA_ARGS__)

static gsm handle;

JNIEXPORT void JNICALL Java_com_tac_kulik_codec_KGSMCodec_initGSM(JNIEnv* env, jobject thiz )
{
    if (handle != 0 ) {
        LOGI("Destroy Handle");
        gsm_destroy(handle);
    }
    LOGI("Create Handle");
    if (!(handle = gsm_create())) LOGE("Error creating gsm");

}

JNIEXPORT void JNICALL Java_com_tac_kulik_codec_KGSMCodec_encode(JNIEnv* env, jobject thiz, jbyteArray input, jbyteArray output, jint frameCount) {
    char* b = (char*) (*env)->GetPrimitiveArrayCritical(env, input, NULL);
    char* bout = (char*) (*env)->GetPrimitiveArrayCritical(env, output, NULL);
//    jbyte* b = env->GetByteArrayElements(input, NULL);
//     jbyte* b = (jbyte*) (*env)->GetByteArrayElements(input, NULL);
//    jbyte* bout = env->GetByteArrayElements(output, NULL);
    LOGI("startEncode");
    int i = 0;
    for (;i < frameCount; i++) {
        LOGI("Encode 1");
        gsm_encode(handle, bout, b);
        LOGI("Encode 2");
        gsm_encode(handle, bout + 33, b + 160);
        LOGI("Encoded");
        b = b + 320;
        bout = bout + 33 + 32;
    }
//    (*env)->ReleasePrimitiveArrayCritical(env, input, b, 0);
//    (*env)->ReleasePrimitiveArrayCritical(env, output, bout, 0);

}

//{
//    jbyte *b = (jbyte *)env->GetByteArrayElements(input, NULL);
//    jsize len = (*env)->GetArrayLength(env, arr);

//    gsm handle;
//    gsm_frame buf;
//    gsm_signal sample[160];
//    int cc, soundfd;

//    if (!(handle = gsm_create())) LOGE("Error creating gsm");

//    int sample_counter = 0;
//    for (int i = 0; i < len; i+= sizeof buf)

//            if (cc != sizeof buf) error...

//                    if (gsm_decode(handle, buf, sample) < 0) error...

//                    if (write(soundfd, sample, sizeof sample) != sizeof sample) error...

//        }

//    gsm_destroy(handle);

//}
//record() {
//    /* read from soundfd, write compressed to standard output */

//    if (!(handle = gsm_create())) error...

//            while (cc = read(soundfd, sample, sizeof sample)) {

//        if (cc != sizeof sample) error...

//                gsm_encode(handle, sample, buf);

//        if (write(1, (char *)buf, sizeof buf) != sizeof sample)

//            error...

//    }

//        gsm_destroy(handle);

//}
//// release it
//env->ReleaseByteArrayElements(jbBase, b, 0 );
//return 0;
//}

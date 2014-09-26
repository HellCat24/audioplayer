
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

void initGSM() {

}

JNIEXPORT void JNICALL Java_com_tac_kulik_codec_KGSMCodec_initGSM(JNIEnv* env, jobject thiz )
{
     if (handle != 0 ) {
           LOGI("Destroy Handle");
           gsm_destroy(handle);
       }
       LOGI("Create Handle");
       if (!(handle = gsm_create())) LOGE("Error creating gsm");

       int valueP=1;
        int      f_fast     = 0;         /* use faster fpt algorithm      (-F) */
        int      f_verbose  = 0;         /* debugging                     (-V) */
        int      f_ltp_cut  = 0;         /* LTP cut-off margin            (-   */
       if(gsm_option(handle,GSM_OPT_WAV49,&valueP) == -1) {
           LOGE("error setting gsm_option for WAV49 format. Recompile gsm library with -DWAV49 option and relink sox");
           return 0;
       }
        if(gsm_option(handle, GSM_OPT_FAST, &f_fast)) {
            LOGE("GSM_OPT_FAST wrong");
            return 0;
        }
        if (gsm_option(handle, GSM_OPT_VERBOSE, &f_verbose)) {
            LOGE("GSM_OPT_VERBOSE wrong");
            return 0;
        }
        if (gsm_option(handle, GSM_OPT_LTP_CUT, &f_ltp_cut)) {
            LOGE("GSM_OPT_LTP_CUT wrong");
            return 0;
        }
}

JNIEXPORT void JNICALL Java_com_tac_kulik_codec_KGSMCodec_encode(JNIEnv* env, jobject thiz, jbyteArray input, jbyteArray output, jint frameCount) {
    gsm_signal* cInput =(gsm_signal*) (*env)->GetByteArrayElements(env,input, NULL);
    if (cInput == NULL) {
        LOGE("input dnt retrived");
    }
    char* initialInput = cInput;
    char* cOutput = (char*) (*env)->GetDirectBufferAddress(env, output);
    if (cOutput == NULL) {
        LOGE("output dnt retrived");
    }
    int i = 0;
    for (;i < frameCount; i++) {
        gsm_encode(handle, cInput, cOutput);
        cInput = (cInput) + 160;
        cOutput = (cOutput) + 32;
        gsm_encode(handle, cInput, cOutput);
        cInput = (cInput) + 160;
        cOutput = (cOutput) + 33;

    }
    (*env)->ReleaseByteArrayElements(env, input, initialInput, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_com_tac_kulik_codec_KGSMDecoder_initGSM(JNIEnv* env, jobject thiz )
{
     if (handle != 0 ) {
           LOGI("Destroy Handle");
           gsm_destroy(handle);
       }
       LOGI("Create Handle");
       if (!(handle = gsm_create())) LOGE("Error creating gsm");

       int valueP=1;
        int      f_fast     = 0;         /* use faster fpt algorithm      (-F) */
        int      f_verbose  = 0;         /* debugging                     (-V) */
        int      f_ltp_cut  = 0;         /* LTP cut-off margin            (-   */
       if(gsm_option(handle,GSM_OPT_WAV49,&valueP) == -1) {
           LOGE("error setting gsm_option for WAV49 format. Recompile gsm library with -DWAV49 option and relink sox");
           return 0;
       }
        if(gsm_option(handle, GSM_OPT_FAST, &f_fast)) {
            LOGE("GSM_OPT_FAST wrong");
            return 0;
        }
        if (gsm_option(handle, GSM_OPT_VERBOSE, &f_verbose)) {
            LOGE("GSM_OPT_VERBOSE wrong");
            return 0;
        }
        if (gsm_option(handle, GSM_OPT_LTP_CUT, &f_ltp_cut)) {
            LOGE("GSM_OPT_LTP_CUT wrong");
            return 0;
        }
}

JNIEXPORT void JNICALL Java_com_tac_kulik_codec_KGSMDecoder_decode(JNIEnv* env, jobject thiz, jbyteArray input, jbyteArray output, jint frameCount) {
    char* cInput = (*env)->GetByteArrayElements(env,input, NULL);
    if (cInput == NULL) {
        LOGE("input dnt retrived");
    }
    char* initialInput = cInput;
    char* cOutput = (char*) (*env)->GetDirectBufferAddress(env, output);
    if (cOutput == NULL) {
        LOGE("output dnt retrived");
    }
    int i = 0;
    for (;i < frameCount; i++) {
        if (gsm_decode(handle, cInput, cOutput) < 0) printf("fail to decode\n");
        cInput = (cInput) + 33;
        cOutput = (cOutput) + 160 * sizeof(gsm_signal); ;
        if (gsm_decode(handle, cInput, cOutput) < 0) printf("fail to decode\n");
        cInput = (cInput) + 32;
        cOutput = (cOutput) + 160 * sizeof(gsm_signal);
    }
        (*env)->ReleaseByteArrayElements(env, input, initialInput, JNI_ABORT);
}
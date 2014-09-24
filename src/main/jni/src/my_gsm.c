
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
//    jbyte* cInput = (*env)->GetByteArrayElements(env,input, NULL);
//    gsm_signal* cInput = (gsm_signal*) (*env)->GetPrimitiveArrayCritical(env, input, NULL);
    gsm_signal* cInput = (*env)->GetByteArrayElements(env,input, NULL);
    if (cInput == NULL) {
        LOGE("input dnt retrived");
    }
    char* initialInput = cInput;
    gsm_frame* cOutput = (gsm_frame*) (*env)->GetDirectBufferAddress(env, output);
    if (cOutput == NULL) {
        LOGE("output dnt retrived");
    }
//    char* cOutput = (char*) (*env)->GetPrimitiveArrayCritical(env, output, NULL);
//    jbyte* cOutput = (*env)->GetByteArrayElements(env, output, NULL);
//    printf("%d ", (jbyte[])* cOutput);
//        LOGE("output dnt retrived --%d", sizeof(gsm_signal));
//    LOGE( sizeof(gsm_frame));
//    jbyte* b = env->GetByteArrayElements(input, NULL);
//     jbyte* b = (jbyte*) (*env)->GetByteArrayElements(input, NULL);
//    jbyte* bout = env->GetByteArrayElements(output, NULL);
    int i = 0;
    for (;i < frameCount; i++) {
        gsm_encode(handle, cInput, cOutput);
        gsm_encode(handle, cInput + 160 * sizeof(gsm_signal), cOutput + 33);
        cInput = cInput + 160 * 2 * sizeof(gsm_signal);
        cOutput = cOutput + 33 + 32;
    }
        (*env)->ReleaseByteArrayElements(env, input, initialInput, JNI_ABORT);
//        env->ReleaseByteArrayElements(env, output, cOutput, JNI_ABORT);
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

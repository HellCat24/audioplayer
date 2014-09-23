

//#include <stdio.h>
//#include "gsm.h"

//#define  LOGI(...)    printf(__VA_ARGS__);
//#define  LOGE(...)    printf(__VA_ARGS__);

//#include <config.h>
//#define	GSM_OPT_WAV49		4
//#ifdef STDC_HEADERS
//# include <stdlib.h>
//#endif
//#ifdef HAVE_UNISTD_H
//# include <unistd.h>
//#endif
//#ifdef HAVE_SYS_TIME_H
//# include <sys/time.h>
//#endif
//#ifdef HAVE_FCNTL_H
//# include <fcntl.h>
//#endif
//#include <signal.h>
//#include <sys/stat.h>

//static FILE *outFile;

//void* memcpy_index(void *s1, const void *s2, size_t index, size_t n) {
//    s2 = ((char*)s2)+index;
//    return memcpy(s1, s2,n);
//}

///**
// * @brief processBuffer
// * @param buffer
// * @param handle
// * @return least in the old buffer that non decoded
// */
////int processBuffer(char* buffer, gsm handle)
////{
////    gsm_frame buf;
////    gsm_signal sample[160];
////    for (int i = 0; i <= sizeof buffer; i+=sizeof buf)
////    {

////        if (gsm_decode(handle, buf, sample) < 0) printf("fail to decode\n");

////        if (fwrite(sample, sizeof(short), sizeof sample, outFile) != sizeof sample) printf("fail to play\n");
////    }
////}

//int main(void)
//{
//    //    jbyte *b = (jbyte *)env->GetByteArrayElements(input, NULL);
//    //      jsize len = (*env)->GetArrayLength(env, arr);
//    FILE *fp;
//    gsm handle;
////    gsm_frame buf;
////    gsm_signal sample[160];
//    int cc, soundfd;

//    //    fp = fopen("/home/kulik/projects/Notate/notateolearis/researches/audio/app/src/main/assets/testfile.attch","r"); // read mode
//    fp = fopen("/home/kulik/projects/Notate/notateolearis/researches/audio/app/src/main/assets/gsmfile.gsm","r"); // read mode
//    outFile = fopen("/home/kulik/projects/Notate/notateolearis/researches/audio/app/src/main/assets/gsmfile.dec","wb"); // read mode

//    if ( fp == NULL )
//    {
//        perror("Error while opening the file.\n");
//    }

//    long bufsize;
//    char *source = NULL;

//    if (fp != NULL) {
//        /* Go to the end of the file. */
//        if (fseek(fp, 0L, SEEK_END) == 0) {
//            /* Get the size of the file. */
//            bufsize = ftell(fp);
//            if (bufsize == -1) { /* Error */ }

//            /* Allocate our buffer to that size. */
//            source = malloc(sizeof(char) * (bufsize + 1));

//            /* Go back to the start of the file. */
//            if (fseek(fp, 0L, SEEK_SET) != 0) { /* Error */ }

//            /* Read the entire file into memory. */
//            size_t newLen = fread(source, sizeof(char), bufsize, fp);
//            if (newLen == 0) {
//                fputs("Error reading file", stderr);
//            } else {
//                source[++newLen] = '\0'; /* Just to be safe. */
//            }
//        }
//        fclose(fp);

//    }
////#define BUFFERSIZE 330

////    unsigned char* buffer = malloc(sizeof(char) * BUFFERSIZE);

//    if (!(handle = gsm_create())) LOGE("Error creating gsm");
//    int valueP=1;

//    int sample_counter = 0;
////    samples = read_header();
////    int data_size = ((samples + (2 * BLOCK_SIZE - 1)) / (2 * BLOCK_SIZE));
////    data_size *= 2 * sizeof (frame) - 1;

//    if(gsm_option(handle,GSM_OPT_WAV49,&valueP) == -1){
//        printf("error setting gsm_option for WAV49 format. Recompile gsm library with -DWAV49 option and relink sox");
//        return 0;
//    }

//    gsm_frame buf;
//    gsm_signal sample[160];
//    gsm_signal *gsmsample;
//    for (long i = 60; i < bufsize; i+= 65)
//    {
//        memcpy_index(buf, source, i, sizeof buf);

//        if (gsm_decode(handle, buf, sample) < 0) printf("fail to decode\n");
//        if (fwrite(sample, sizeof( char), sizeof sample, outFile) != sizeof sample) printf("fail to play\n");

//        memcpy_index(buf, source, i+33, sizeof buf);
//        if (gsm_decode(handle, buf, sample) < 0) printf("fail to decode\n");
//        if (fwrite(sample, sizeof( char), sizeof sample, outFile) != sizeof sample) printf("fail to play\n");

//    }



////    for (long i = 0; i < bufsize; i+= BUFFERSIZE)
////    {
////        // iterate by buffer
////        memcpy_index(buffer, source, i, BUFFERSIZE);

////        processBuffer(buffer, handle);
////    }
//    gsm_destroy(handle);
//    free(source);
//    fclose(outFile);
//    return 0;
//}


///* makewave.c -- Read Sun .au and write GSM-containing MS wave file */

///*
// *  Copyright (C) 1996 Jeffrey Chilton
// *
// *  Permission is granted to anyone to make or distribute copies of
// *  this program, in any medium, provided that the copyright notice
// *  and permission notice are preserved, and that the distributor
// *  grants the recipient permission for further redistribution as
// *  permitted by this notice.
// *
// *  Author's E-mail address:  jwc@chilton.com
// *
// */

//#include <stdio.h>

//#include "gsm.h"

//extern unsigned short MuLaw_Linear[];

//#define MULAW_ZERO 255

//int F_wav_fmt = 1;

//long read_header(FILE *);
//int write_header(long, FILE *);

//#define BLOCK_SIZE 160

//main(argc, argv)
//int argc;
//char *argv[];
//{
//    register int i;
//    FILE *sfp;
//    FILE *dfp;
//    gsm handle;
//    long samples;
//    long data_size;
//    long total_out;
//    unsigned char mulaw[2 * BLOCK_SIZE];
//    gsm_signal linear[2 * BLOCK_SIZE];
//    gsm_frame frame;
//    int	out_size;
//    int rc;

//    sfp = fopen("/home/kulik/projects/Notate/notateolearis/researches/audio/app/src/main/assets/gsmfile.attch", "r");
//    if (!sfp)
//    {
//    perror("open fails");
//    exit(1);
//    }

//    /* Read .au file header info and calulate output size */

//    samples = read_header(sfp);
//    data_size = ((samples + (2 * BLOCK_SIZE - 1)) / (2 * BLOCK_SIZE));
//    data_size *= 2 * sizeof (frame) - 1;

///*
//fprintf(stderr, "samples from header: 0x%x\n", samples);
//fprintf(stderr, "calculated data size: 0x%x\n", data_size);
//*/

//    dfp = stdout;

//    /* Create the GSM codec object and option it for wave framing */

//    handle = gsm_create();
//    if (!handle)
//    {
//    perror("cannot create gsm codec");
//    exit(1);
//    }

//    (void )gsm_option(handle, GSM_OPT_WAV49, &F_wav_fmt);

//    /* Write the .wav file header */

//    rc = write_header(data_size, dfp);
//    if (rc)
//    {
//    perror("error writing header");
//    exit(1);
//    }

//    /* Compress the audio */

//    total_out = 0;
//    while (samples > 0)
//    {
//    /* Read two frames worth of samples and convert to linear */

//    rc = fread(mulaw, (size_t )1, sizeof (mulaw), sfp);
//    if (rc < 0)
//    {
//        perror("error reading input");
//        exit(1);
//    }
//    samples -= rc;
//    if (rc < sizeof (mulaw))
//    {
//        memset((char *)mulaw + rc, MULAW_ZERO, sizeof (mulaw) - rc);
//    }
//    for (i = 0; i < sizeof (mulaw); i++)
//    {
//        linear[i] = MuLaw_Linear[mulaw[i]];
//    }

//    /* Encode the even half and write short (32-byte) frame */

//    gsm_encode(handle, &linear[0], frame);
//    out_size = sizeof (frame) - 1;
//    rc = fwrite(frame, (size_t )1, out_size, dfp);
//    if (rc != out_size)
//    {
//        perror("error writing output");
//        exit(1);
//    }
//    total_out += rc;

//    /* Encode the odd half and write long (33-byte) frame */

//    gsm_encode(handle, &linear[160], frame);
//    out_size = sizeof (frame);
//    rc = fwrite(frame, (size_t )1, out_size, dfp);
//    if (rc != out_size)
//    {
//        perror("error writing output");
//        exit(1);
//    }
//    total_out += rc;
//    }

//    /* Pad output to even number of bytes */

//    if (total_out & 0x1)
//    {
//    frame[0] = 0x00;
//    rc = fwrite(frame, (size_t )1, 1, dfp);
//    if (rc != 1)
//    {
//        perror("error writing output");
//        exit(1);
//    }
//    total_out += rc;
//    }

///*
//fprintf(stderr, "total bytes written: 0x%lx\n", total_out);
//*/

//    /* Clean up */

//    gsm_destroy(handle);

//}

///* read_header - read Sun .au file header */

//static long
//getlong(fp)
//FILE *fp;
//{
//    long  l;

//    l = 0;
//    l = (l << 8) | 0xFF & getc(fp);
//    l = (l << 8) | 0xFF & getc(fp);
//    l = (l << 8) | 0xFF & getc(fp);
//    l = (l << 8) | 0xFF & getc(fp);

//    return l;

//}

//#define	AU_FILE_MAGIC 0x2e736e64
//#define	AU_FILE_MULAW_8 1
//#define SAMPLE_RATE 8000
//#define SAMPLE_CHANNELS 1

//long
//read_header(fp)
//FILE *fp;
//{
//    long magic;
//    long header_size;
//    long data_size;
//    long code;
//    long rate;
//    long n_chan;
//    int rc;

//    magic = getlong(fp);
//    if (magic != AU_FILE_MAGIC)
//    {
//    fprintf(stderr, "input is not an audio file\n");
//    rc = -1;
//    goto out;
//    }

//    header_size = getlong(fp);
//    data_size = getlong(fp);
//    code = getlong(fp);
//    rate = getlong(fp);
//    n_chan = getlong(fp);

//    header_size -= 24;
//    while (header_size)
//    {
//    (void )getc(fp);
//    header_size--;
//    }

//    if (code != AU_FILE_MULAW_8 ||
//    rate != SAMPLE_RATE ||
//    n_chan != SAMPLE_CHANNELS)
//    {
//    fprintf(stderr, "input must by mu-law 8KHz mono\n");
//    rc = 01;
//    goto out;
//    }

//    rc = data_size;

//out:

//    return rc;

//}

//unsigned short MuLaw_Linear[] = {
//    33280, 34308, 35336, 36364, 37393, 38421, 39449, 40477,
//    41505, 42534, 43562, 44590, 45618, 46647, 47675, 48703,
//    49474, 49988, 50503, 51017, 51531, 52045, 52559, 53073,
//    53587, 54101, 54616, 55130, 55644, 56158, 56672, 57186,
//    57572, 57829, 58086, 58343, 58600, 58857, 59114, 59371,
//    59628, 59885, 60142, 60399, 60656, 60913, 61171, 61428,
//    61620, 61749, 61877, 62006, 62134, 62263, 62392, 62520,
//    62649, 62777, 62906, 63034, 63163, 63291, 63420, 63548,
//    63645, 63709, 63773, 63838, 63902, 63966, 64030, 64095,
//    64159, 64223, 64287, 64352, 64416, 64480, 64544, 64609,
//    64657, 64689, 64721, 64753, 64785, 64818, 64850, 64882,
//    64914, 64946, 64978, 65010, 65042, 65075, 65107, 65139,
//    65163, 65179, 65195, 65211, 65227, 65243, 65259, 65275,
//    65291, 65308, 65324, 65340, 65356, 65372, 65388, 65404,
//    65416, 65424, 65432, 65440, 65448, 65456, 65464, 65472,
//    65480, 65488, 65496, 65504, 65512, 65520, 65528,     0,
//    32256, 31228, 30200, 29172, 28143, 27115, 26087, 25059,
//    24031, 23002, 21974, 20946, 19918, 18889, 17861, 16833,
//    16062, 15548, 15033, 14519, 14005, 13491, 12977, 12463,
//    11949, 11435, 10920, 10406,  9892,  9378,  8864,  8350,
//     7964,  7707,  7450,  7193,  6936,  6679,  6422,  6165,
//     5908,  5651,  5394,  5137,  4880,  4623,  4365,  4108,
//     3916,  3787,  3659,  3530,  3402,  3273,  3144,  3016,
//     2887,  2759,  2630,  2502,  2373,  2245,  2116,  1988,
//     1891,  1827,  1763,  1698,  1634,  1570,  1506,  1441,
//     1377,  1313,  1249,  1184,  1120,  1056,   992,   927,
//      879,   847,   815,   783,   751,   718,   686,   654,
//      622,   590,   558,   526,   494,   461,   429,   397,
//      373,   357,   341,   325,   309,   293,   277,   261,
//      245,   228,   212,   196,   180,   164,   148,   132,
//      120,   112,   104,    96,    88,    80,    72,    64,
//       56,    48,    40,    32,    24,    16,    8,      0
//    };

///* write_header - write (approximation of) MS wave file header */

//static int
//fputlong(x, fp)
//long x;
//FILE *fp;
//{
//    return fputc(x & 0xFF, fp) == EOF ||
//    fputc((x >> 8) & 0xFF, fp) == EOF ||
//    fputc((x >> 16) & 0xFF, fp) == EOF ||
//    fputc((x >> 24) & 0xFF, fp) == EOF;
//}

//static int
//fputshort(x, fp)
//short x;
//FILE *fp;
//{
//    return fputc(x & 0xFF, fp) == EOF ||
//    fputc((x >> 8) & 0xFF, fp) == EOF;
//}

//#define WAVE_HS 20
//#define FACT_HS 4

//#define GSM_FMT 49		/* Format code number		*/
//#define N_CHAN 1		/* Number of channels (mono)	*/
//#define SAMP_FREQ 8000		/* Uncompressed samples/second	*/
//#define BYTE_FREQ 1625		/* Compressed bytes/second	*/
//#define X_1 65			/* Unknown format-specific 	*/
//#define X_2 2			/* Unknown format-specific 	*/
//#define X_3 320			/* Unknown format-specific 	*/

//#define Y_1 20160		/* Unknown "fact" value		*/

//int
//write_header(data_size, fp)
//long data_size;
//FILE *fp;
//{
//    unsigned short s;
//    int rc;

//    rc = 0;
//    rc |= fputs("RIFF", fp) == EOF;
//    rc |= fputlong(52 + ((data_size + 1) & ~0x1), fp);

//    rc |= fputs("WAVEfmt ", fp) == EOF;
//    rc |= fputlong(WAVE_HS, fp);
//    rc |= fputshort(GSM_FMT, fp);
//    rc |= fputshort(N_CHAN, fp);
//    rc |= fputlong(SAMP_FREQ, fp);
//    rc |= fputlong(BYTE_FREQ, fp);
//    rc |= fputlong(X_1, fp);
//    rc |= fputshort(X_2, fp);
//    rc |= fputshort(X_3, fp);

//    rc |= fputs("fact", fp) == EOF;
//    rc |= fputlong(FACT_HS, fp);
//    rc |= fputlong(Y_1, fp);

//    rc |= fputs("data", fp) == EOF;
//    rc |= fputlong(data_size, fp);

//    return rc;
//}


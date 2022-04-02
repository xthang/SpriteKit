#include "BufferUtils.h"
#include <cstdio>
#include <cstdlib>
#include <cstring>

// ===========================================================
// org.andengine.opengl.util.BufferUtils
// ===========================================================

extern "C" JNIEXPORT void JNICALL
Java_org_andengine_opengl_util_BufferUtils_jniPut(JNIEnv *env, jclass, jobject pBuffer,
                                                  jfloatArray pData, jint pLength, jint pOffset) {
    unsigned char *bufferAddress = (unsigned char *) env->GetDirectBufferAddress(pBuffer);
    float *dataAddress = (float *) env->GetPrimitiveArrayCritical(pData, 0);

    memcpy(bufferAddress, dataAddress + pOffset, pLength << 2);
    env->ReleasePrimitiveArrayCritical(pData, dataAddress, 0);
}

extern "C" JNIEXPORT jobject JNICALL
Java_org_andengine_opengl_util_BufferUtils_jniAllocateDirect(JNIEnv *env, jclass, jint pCapacity) {
    jbyte *bytebuffer = (jbyte *) malloc(sizeof(jbyte) * pCapacity);
    return env->NewDirectByteBuffer(bytebuffer, pCapacity);
}

extern "C" JNIEXPORT void JNICALL
Java_org_andengine_opengl_util_BufferUtils_jniFreeDirect(JNIEnv *env, jclass, jobject pBuffer) {
    unsigned char *bufferAddress = (unsigned char *) env->GetDirectBufferAddress(pBuffer);
    free(bufferAddress);
}

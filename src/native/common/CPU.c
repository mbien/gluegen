
#include <jni.h>

#include <assert.h>

JNIEXPORT jint JNICALL 
Java_com_jogamp_gluegen_runtime_Platform_getPointerSizeInBitsImpl(JNIEnv *env, jclass _unused) {
    return sizeof(void *) * 8;
}


#include <jni.h>

extern "C" JNIEXPORT jstring JNICALL
Java_unzen_android_test_cpp_exec_cppmodule_CppModule_getStringFromJni(JNIEnv* env, jclass)
{
    return env->NewStringUTF("I'm libjnifoo.so!");
}

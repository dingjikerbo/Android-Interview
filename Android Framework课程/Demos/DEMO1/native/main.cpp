#include "jni.h"
#include <iostream>

using namespace std;

#define NELEMS(x) (sizeof(x)/sizeof((x)[0]))

static void enterJavaWorld(JNIEnv *env, const char *className, const char *methodName, const char *sig, int argc, const char **argv) {
    jclass stringClass = env->FindClass("java/lang/String");
    jobjectArray strArray = env->NewObjectArray(argc, stringClass, NULL);

    for (int i = 0; i < argc; i++) {
        jstring optionsStr = env->NewStringUTF(argv[i]);
        env->SetObjectArrayElement(strArray, i, optionsStr);
    }

    jclass clazz = env->FindClass(className);
    if (clazz != NULL) {
        jmethodID method = env->GetStaticMethodID(clazz, methodName, sig);
        if (method != NULL) {
            env->CallStaticVoidMethod(clazz, method, strArray);
        }
    }
}

int main() {
    JavaVM *jvm;
    JNIEnv *env;

    JavaVMOption *options = new JavaVMOption[1];
    options[0].optionString = "-Djava.class.path=../../java/out/production/ZygoteInit";

    JavaVMInitArgs vm_args;
    vm_args.version = JNI_VERSION_1_8;
    vm_args.nOptions = 1;
    vm_args.options = options;
    vm_args.ignoreUnrecognized = false;

    JNI_CreateJavaVM(&jvm, (void **) &env, &vm_args);

    const char *argv[] = {"Hello", "Java"};
    enterJavaWorld(env, "ZygoteInit", "main", "([Ljava/lang/String;)V", NELEMS(argv), argv);


    cout << "jvm is quit" << endl;

    jvm->DestroyJavaVM();

    return 0;
}
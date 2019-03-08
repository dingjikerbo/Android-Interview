```
while (true) {
    ......
    poll(pollFds, -1);
    ......
    runOnce();
           
}

int main(int argc, char *argv[]) {
    JavaVM *jvm;
    JNIEnv *env;

    ......

    JNI_CreateJavaVM(&jvm, (void **) &env, &vm_args);

    jclass clazz = env->FindClass("ZygoteInit");
    if (clazz != NULL) {
        jmethodID method = env->GetStaticMethodID(clazz, "Main", "([Ljava/lang/String;)V");
        if (method != NULL) {
            env->CallStaticVoidMethod(clazz, method, args);
        }
    }

    jvm->DestroyJavaVM();

    return 0;
}
```
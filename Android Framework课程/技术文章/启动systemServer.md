

ZygoteInit.startSystemServer
-> Zygote.forkSystemServer
-> SystemServer.run()

```
private static boolean startSystemServer(String abiList, String socketName) {
    int pid;

    try {
        /* Request to fork the system server process */
        pid = Zygote.forkSystemServer(
                parsedArgs.uid, parsedArgs.gid,
                parsedArgs.gids,
                parsedArgs.debugFlags,
                null,
                parsedArgs.permittedCapabilities,
                parsedArgs.effectiveCapabilities);
    } catch (IllegalArgumentException ex) {
        throw new RuntimeException(ex);
    }

    /* For child process */
    if (pid == 0) {
        handleSystemServerProcess(parsedArgs);
    }

    return true;
}
```

# forkSystemServer
```
public static int forkSystemServer(int uid, int gid, int[] gids, int debugFlags,
        int[][] rlimits, long permittedCapabilities, long effectiveCapabilities) {
    VM_HOOKS.preFork();
    int pid = nativeForkSystemServer(
            uid, gid, gids, debugFlags, rlimits, permittedCapabilities, effectiveCapabilities);
    VM_HOOKS.postForkCommon();
    return pid;
}
```

这个preFork是干嘛的？停掉所有的守护线程，重置虚拟机状态，而postFork就是重新启动这些守护线程。

这个nativeForkSystemServer的实现如下，主要是ForkAndSpecializeCommon
```
static jint com_android_internal_os_Zygote_nativeForkSystemServer(
        JNIEnv* env, jclass, uid_t uid, gid_t gid, jintArray gids,
        jint debug_flags, jobjectArray rlimits, jlong permittedCapabilities,
        jlong effectiveCapabilities) {
  pid_t pid = ForkAndSpecializeCommon(env, uid, gid, gids,
                                      debug_flags, rlimits,
                                      permittedCapabilities, effectiveCapabilities,
                                      MOUNT_EXTERNAL_DEFAULT, NULL, NULL, true, NULL,
                                      NULL, NULL);
  return pid;
}
```

这个ForkAndSpecializeCommon，创建子进程后，执行callPostForkChildHooks
```
static pid_t ForkAndSpecializeCommon(JNIEnv* env, ) {
  pid_t pid = fork();

  if (pid == 0) {
    env->CallStaticVoidMethod(gZygoteClass, gCallPostForkChildHooks, debug_flags, is_system_server ? NULL : instructionSet);
  } else if (pid > 0) {
    // the parent process
  }
  return pid;
}
```


这个函数干了什么，这里面也只是初始化了虚拟机相关的东西，初始化守护线程
```
private static void callPostForkChildHooks(int debugFlags, String instructionSet) {
    VM_HOOKS.postForkChild(debugFlags, instructionSet);
}   
```

好像没看到binder是在哪启动的。我们看下handleSystemServerProcess，这里调到了RuntimeInit.zygoteInit，而且永远不应该返回。

```
private static void handleSystemServerProcess() {
    RuntimeInit.zygoteInit(parsedArgs.targetSdkVersion, parsedArgs.remainingArgs, cl);

    /* should never reach here */
}
```

看下这个函数，commonInit没做什么，nativeZygoteInit调了AppRuntime的onzygoteInit
```
public static final void zygoteInit( {
    commonInit();
    nativeZygoteInit();
    applicationInit(targetSdkVersion, argv, classLoader);
}
```


这里面会打开binder驱动，注册binder线程
```
virtual void onZygoteInit() {
    sp<ProcessState> proc = ProcessState::self();
    ALOGV("App process: starting thread pool.\n");
    proc->startThreadPool();
}
```

我们看普通应用进程的启动，显示forkAndSpecialize，然后handleChildProc，这个函数永远不该返回。

```
boolean runOnce() throws ZygoteInit.MethodAndArgsCaller {
    pid = Zygote.forkAndSpecialize();

    if (pid == 0) {
        handleChildProc(parsedArgs, descriptors, childPipeFd, newStderr);

        // should never get here, the child is expected to either
        // throw ZygoteInit.MethodAndArgsCaller or exec().
        return true;
    } else {
        return handleParentProc();
    }
}
```

Zygote.forkAndSpecialize，里面执行了ForkAndSpecializeCommon，和启动systemServer一样

```
public static int forkAndSpecialize() {
    VM_HOOKS.preFork();
    int pid = nativeForkAndSpecialize();
    VM_HOOKS.postForkCommon();
    return pid;
}

static jint com_android_internal_os_Zygote_nativeForkAndSpecialize() {
    return ForkAndSpecializeCommon(env);
}
```

看下handleChildProc，这个是重点，里面的RuntimeInit.zygoteInit和systemServer一样，都要启动Binder机制。
```
private void handleChildProc() {
    RuntimeInit.zygoteInit();
}
```
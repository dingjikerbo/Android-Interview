

Java层new Thread的时候没做什么特别的，关键是start函数，

```
public synchronized void start() {
    nativeCreate(this, stackSize, daemon);
}
```

```
static void Thread_nativeCreate(JNIEnv* env, jclass, jobject java_thread, jlong stack_size,
                                jboolean daemon) {
  Thread::CreateNativeThread(env, java_thread, stack_size, daemon == JNI_TRUE);
}
```

下面是关键了，JNIEnv可以转成JNIEnvExt，里面有个self，保存了当前线程的Thread的native对象。但是这只是当前调用者的thread对象，不是我们要新创建的线程的thread对象，
下面的child_thread就是新线程了，给java层的Thread对象保存在这个tlsPtr的jpeer变量了。这个tlsPtr是啥，是Native层thread对象的一个内部结构体变量。然后给这个native
层thread对象的指针保存到了Java层Thread对象里。另外还有一个重点，需要为这个线程创建一个JniEnvExt对象，这个JniEnvExt继承自JNIEnv，
下面最重要的就是pthread_create了，
```
void Thread::CreateNativeThread(JNIEnv* env, jobject java_peer, size_t stack_size, bool is_daemon) {
    Thread* self = static_cast<JNIEnvExt*>(env)->self;
    Runtime* runtime = Runtime::Current();

    Thread* child_thread = new Thread(is_daemon);
    child_thread->tlsPtr_.jpeer = env->NewGlobalRef(java_peer);

    env->SetLongField(java_peer, WellKnownClasses::java_lang_Thread_nativePeer,
                    reinterpret_cast<jlong>(child_thread));

    std::unique_ptr<JNIEnvExt> child_jni_env_ext(
      JNIEnvExt::Create(child_thread, Runtime::Current()->GetJavaVM()));
    ......

    pthread_t new_pthread;
    
    pthread_create(&new_pthread, &attr, Thread::CreateCallback, child_thread);
}
```

线程启动后，执行CreateCallback, 里面会调用Java层的run函数，

```
void* Thread::CreateCallback(void* arg) {
    Thread* self = reinterpret_cast<Thread*>(arg);
    Runtime* runtime = Runtime::Current();

    self->Init(runtime->GetThreadList(), runtime->GetJavaVM(), self->tlsPtr_.tmp_jni_env);
  
    ScopedObjectAccess soa(self);
    self->InitStringEntryPoints();

    self->tlsPtr_.opeer = soa.Decode<mirror::Object*>(self->tlsPtr_.jpeer);
    self->tlsPtr_.jpeer = nullptr;

    mirror::Object* receiver = self->tlsPtr_.opeer;
    jmethodID mid = WellKnownClasses::java_lang_Thread_run;
    ScopedLocalRef<jobject> ref(soa.Env(), soa.AddLocalReference<jobject>(receiver));
    InvokeVirtualOrInterfaceWithJValues(soa, ref.get(), mid, nullptr);

    return nullptr;
}
```

咱们再看Init函数，这里

```
bool Thread::Init(ThreadList* thread_list, JavaVMExt* java_vm, JNIEnvExt* jni_env_ext) {
    tlsPtr_.pthread_self = pthread_self();

    pthread_setspecific(Thread::pthread_key_self_, this);

    if (jni_env_ext != nullptr) {
        tlsPtr_.jni_env = jni_env_ext;
    } else {
        tlsPtr_.jni_env = JNIEnvExt::Create(this, java_vm);
        if (tlsPtr_.jni_env == nullptr) {
            return false;
        }
    }
    return true;
}
```

咱们再看currentThread，这个GetPeer其实返回的是Java层的Thread对象，

```
static jobject Thread_currentThread(JNIEnv* env, jclass) {
  ScopedFastNativeObjectAccess soa(env);
  return soa.AddLocalReference<jobject>(soa.Self()->GetPeer());
}
```

这个soa.Self是啥，ScopedFastNativeObjectAccess继承自ScopedObjectAccessAlreadyRunnable，这里的Self函数返回的就是他的self_了，这个self_是ThreadForEnv返回的，
其实就是JNIEnv转成JNIEnvExt后里面的self。

```
explicit ScopedObjectAccessAlreadyRunnable(JNIEnv* env)
  LOCKS_EXCLUDED(Locks::thread_suspend_count_lock_) ALWAYS_INLINE
  : self_(ThreadForEnv(env)), env_(down_cast<JNIEnvExt*>(env)), vm_(env_->vm) {
}


static inline Thread* ThreadForEnv(JNIEnv* env) {
  JNIEnvExt* full_env(down_cast<JNIEnvExt*>(env));
  return full_env->self;
}
```

再看JNIEnvExt，这里create是new了一个JNIEnvExt对象，继承JNIEnv，
```
JNIEnvExt* JNIEnvExt::Create(Thread* self_in, JavaVMExt* vm_in) {
  std::unique_ptr<JNIEnvExt> ret(new JNIEnvExt(self_in, vm_in));
  if (CheckLocalsValid(ret.get())) {
    return ret.release();
  }
  return nullptr;
}

JNIEnvExt::JNIEnvExt(Thread* self_in, JavaVMExt* vm_in)
    : self(self_in),
      vm(vm_in),
      local_ref_cookie(IRT_FIRST_SEGMENT),
      locals(kLocalsInitial, kLocalsMax, kLocal, false),
      check_jni(false),
      critical(0),
      monitors("monitors", kMonitorsInitial, kMonitorsMax) {
  functions = unchecked_functions = GetJniNativeInterface();
  if (vm->IsCheckJniEnabled()) {
    SetCheckJniEnabled(true);
  }
}
```

现在还有最后一个疑问，你new了一个Thread的Java对象，这个线程会自动AttachCurrentThread么，这个线程有JNIEnv我是知道的，
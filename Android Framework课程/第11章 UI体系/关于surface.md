咱们从ViewRootImpl的setView开始吧，主要做了三件事，一个是设置mView，一个是requestLayout，一个是向WMS调用addToDisplay。这个在添加到WMS之前要来一次requestLayout，

```
public void setView(View view, WindowManager.LayoutParams attrs, View panelParentView) {
    mView = view;

    // Schedule the first layout -before- adding to the window
    // manager, to make sure we do the relayout before receiving
    // any other events from the system.
    requestLayout();

    res = mWindowSession.addToDisplay(mWindow, ...);

    ......
}
```

看下requestLayout的实现，这个checkThread就是检查当前线程和ViewRootImpl创建的线程是不是一个线程。再看scheduleTraversals，

```
@Override
public void requestLayout() {
    if (!mHandlingLayoutInLayoutRequest) {
        checkThread();
        mLayoutRequested = true;
        scheduleTraversals();
    }
}
```

这个往handler里面post了一个syncBarrier，然后又是Choreographer里post了一个mTraversalRunnable。这插入一个syncBarrier什么意思呢，就是说这之后再往主线程丢的消息都暂时先不处理了，除非移除了这个barrier，或者是异步消息。
```
void scheduleTraversals() {
    if (!mTraversalScheduled) {
        mTraversalScheduled = true;
        mTraversalBarrier = mHandler.getLooper().getQueue().postSyncBarrier();
        mChoreographer.postCallback(
                Choreographer.CALLBACK_TRAVERSAL, mTraversalRunnable, null);
    }
}
```

这个traversalRunnable里面执行了performTraversal，这里面有一件重要的事情，就是relayoutWindow，这个是干嘛的呢？

```
private void performTraversals() {
    ......
    int relayoutResult = 0;
    if (mFirst || windowShouldResize || insetsChanged ||
                viewVisibilityChanged || params != null) {
        boolean hadSurface = mSurface.isValid();
        relayoutResult = relayoutWindow(params, viewVisibility, ...);
        if (!hadSurface) {
            if (mSurface.isValid()) {
                newSurface = true;
                ......
            }
            ......
        }
    }

    ......
    performDraw();
    ...... 
}
```

看看relayoutWindow的实现，这里传了一个mSurface，这个是一个空壳。这个mWindowSession是哪的？

```
private int relayoutWindow(WindowManager.LayoutParams params, int viewVisibility, ...) {
    int relayoutResult = mWindowSession.relayout(
            mWindow, mSeq, params,
            (int) (mView.getMeasuredWidth() * appScale + 0.5f),
            (int) (mView.getMeasuredHeight() * appScale + 0.5f),
            viewVisibility, insetsPending ? WindowManagerGlobal.RELAYOUT_INSETS_PENDING : 0,
            mWinFrame, mPendingOverscanInsets, mPendingContentInsets, mPendingVisibleInsets,
            mPendingStableInsets, mPendingOutsets, mPendingConfiguration, mSurface);
   
    return relayoutResult;
}
```

是ViewRootImpl构造函数里获取的，

```
public ViewRootImpl(Context context, Display display) {
    mContext = context;
    mWindowSession = WindowManagerGlobal.getWindowSession();
    ......
}
```

原来是通过WMS的openSession函数获得的，咱们看WMS里面的实现，
```
public static IWindowSession getWindowSession() {
    synchronized (WindowManagerGlobal.class) {
        if (sWindowSession == null) {
            try {
                InputMethodManager imm = InputMethodManager.getInstance();
                IWindowManager windowManager = getWindowManagerService();
                sWindowSession = windowManager.openSession(
                        new IWindowSessionCallback.Stub() {
                            @Override
                            public void onAnimatorScaleChanged(float scale) {
                                ValueAnimator.setDurationScale(scale);
                            }
                        },
                        imm.getClient(), imm.getInputContext());
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to open window session", e);
            }
        }
        return sWindowSession;
    }
}
```


这里new了一个Session，
```
@Override
public IWindowSession openSession(IWindowSessionCallback callback, IInputMethodClient client,
        IInputContext inputContext) {
    Session session = new Session(this, callback, client, inputContext);
    return session;
}
```

这个Session的定义来看下，这是IWindowSession类型的，构造函数带了好几个binder句柄，不过咱们不关注。

```
final class Session extends IWindowSession.Stub
        implements IBinder.DeathRecipient {
    final WindowManagerService mService;
    final IWindowSessionCallback mCallback;
    final IInputMethodClient mClient;
    final IInputContext mInputContext;
    ......

    public Session(WindowManagerService service, IWindowSessionCallback callback,
            IInputMethodClient client, IInputContext inputContext) {
        ......
    }
    ......
}
```

再来看这个session的relayout函数，这又冒出一个mService，这个mService就是WMS，

```
public int relayout(IWindow window, int seq, WindowManager.LayoutParams attrs,
        int requestedWidth, int requestedHeight, int viewFlags,
        int flags, Rect outFrame, Rect outOverscanInsets, Rect outContentInsets,
        Rect outVisibleInsets, Rect outStableInsets, Rect outsets, Configuration
                outConfig,
        Surface outSurface) {
   
    int res = mService.relayoutWindow(this, window, seq, attrs,
            requestedWidth, requestedHeight, viewFlags, flags,
            outFrame, outOverscanInsets, outContentInsets, outVisibleInsets,
            outStableInsets, outsets, outConfig, outSurface);
    
    return res;
}
```

看看这个relayoutWindow的实现，这会通过winAnimator的createSurfaceLocked创建一个surfaceControl，然后copyFrom，给surface拷到outSurface里面？
```
public int relayoutWindow(Session session, IWindow client, int seq,
            WindowManager.LayoutParams attrs, int requestedWidth,
            int requestedHeight, int viewVisibility, int flags,
            Rect outFrame, Rect outOverscanInsets, Rect outContentInsets,
            Rect outVisibleInsets, Rect outStableInsets, Rect outOutsets, Configuration outConfig,
            Surface outSurface) {
        
    WindowState win = windowForClientLocked(session, client, false);

    if (viewVisibility == View.VISIBLE && (win.mAppToken == null || !win.mAppToken.clientHidden)) {
        if (!win.mHasSurface) {
            surfaceChanged = true;
        }
        SurfaceControl surfaceControl = winAnimator.createSurfaceLocked();
        outSurface.copyFrom(surfaceControl);
    } else {
        ......
    }

    if (win.mAppToken != null) {
        win.mAppToken.updateReportedVisibilityLocked();
    }
    ......
}
```

看看createSurfaceLocked函数，这new了一个SurfaceControl，咱们看上面的copyFrom，

```
SurfaceControl createSurfaceLocked() {
    if (mSurfaceControl == null) {
        ......

        mSurfaceControl = new SurfaceControl(
                mSession.mSurfaceSession,
                attrs.getTitle().toString(),
                width, height, format, flags);

        ......
    }

    return mSurfaceControl;
}
```

看这个copyFrom，SurfaceControl里有一个native的SurfaceControl对象，这里的nativeCreateFromSurfaceControl其实就是从Native的SurfaceControl对象里获取Native
Surface对象，然后塞到这个新的Surface的Java对象里。

```
public void copyFrom(SurfaceControl other) {
    long surfaceControlPtr = other.mNativeObject;
    long newNativeObject = nativeCreateFromSurfaceControl(surfaceControlPtr);
    setNativeObjectLocked(newNativeObject);
}
```

咱们看SurfaceControl的构造函数，看看他的构造函数，
```
public SurfaceControl(SurfaceSession session,
        String name, int w, int h, int format, int flags) {
    mName = name;
    mNativeObject = nativeCreate(session, name, w, h, format, flags);
}
```

看native层，这里传的session是啥，是mSession里面的surfaceSession，这个mSession是咱们通过openSession得来的，这个surfaceSession呢？记得ViewRootImpl里面的setView嘛？里面先requestLayout，然后addToDisplay，虽然requestLayout先调的，但是因为在下一个vsync才执行，所以这里addToDisplay先执行了，这个实际上执行到了WMS的addWindow，
```
static jlong nativeCreate(JNIEnv* env, jclass clazz, jobject sessionObj,
        jstring nameStr, jint w, jint h, jint format, jint flags) {
    sp<SurfaceComposerClient> client(android_view_SurfaceSession_getClient(env, sessionObj));
    sp<SurfaceControl> surface = client->createSurface(
            String8(name.c_str()), w, h, format, flags);
    if (surface == NULL) {
        jniThrowException(env, OutOfResourcesException, NULL);
        return 0;
    }
    surface->incStrong((void *)nativeCreate);
    return reinterpret_cast<jlong>(surface.get());
}
```

这是WMS的addWindow，这关键是会new一个WindowState和一个WindowToken，保存在map里，另外还会调WindowState的attach函数，
```
public int addWindow(Session session, IWindow client, int seq,
        WindowManager.LayoutParams attrs, int viewVisibility, int displayId,
        Rect outContentInsets, Rect outStableInsets, Rect outOutsets,
        InputChannel outInputChannel) {
    ......
    WindowState win = new WindowState(this, session, client, token,
            attachedWindow, appOp[0], seq, attrs, viewVisibility, displayContent);
    ......
    win.attach();
    ......        
    return res;
}
```

看看这个attach函数，
```
void attach() {
    mSession.windowAddedLocked();
}
```

在这个windowAddedLocked函数里，会new一个SurfaceSession，这是干嘛的，
```
void windowAddedLocked() {
    if (mSurfaceSession == null) {
        if (WindowManagerService.localLOGV) Slog.v(
            WindowManagerService.TAG, "First window added to " + this + ", creating SurfaceSession");
        mSurfaceSession = new SurfaceSession();
        if (WindowManagerService.SHOW_TRANSACTIONS) Slog.i(
                WindowManagerService.TAG, "  NEW SURFACE SESSION " + mSurfaceSession);
        mService.mSessions.add(this);
        if (mLastReportedAnimatorScale != mService.getCurrentAnimatorScale()) {
            mService.dispatchNewAnimatorScaleLocked(this);
        }
    }
    mNumWindow++;
}
```

SurfaceSession也是要native层初始化的，
```
public SurfaceSession() {
    mNativeClient = nativeCreate();
}
```

这new了一个SurfaceComposerClient，这里面调用了createConnection创建了一个到SurfaceFlinger的连接，类型是ISurfaceComposerClient。
所以我们可以认为SurfaceSession就是创建了一个到SurfaceFlinger的连接。
```
static jlong nativeCreate(JNIEnv* env, jclass clazz) {
    SurfaceComposerClient* client = new SurfaceComposerClient();
    client->incStrong((void*)nativeCreate);
    return reinterpret_cast<jlong>(client);
}

SurfaceComposerClient::SurfaceComposerClient()
    : mStatus(NO_INIT), mComposer(Composer::getInstance())
{
}

void SurfaceComposerClient::onFirstRef() {
    sp<ISurfaceComposer> sm(ComposerService::getComposerService());
    if (sm != 0) {
        sp<ISurfaceComposerClient> conn = sm->createConnection();
        if (conn != 0) {
            mClient = conn;
            mStatus = NO_ERROR;
        }
    }
}
```

我们回到surfaceControl，他的构造函数里面先拿到SurfaceSession的这个native对象，SurfaceComposerClient，调用他的createSurface，返回一个native层的SurfaceControl
对象，也是，SurfaceSession是直接跟SurfaceFlinger连接的，所以活要交给这个SurfaceSession来做，SurfaceControl只是控制流程用的，这个SurfaceComposerClient的createSurface其实要交给里面的那个mClient来做，也就是ISurfaceComposerClient。这里返回了两个值，一个是handle，一个是IGraphicBufferProducer，然后用这两个值，new一个native层的
SurfaceControl对象，然后返回给Java层。咱们看这个createSurface的实现，

```
sp<SurfaceControl> SurfaceComposerClient::createSurface(
        const String8& name,
        uint32_t w,
        uint32_t h,
        PixelFormat format,
        uint32_t flags)
{
    sp<SurfaceControl> sur;
    if (mStatus == NO_ERROR) {
        sp<IBinder> handle;
        sp<IGraphicBufferProducer> gbp;
        status_t err = mClient->createSurface(name, w, h, format, flags,
                &handle, &gbp);
        if (err == NO_ERROR) {
            sur = new SurfaceControl(this, handle, gbp);
        }
    }
    return sur;
}
```

这里是应用端的，transact出去之后，读入两个binder，
```
virtual status_t createSurface(const String8& name, uint32_t width,
        uint32_t height, PixelFormat format, uint32_t flags,
        sp<IBinder>* handle,
        sp<IGraphicBufferProducer>* gbp) {
    Parcel data, reply;
    data.writeInterfaceToken(ISurfaceComposerClient::getInterfaceDescriptor());
    data.writeString8(name);
    data.writeUint32(width);
    data.writeUint32(height);
    data.writeInt32(static_cast<int32_t>(format));
    data.writeUint32(flags);
    remote()->transact(CREATE_SURFACE, data, &reply);
    *handle = reply.readStrongBinder();
    *gbp = interface_cast<IGraphicBufferProducer>(reply.readStrongBinder());
    return reply.readInt32();
}
```

看Bn端，这里调用createSurface，然后writeStrongBinder，

```
status_t BnSurfaceComposerClient::onTransact(
    uint32_t code, const Parcel& data, Parcel* reply, uint32_t flags)
{
     switch(code) {
        case CREATE_SURFACE: {
            CHECK_INTERFACE(ISurfaceComposerClient, data, reply);
            String8 name = data.readString8();
            uint32_t width = data.readUint32();
            uint32_t height = data.readUint32();
            PixelFormat format = static_cast<PixelFormat>(data.readInt32());
            uint32_t createFlags = data.readUint32();
            sp<IBinder> handle;
            sp<IGraphicBufferProducer> gbp;
            status_t result = createSurface(name, width, height, format,
                    createFlags, &handle, &gbp);
            reply->writeStrongBinder(handle);
            reply->writeStrongBinder(IInterface::asBinder(gbp));
            reply->writeInt32(result);
            return NO_ERROR;
        }
    ......
}
```

这个createSurface实现在哪，在Client.cpp里，这个Client是继承BnSurfaceComposerClient的，这new了一个message，post到flinger工作线程中去处理了，然后同步等待消息处理结果，最后返回。这种处理消息的方式老子还是第一次见到，这个其实调到了SurfaceFlinger里，SurfaceFlinger里面有个MessageQueue，在一个工作线程里处理这个消息，这里调的是
createLayer函数，

```
status_t Client::createSurface(
        const String8& name,
        uint32_t w, uint32_t h, PixelFormat format, uint32_t flags,
        sp<IBinder>* handle,
        sp<IGraphicBufferProducer>* gbp)
{
    /*
     * createSurface must be called from the GL thread so that it can
     * have access to the GL context.
     */

    class MessageCreateLayer : public MessageBase {
        SurfaceFlinger* flinger;
        Client* client;
        sp<IBinder>* handle;
        sp<IGraphicBufferProducer>* gbp;
        status_t result;
        const String8& name;
        uint32_t w, h;
        PixelFormat format;
        uint32_t flags;
    public:
        MessageCreateLayer(SurfaceFlinger* flinger,
                const String8& name, Client* client,
                uint32_t w, uint32_t h, PixelFormat format, uint32_t flags,
                sp<IBinder>* handle,
                sp<IGraphicBufferProducer>* gbp)
            : flinger(flinger), client(client),
              handle(handle), gbp(gbp),
              name(name), w(w), h(h), format(format), flags(flags) {
        }
        status_t getResult() const { return result; }
        virtual bool handler() {
            result = flinger->createLayer(name, client, w, h, format, flags,
                    handle, gbp);
            return true;
        }
    };

    sp<MessageBase> msg = new MessageCreateLayer(mFlinger.get(),
            name, this, w, h, format, flags, handle, gbp);
    mFlinger->postMessageSync(msg);
    return static_cast<MessageCreateLayer*>( msg.get() )->getResult();
}
```

看这个createLayer，这里createNormalLayer，然后addClientLayer。
```
status_t SurfaceFlinger::createLayer(
        const String8& name,
        const sp<Client>& client,
        uint32_t w, uint32_t h, PixelFormat format, uint32_t flags,
        sp<IBinder>* handle, sp<IGraphicBufferProducer>* gbp)
{
    status_t result = NO_ERROR;

    sp<Layer> layer;

    switch (flags & ISurfaceComposerClient::eFXSurfaceMask) {
        case ISurfaceComposerClient::eFXSurfaceNormal:
            result = createNormalLayer(client,
                    name, w, h, flags, format,
                    handle, gbp, &layer);
            break;
        ......
    }

    result = addClientLayer(client, *handle, *gbp, layer);

    setTransactionFlags(eTransactionNeeded);
    return result;
}
```

这里new了一个layer，然后设置handle和gbp。这个Layer继承了SurfaceFlingerConsumer，这个构造函数里面创建了一个纹理，看他的onFirstRef，
```
status_t SurfaceFlinger::createNormalLayer(const sp<Client>& client,
        const String8& name, uint32_t w, uint32_t h, uint32_t flags, PixelFormat& format,
        sp<IBinder>* handle, sp<IGraphicBufferProducer>* gbp, sp<Layer>* outLayer)
{
    *outLayer = new Layer(this, client, name, w, h, flags);
    status_t err = (*outLayer)->setBuffers(w, h, format, flags);
    *handle = (*outLayer)->getHandle();
    *gbp = (*outLayer)->getProducer();
    return err;
}
```

这里createBufferQueue，生成producer端和consumer端，这生成了一个SurfaceFlingerConsumer对象，里面包含了纹理和BufferQueue的consumer。
然后看来默认是三缓冲的，不然还有warning。这还有一个Producer对象，那上面getHandle和getProducer返回的什么呢？
```
void Layer::onFirstRef() {
    // Creates a custom BufferQueue for SurfaceFlingerConsumer to use
    sp<IGraphicBufferProducer> producer;
    sp<IGraphicBufferConsumer> consumer;
    BufferQueue::createBufferQueue(&producer, &consumer);
    mProducer = new MonitoredProducer(producer, mFlinger);
    mSurfaceFlingerConsumer = new SurfaceFlingerConsumer(consumer, mTextureName);
    mSurfaceFlingerConsumer->setConsumerUsageBits(getEffectiveUsage(0));
    mSurfaceFlingerConsumer->setContentsChangedListener(this);
    mSurfaceFlingerConsumer->setName(mName);

#ifdef TARGET_DISABLE_TRIPLE_BUFFERING
#warning "disabling triple buffering"
    mSurfaceFlingerConsumer->setDefaultMaxBufferCount(2);
#else
    mSurfaceFlingerConsumer->setDefaultMaxBufferCount(3);
#endif

    const sp<const DisplayDevice> hw(mFlinger->getDefaultDisplayDevice());
    updateTransformHint(hw);
}
```

这个getProducer返回的就是mProducer了，getHandle返回的是只是个普通的binder对象，这个handle的作用就是当给handle跨进程传到应用端，如果应用端进程挂了，那么这个layer能
自动destroy。
```
sp<IBinder> Layer::getHandle() {
    Mutex::Autolock _l(mLock);

    mHasSurface = true;

    /*
     * The layer handle is just a BBinder object passed to the client
     * (remote process) -- we don't keep any reference on our side such that
     * the dtor is called when the remote side let go of its reference.
     *
     * LayerCleaner ensures that mFlinger->onLayerDestroyed() is called for
     * this layer when the handle is destroyed.
     */

    class Handle : public BBinder, public LayerCleaner {
        wp<const Layer> mOwner;
    public:
        Handle(const sp<SurfaceFlinger>& flinger, const sp<Layer>& layer)
            : LayerCleaner(flinger, layer), mOwner(layer) {
        }
    };

    return new Handle(mFlinger, this);
}

sp<IGraphicBufferProducer> Layer::getProducer() const {
    return mProducer;
}
```

好了，现在咱们能了解了，SurfaceControl在创建的时候会createSurface，这其实就是在SurfacFlinger创建了一个bufferQueue和一个layer，给layer的producer端返回给应用
层，consumer端对应这SurfaceFlinger和一个纹理，同时应用端还有一个handle，这是个binder句柄，如果应用进程挂了，surfaceFlinger中对应的layer也会回收。

咱们再看surface是怎么copyFrom一个SurfaceControl的，这个copyFrom是在WMS里面，SurfaceControl的java对象里有个mNativeObject，指向native层的SurfaceControl对象，


```
public void copyFrom(SurfaceControl other) {
    long surfaceControlPtr = other.mNativeObject;
    long newNativeObject = nativeCreateFromSurfaceControl(surfaceControlPtr);
    setNativeObjectLocked(newNativeObject);
}
```


这个native层的SurfaceControl的getSurface函数返回了一个Native层的Surface对象，
```
static jlong nativeCreateFromSurfaceControl(JNIEnv* env, jclass clazz,
        jlong surfaceControlNativeObj) {
    sp<SurfaceControl> ctrl(reinterpret_cast<SurfaceControl *>(surfaceControlNativeObj));
    sp<Surface> surface(ctrl->getSurface());
    if (surface != NULL) {
        surface->incStrong(&sRefBaseOwner);
    }
    return reinterpret_cast<jlong>(surface.get());
}
```

这里果然new了一个Surface，包含了buffer producer，这个surface producer的buffer，最终都要被surfaceFlinger消费。回到上面，copyFrom其实就是给SurfaceControl
里创建的一个Native层的Surface对象指针塞到Surface的Java对象里。这样的话会有问题吧，因为现在Surface还在WMS进程，这个指针到应用进程肯定就失效了啊，怎么看Surface怎么
传回应用进程的，
```
sp<Surface> SurfaceControl::getSurface() const {
    Mutex::Autolock _l(mLock);
    if (mSurfaceData == 0) {
        // This surface is always consumed by SurfaceFlinger, so the
        // producerControlledByApp value doesn't matter; using false.
        mSurfaceData = new Surface(mGraphicBufferProducer, false);
    }
    return mSurfaceData;
}
```

在IWindowSession.aidl里，这个outSurface是个out，意思是对于proxy端来说，这个surface是要从Parcel里读出来的，那surface肯定是实现了Parcelable接口喽？果不其然，确实如此。

```
int relayout(IWindow window, int seq, in WindowManager.LayoutParams attrs,
            int requestedWidth, int requestedHeight, int viewVisibility,
            int flags, out Rect outFrame, out Rect outOverscanInsets,
            out Rect outContentInsets, out Rect outVisibleInsets, out Rect outStableInsets,
            out Rect outOutsets, out Configuration outConfig, out Surface outSurface);
```

看下他的readFromParcel，这里调nativeReadFromParcel，得到一个long，估计是native层对象的指针，然后setNativeObjectLocked是啥？
```
public void readFromParcel(Parcel source) {
        synchronized (mLock) {
            mName = source.readString();
            setNativeObjectLocked(nativeReadFromParcel(mNativeObject, source));
        }
    }
```

这里根据Java层的Parcel对象拿到他对应的Native层的parcel对象，这个nativeObject是个Native层的surface对象，之前在WMS里copyFrom已经设置过了，不过注意啊，Surface的writeToParcel里只写了surface的name和gbp的binder，没有写这个nativeSurface的指针，写了也没用，这个指针到应用层没用。看下面的readStringBinder，要知道这是什么，咱们得看Surface的writeToParcel函数，里面有个nativeWriteToParcel函数，这个会给GraphicBufferProducer的binder句柄写到Parcel里，
所以这里读出来的就是GraphicBufferProducer的binder句柄了。然后这里创建了native层的surface对象，返回java层，设置到java层的nativeObject里。

```
static jlong nativeReadFromParcel(JNIEnv* env, jclass clazz,
        jlong nativeObject, jobject parcelObj) {
    Parcel* parcel = parcelForJavaObject(env, parcelObj);

    sp<Surface> self(reinterpret_cast<Surface *>(nativeObject));
    sp<IBinder> binder(parcel->readStrongBinder());

    sp<Surface> sur;
    sp<IGraphicBufferProducer> gbp(interface_cast<IGraphicBufferProducer>(binder));
    if (gbp != NULL) {
        sur = new Surface(gbp, true);
    }

    return jlong(sur.get());
}
```

new一个native层的surface是干嘛的呢？初始化了一堆ANativeWindow的函数指针。

```
Surface::Surface(
        const sp<IGraphicBufferProducer>& bufferProducer,
        bool controlledByApp)
    : mGraphicBufferProducer(bufferProducer),
      mGenerationNumber(0)
{
    // Initialize the ANativeWindow function pointers.
    ANativeWindow::setSwapInterval  = hook_setSwapInterval;
    ANativeWindow::dequeueBuffer    = hook_dequeueBuffer;
    ANativeWindow::cancelBuffer     = hook_cancelBuffer;
    ANativeWindow::queueBuffer      = hook_queueBuffer;
    ANativeWindow::query            = hook_query;
    ANativeWindow::perform          = hook_perform;

    ANativeWindow::dequeueBuffer_DEPRECATED = hook_dequeueBuffer_DEPRECATED;
    ANativeWindow::cancelBuffer_DEPRECATED  = hook_cancelBuffer_DEPRECATED;
    ANativeWindow::lockBuffer_DEPRECATED    = hook_lockBuffer_DEPRECATED;
    ANativeWindow::queueBuffer_DEPRECATED   = hook_queueBuffer_DEPRECATED;

    const_cast<int&>(ANativeWindow::minSwapInterval) = 0;
    const_cast<int&>(ANativeWindow::maxSwapInterval) = 1;

    mReqWidth = 0;
    mReqHeight = 0;
    mReqFormat = 0;
    mReqUsage = 0;
    mTimestamp = NATIVE_WINDOW_TIMESTAMP_AUTO;
    mDataSpace = HAL_DATASPACE_UNKNOWN;
    mCrop.clear();
    mScalingMode = NATIVE_WINDOW_SCALING_MODE_FREEZE;
    mTransform = 0;
    mStickyTransform = 0;
    mDefaultWidth = 0;
    mDefaultHeight = 0;
    mUserWidth = 0;
    mUserHeight = 0;
    mTransformHint = 0;
    mConsumerRunningBehind = false;
    mConnectedToCpu = false;
    mProducerControlledByApp = controlledByApp;
    mSwapIntervalZero = false;
}
```

最后，咱们来看看java层surface对象的一些函数，这里调了native层的lockCanvas函数，并且返回一个mLockedObject，这里不能重复Lock，否则抛异常。

```
public Canvas lockCanvas(Rect inOutDirty) {
    synchronized (mLock) {
        if (mLockedObject != 0) {
            throw new IllegalArgumentException("Surface was already locked");
        }
        mLockedObject = nativeLockCanvas(mNativeObject, mCanvas, inOutDirty);
        return mCanvas;
    }
}
```


这是干嘛的呢？首先拿到native层的surface对象，这给Java层传的Rect对象的四个角坐标赋值给Native层的Rect对象，ANativeWindow_Buffer是啥，就是个结构体，保存了Window的
width, height, stride, format，里面还有一个指针变量bits不知道值到哪的。这里调用了nativeSurface的lock函数，然后创建了一个SkImageInfo对象，然后生成一个
SkBitmap对象，设置缓冲区，缓冲区就是ANativeWindow_buffer的bits啊，然后通过Java层的canvas对象拿到对应的native层canvas对象，给bitmap设置给他，然后返回。
所以这里的lockCanvas最关键的步骤就是创建了一个

```
static jlong nativeLockCanvas(JNIEnv* env, jclass clazz,
        jlong nativeObject, jobject canvasObj, jobject dirtyRectObj) {
    sp<Surface> surface(reinterpret_cast<Surface *>(nativeObject));

    ANativeWindow_Buffer outBuffer;
    status_t err = surface->lock(&outBuffer, dirtyRectPtr);

    ......

    SkBitmap bitmap;
    bitmap.setPixels(outBuffer.bits);       

    Canvas* nativeCanvas = GraphicsJNI::getNativeCanvas(env, canvasObj);
    nativeCanvas->setBitmap(bitmap);

    ......

    sp<Surface> lockedSurface(surface);
    return (jlong) lockedSurface.get();
}
```

看下native层surface的lock函数，GraphicBuffer继承自ANativeWindowBuffer，Native层的surface对象有两个GraphicBuffer，一个是mLockedBuffer，一个是mPostedBuffer，


这的dequeueBuffer其实就是通过GraphicBufferProducer跨进程向SurfaceFlinger中的bufferQueue申请一块buffer，

```
status_t Surface::lock(
        ANativeWindow_Buffer* outBuffer, ARect* inOutDirtyBounds)
{
    ANativeWindowBuffer* out;

    status_t err = dequeueBuffer(&out, &fenceFd);
    ......

    void* vaddr;
    status_t res = backBuffer->lockAsync(
            GRALLOC_USAGE_SW_READ_OFTEN | GRALLOC_USAGE_SW_WRITE_OFTEN,
            newDirtyRegion.bounds(), &vaddr, fenceFd);

    
    mLockedBuffer = backBuffer;
    outBuffer->width  = backBuffer->width;
    outBuffer->height = backBuffer->height;
    outBuffer->stride = backBuffer->stride;
    outBuffer->format = backBuffer->format;
    outBuffer->bits   = vaddr;

    return err;
}

```

咱们看unlockAndPost，

```
static void nativeUnlockCanvasAndPost(JNIEnv* env, jclass clazz,
        jlong nativeObject, jobject canvasObj) {
    sp<Surface> surface(reinterpret_cast<Surface *>(nativeObject));

    ......

    // unlock surface
    status_t err = surface->unlockAndPost();
}
```

看surface的unlockAndPost，这里给mLockedBuffer释放了，并且放回到BufferQueue里，这里给buffer入队，SurfaceFlinger作为consumer端就会处理这个buffer，最终显示出来。

```
status_t Surface::unlockAndPost()
{

    int fd = -1;
    status_t err = mLockedBuffer->unlockAsync(&fd);

    err = queueBuffer(mLockedBuffer.get(), fd);

    mPostedBuffer = mLockedBuffer;
    mLockedBuffer = 0;
    return err;
}
```
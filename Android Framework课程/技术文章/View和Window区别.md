
https://blog.csdn.net/jiang19921002/article/details/78977560

Activity里有个mDecor，一个mWindow，这两个什么地方设置的呢？

先看mDecor,

```
// ActivityThread
final void handleResumeActivity(IBinder token, ...) {
    ActivityClientRecord r = performResumeActivity(token, clearHide);
    final Activity a = r.activity;

    r.activity.makeVisible();

    ActivityManagerNative.getDefault().activityResumed(token);
}
```

```
// Activity
void makeVisible() {
    ViewManager wm = getWindowManager();
    wm.addView(mDecor, getWindow().getAttributes());
    mDecor.setVisibility(View.VISIBLE);
}
```

这个是resume Activity的时候调用的，
每启动一个Activity，AMS都有一个ActivityRecord，并且会有一个binder句柄类型的token跟这个ActivityRecord绑定。并且AMS调到应用时会带上这个token。ActivityThread里也会创建一个ActivityClientRecord，跟AMS中的ActivityRecord相对应，用一个map保存，key就是这个token。

所以这里performResumeActivity，先根据token从map里找到ActivityClientRecord，然后执行他的onResume回调，就返回了，然后设置r的window，Activity里的window是什么时候设置的，是Activity的attach函数中设置的，是个PhoneWindow，这个attach是performLaunchActivity调用的，这个是启动Activity的。包括创建Activity类，创建上下文，attach各类参数，执行onCreate回调，给ActivityClientRecord塞到缓存里。这个attach里setWindowManager其实是创建了一个WindowManagerImpl，里面负责和WMS通信，通信有一个WindowManagerGlobal负责，整个应用就这一个，不可能每个window都单独和WMS通信。通信是通过IWindowSession，这个保存在ViewRootImpl里了，这个session是WindowManagerGlobal单例里打开的，所有的ViewRootImpl公用。怎么区分不同的window呢，IWindowSession里所有的函数第一个参数就是IWindow，是个W类型的binder句柄，定义在ViewRootImpl里面，每次创建一个ViewRootImpl时，构造函数里都会创建一个W，所以可以理解为一个ViewRootImpl对应一个window，对应一个decorView。

```
// Activity
final void attach(Context context, ActivityThread aThread) {
    attachBaseContext(context);

    mWindow = new PhoneWindow(this);
    mWindow.setCallback(this);
    
    mUiThread = Thread.currentThread();

    mMainThread = aThread;
    
    mToken = token;
    
    mApplication = application;

    mWindow.setWindowManager(
            (WindowManager)context.getSystemService(Context.WINDOW_SERVICE),
            mToken, ...);

    mWindowManager = mWindow.getWindowManager();
}
```

回到上面的handleResumeActivity，PhoneWindow里面什么时候有个DecorView，这个DecorView是window的top level view，注释里是这么说的。是一个FrameLayout，包含导航栏，状态栏，还有我们自己的contentView。Activity里的decorView就是从PhoneWindow里获取的，我们在onCreate里setContentView的时候，就是调用PhoneWindow的setContentView，
这个installDecor就是创建DecorView了。

所以能有结论，在Activity的onCreate的时候，就给PhoneWindow和DecorView创建好了，onResume的时候调用了windowManager的addView，其实是创建了一个ViewRootImpl，然后调用他的setView函数，里面会执行一次requestLayout，然后调用IWindowSession的addToDisplay跟WMS通信。

所以我们算是明白了，通信通道一个应用进程只有一个，而每个Activity会默认创建一个PhoneWindow，里面有一个DecorView，同时对应一个ViewRootImpl，这个持有windowSession负责和WMS通信，同时持有IWindow用于和WMS双向调用。并且全局控制view的测量和绘制。

```
// PhoneWindow

// This is the top-level view of the window, containing the window decor.
private DecorView mDecor;

@Override
public final View getDecorView() {
    if (mDecor == null) {
        installDecor();
    }
    return mDecor;
}

@Override
public void setContentView(int layoutResID) {
    installDecor();
    mLayoutInflater.inflate(layoutResID, mContentParent);
}
```

我们继续看ViewRootImpl里面的setView，一个ViewRootImpl只能有一个DecorView，就是通过这个setView设置的，如果多次设置是不生效的。

```
public void setView(View view, WindowManager.LayoutParams attrs, View panelParentView) {
    synchronized (this) {
        if (mView == null) {
            mView = view;

            // Schedule the first layout -before- adding to the window
            // manager, to make sure we do the relayout before receiving
            // any other events from the system.
            requestLayout();
              
            res = mWindowSession.addToDisplay(mWindow, mSeq, mWindowAttributes,
                        getHostVisibility(), mDisplay.getDisplayId(),
                        mAttachInfo.mContentInsets, mAttachInfo.mStableInsets,
                        mAttachInfo.mOutsets, mInputChannel);
        }
    }
}
```

这先requestLayout，

```
@Override
public void requestLayout() {
    checkThread();
    mLayoutRequested = true;
    scheduleTraversals();
}

void scheduleTraversals() {
    if (!mTraversalScheduled) {
        mTraversalScheduled = true;
        mTraversalBarrier = mHandler.getLooper().getQueue().postSyncBarrier();
        mChoreographer.postCallback(
                Choreographer.CALLBACK_TRAVERSAL, mTraversalRunnable, null);
        if (!mUnbufferedInputDispatch) {
            scheduleConsumeBatchedInput();
        }
        notifyRendererOfFramePending();
        pokeDrawLockIfNeeded();
    }
}
```

这个scheduleTraversals会post一个traversalRunnable，里面会执行一个performTraversal，这个很复杂，不过里面会执行一个relayoutWindow，会调到IWindowSession的relayout，WMS中会通过createSurfaceLocked创建一个SurfaceControl，注意一下relayoutWindow的时候，会带上一个Surface，只不过这个Surface只是个空壳，到了WMS中，创建好了surfaceControl之后，会拷贝到这个surface中。

好了，requestLayout完了就该addToDisplay了，这个其实调到了WMS中的addWindow函数，里面会先创建一个WindowState，然后执行他的attach函数，这个函数会执行windowAddedLocked,这会创建一个SurfaceSession，跟surfaceFlinger通信。这里面会创建一个SurfaceComposerClient,
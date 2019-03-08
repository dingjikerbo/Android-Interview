

我们在UI线程刷新界面的时候，可能会遇到这个异常，

```
Only the original thread that created a view hierarchy can touch its views.
```

搜一下这个是哪抛的呢？

是ViewRootImpl里面，

```
void checkThread() {
    if (mThread != Thread.currentThread()) {
        throw new CalledFromWrongThreadException(
                "Only the original thread that created a view hierarchy can touch its views.");
    }
}
```

就是说当前线程如果不是mThread就抛异常了，看来mThread是UI线程。mThread是ViewRootImpl构造函数所在的线程，

```
public ViewRootImpl(Context context, Display display) {
    ......
    mThread = Thread.currentThread();
}
```

而ViewRootImpl是哪里构造的呢？是在WindowManagerGlobal类中的，

```
public void addView(View view, ...) {
    ......
    root = new ViewRootImpl(view.getContext(), display);
    ......
}
```

再往上是WindowManagerImpl的addView函数，

```
@Override
public void addView(@NonNull View view, @NonNull ViewGroup.LayoutParams params) {
    applyDefaultToken(params);
    mGlobal.addView(view, params, mDisplay, mParentWindow);
}
```

这个再往上是Activity这个类里面，

```
void makeVisible() {
    if (!mWindowAdded) {
        ViewManager wm = getWindowManager();
        wm.addView(mDecor, getWindow().getAttributes());
        mWindowAdded = true;
    }
    mDecor.setVisibility(View.VISIBLE);
}
```

这个再往上是ActivityThread里的handleResumeActivity，当然还有一些函数会调到这个，但是都没这个早。所以如果在这之前

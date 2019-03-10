

```
void SurfaceFlinger::init() {
    // Initialize the H/W composer object.  There may or may not be an
    // actual hardware composer underneath.
    mHwc = new HWComposer(this,
            *static_cast<HWComposer::EventHandler *>(this));
}
```

```
HWComposer::HWComposer(
        const sp<SurfaceFlinger>& flinger,
        EventHandler& handler)
    : mFlinger(flinger),
      mFbDev(0), mHwc(0), mNumDisplays(1),
      mCBContext(new cb_context),
      mEventHandler(handler),
      mDebugForceFakeVSync(false)
{
    if (needVSyncThread) {
        // we don't have VSYNC support, we need to fake it
        mVSyncThread = new VSyncThread(*this);
    }
}

// this class is only used to fake the VSync event on systems that don't
// have it.
class VSyncThread : public Thread {
    HWComposer& mHwc;
    mutable Mutex mLock;
    Condition mCondition;
    bool mEnabled;
    mutable nsecs_t mNextFakeVSync;
    nsecs_t mRefreshPeriod;
    virtual void onFirstRef();
    virtual bool threadLoop();
public:
    VSyncThread(HWComposer& hwc);
    void setEnabled(bool enabled);
};
```
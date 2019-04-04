

如果binder驱动层返回BR_FAILED_REPLY指令，err就是FAILED_TRANSACTION，
```
status_t IPCThreadState::waitForResponse(Parcel *reply, status_t *acquireResult)
{
    uint32_t cmd;
    int32_t err;

    while (1) {
        if ((err=talkWithDriver()) < NO_ERROR) break;
        err = mIn.errorCheck();
        if (err < NO_ERROR) break;
        if (mIn.dataAvail() == 0) continue;
        
        cmd = (uint32_t)mIn.readInt32();
        
        switch (cmd) {
            ......

            case BR_FAILED_REPLY:
                err = FAILED_TRANSACTION;
                goto finish;
            ......
        }
    }
    return err;
}
```


这里kzalloc如果返回null，就返回BR_FAILED_REPLY.
下面binder_alloc_buf如果返回null，也返回BR_FAILED_REPLY.
copy_from_user失败，也返回BR_FAILED_REPLY

这个kzalloc是在内核空间分配内存，一般不会有问题。
但是binder_alloc_buf是在binder驱动为进程分配的缓冲区中分配的，
```
static void binder_transaction(struct binder_proc *proc,
                   struct binder_thread *thread,
                   struct binder_transaction_data *tr, int reply) {
    ......
    e->to_proc = target_proc->pid;
    
    t = kzalloc(sizeof(*t), GFP_KERNEL);
    if (t == NULL) {
        return_error = BR_FAILED_REPLY;
        goto err_alloc_t_failed;
    }

    tcomplete = kzalloc(sizeof(*tcomplete), GFP_KERNEL);
    if (tcomplete == NULL) {
        return_error = BR_FAILED_REPLY;
        goto err_alloc_tcomplete_failed;
    }

    ......

    t->buffer = binder_alloc_buf(target_proc, tr->data_size,
        tr->offsets_size, !reply && (t->flags & TF_ONE_WAY));
    if (t->buffer == NULL) {
        return_error = BR_FAILED_REPLY;
        goto err_binder_alloc_buf_failed;
    }
    
    ......

    if (copy_from_user(t->buffer->data, (const void __user *)(uintptr_t)
               tr->data.ptr.buffer, tr->data_size)) {
        return_error = BR_FAILED_REPLY;
        goto err_copy_data_failed;
    }

    if (copy_from_user(offp, (const void __user *)(uintptr_t)
               tr->data.ptr.offsets, tr->offsets_size)) {
        binder_user_error("%d:%d got transaction with invalid offsets ptr\n",
                proc->pid, thread->pid);
        return_error = BR_FAILED_REPLY;
        goto err_copy_data_failed;
    }
    ......
}
```

看异常是调到下面的mRemote的transact里失败了，咱们看这里的intent.writeToParcel的实现，

```
public int startActivity(IApplicationThread caller, String callingPackage, Intent intent,
            String resolvedType, IBinder resultTo, String resultWho, int requestCode,
            int startFlags, ProfilerInfo profilerInfo, Bundle options) throws RemoteException {
    Parcel data = Parcel.obtain();
    Parcel reply = Parcel.obtain();
    data.writeInterfaceToken(IActivityManager.descriptor);
    data.writeStrongBinder(caller != null ? caller.asBinder() : null);
    data.writeString(callingPackage);
    intent.writeToParcel(data, 0);
    data.writeString(resolvedType);
    data.writeStrongBinder(resultTo);
    data.writeString(resultWho);
    data.writeInt(requestCode);
    data.writeInt(startFlags);
    if (profilerInfo != null) {
        data.writeInt(1);
        profilerInfo.writeToParcel(data, Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
    } else {
        data.writeInt(0);
    }
    if (options != null) {
        data.writeInt(1);
        options.writeToParcel(data, 0);
    } else {
        data.writeInt(0);
    }
    mRemote.transact(START_ACTIVITY_TRANSACTION, data, reply, 0);
    reply.readException();
    int result = reply.readInt();
    reply.recycle();
    data.recycle();
    return result;
}
```

看看intent的writeToParcel的实现，其实就是给intent里的数据都写到这个叫out的parcel里，我们重点关注里面的Bundle，就是这个mExtras。
```
public void writeToParcel(Parcel out, int flags) {
    ......
    out.writeBundle(mExtras);
}
```

再看parcel的writeBundle函数，调到了Bundle自己的writeToParcel函数，

```
public final void writeBundle(Bundle val) {
    ......
    val.writeToParcel(this, 0);
}
```

看看Bundle的writeToParcel函数，

```
public void writeToParcel(Parcel parcel, int flags) {
    ......
    super.writeToParcelInner(parcel, flags);    
}
```

super就是BaseBundle这个类，他的writeToParcelInner函数如下，Bundle就是一个Map，这里的writeToParcelInner函数是给Bundle里的Map序列化到parcel里。


```
void writeToParcelInner(Parcel parcel, int flags) {
    ......
    int lengthPos = parcel.dataPosition();
    parcel.writeInt(-1); // dummy, will hold length
    parcel.writeInt(BUNDLE_MAGIC);

    int startPos = parcel.dataPosition();
    parcel.writeArrayMapInternal(mMap);
    int endPos = parcel.dataPosition();

    // Backpatch length
    parcel.setDataPosition(lengthPos);
    int length = endPos - startPos;
    parcel.writeInt(length);
    parcel.setDataPosition(endPos);
}
```

看看这个writeArrayMapInternal函数，先写map的size，再遍历map，先写key，再写value。
```
/**
 * Flatten an ArrayMap into the parcel at the current dataPosition(),
 * growing dataCapacity() if needed.  The Map keys must be String objects.
 */
void writeArrayMapInternal(ArrayMap<String, Object> val) {
    final int N = val.size();
    writeInt(N);
    int startPos;
    for (int i=0; i<N; i++) {
        writeString(val.keyAt(i));
        writeValue(val.valueAt(i));
    }
}
```

咱们重点看writeValue，这里根据value的type来有不同的写法，咱们重点关注Bitmap，他是parcelable的，先写了个类型的id，再调writeParcelable，

```
public final void writeValue(Object v) {
    ......
    else if (v instanceof Parcelable) {
        writeInt(VAL_PARCELABLE);
        writeParcelable((Parcelable) v, 0);
    } 
    ......
}
```

看看这个writeParcelable的实现，这里重点调了Bitmap的writeToParcel的实现，
```
public final void writeParcelable(Parcelable p, int parcelableFlags) {
    writeParcelableCreator(p);
    p.writeToParcel(this, parcelableFlags);
}
```

这里调了个native函数，
```
public void writeToParcel(Parcel p, int flags) {
    nativeWriteToParcel(mFinalizer.mNativeBitmap, mIsMutable, mDensity, p);
}
```

看看native层的实现，这里先拿到native层的parcel对象，然后拿到native层bitmap对象，然后拿到SkBitmap，向native层parcel对象里写了一堆bitmap相关的东西，比如宽高什么的，
然后从native层bitmap对象中，获取ashmemFd，如果拿到了，并且isMutable是false，并且allowFds是true，就向natice层parcel对象中写入这个fd，就直接返回了，想必这样是不会
导致binder transaction too large的。但是如果继续往下走，就要涉及到拷贝了，这个WritableBlob是啥？

其实就是创建了一个Blob对象，从parcel里分配一块地址，给bitmap的pixels拷贝过来。

```
static jboolean Bitmap_writeToParcel(JNIEnv* env, jobject,
                                     jlong bitmapHandle,
                                     jboolean isMutable, jint density,
                                     jobject parcel) {
    android::Parcel* p = android::parcelForJavaObject(env, parcel);
    SkBitmap bitmap;

    android::Bitmap* androidBitmap = reinterpret_cast<Bitmap*>(bitmapHandle);
    androidBitmap->getSkBitmap(&bitmap);

    p->writeInt32(isMutable);
    p->writeInt32(bitmap.colorType());
    p->writeInt32(bitmap.alphaType());
    p->writeInt32(bitmap.width());
    p->writeInt32(bitmap.height());
    p->writeInt32(bitmap.rowBytes());
    p->writeInt32(density);

    ......

    // Transfer the underlying ashmem region if we have one and it's immutable.
    android::status_t status;
    int fd = androidBitmap->getAshmemFd();
    if (fd >= 0 && !isMutable && p->allowFds()) {
        status = p->writeDupImmutableBlobFileDescriptor(fd);
        return JNI_TRUE;
    }

    // Copy the bitmap to a new blob.
    bool mutableCopy = isMutable;
    size_t size = bitmap.getSize();
    android::Parcel::WritableBlob blob;
    p->writeBlob(size, mutableCopy, &blob);

    bitmap.lockPixels();
    const void* pSrc =  bitmap.getPixels();
    memcpy(blob.data(), pSrc, size);
    bitmap.unlockPixels();

    blob.release();
    return JNI_TRUE;
}
```

看看这个WritableBlob，继承Blob，Blob里有个mData，还有mFd，回到上面这个Bitmap_writeToParcel函数，看看parcel的writeBlob函数，
```
class WritableBlob : public Blob {
    friend class Parcel;
public:
    inline void* data() { return mData; }
};

class Blob {
public:
    Blob();
    ~Blob();

    void clear();
    void release();
    inline size_t size() const { return mSize; }
    inline int fd() const { return mFd; };
    inline bool isMutable() const { return mMutable; }

protected:
    void init(int fd, void* data, size_t size, bool isMutable);

    int mFd; // owned by parcel so not closed when released
    void* mData;
    size_t mSize;
    bool mMutable;
};
```


这个writeBlob是干嘛的呢？这个BLOB_INPLACE_LIMIT定义为16K，意思是可以就地存储的最大size。

如果不允许fd传输，并且小于这个limit，就准备就地存储。这个blob的init函数，分别是初始化mFd，mData，mSize和mMutable。这里mFd是-1，mData就是parcel里的一块空间的地址，mutable是false表示不会变。

如果允许fd传输，并且数据量非常大，那就走ashmem吧，这创建了ashmem，用一个fd表示，映射了一块内存，如果bitmap不可变，就给这块内存权限改为只读，然后给这个fd写到parcel里。
```
status_t Parcel::writeBlob(size_t len, bool mutableCopy, WritableBlob* outBlob) {
    status_t status;
    if (!mAllowFds || len <= BLOB_INPLACE_LIMIT) {
        status = writeInt32(BLOB_INPLACE);
        void* ptr = writeInplace(len);
        outBlob->init(-1, ptr, len, false);
        return NO_ERROR;
    }

    int fd = ashmem_create_region("Parcel Blob", len);

    int result = ashmem_set_prot_region(fd, PROT_READ | PROT_WRITE);
    void* ptr = ::mmap(NULL, len, PROT_READ | PROT_WRITE, MAP_SHARED, fd, 0);
    if (!mutableCopy) {
        result = ashmem_set_prot_region(fd, PROT_READ);
    }
    writeInt32(mutableCopy ? BLOB_ASHMEM_MUTABLE : BLOB_ASHMEM_IMMUTABLE);
    writeFileDescriptor(fd, true /*takeOwnership*/);
    outBlob->init(fd, ptr, len, mutableCopy);
    return NO_ERROR;
}
```

总结一下，如果不允许fd或者Bitmap比较小，就从Parcel里分配一块空间，将Bitmap拷进去。如果允许fd并且bitmap很大，就另外创建一个ashmem，映射出一块内存，给Bitmap考进去，
给fd写到parcel里。

所以，这里关键是parcel里的mAllowFds，如果这个设为false，就算你图片再大，都不会自动转为ashMem。这个mAllowFds哪里设置的呢？native层Parcel对象的构造函数中会调
initState函数，这里面会设置mAllowFds为true。

```
restartWrite -> true
initState -> true
```

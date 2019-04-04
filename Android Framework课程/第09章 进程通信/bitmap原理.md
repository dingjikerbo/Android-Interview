bitmap实现了Parcelable接口，就是说可以跨进程传递，

```
public void writeToParcel(Parcel p, int flags) {
    nativeWriteToParcel(mFinalizer.mNativeBitmap, mIsMutable, mDensity, p);
}
```

这个bitmap的writeToParcel就是往native层parcel对象里写了一堆SkBitmap的参数，值得注意的是
bitmap里还有一个ashmem fd，这是干嘛的？
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

    if (bitmap.colorType() == kIndex_8_SkColorType) {
        SkColorTable* ctable = bitmap.getColorTable();
        if (ctable != NULL) {
            int count = ctable->count();
            p->writeInt32(count);
            memcpy(p->writeInplace(count * sizeof(SkPMColor)),
                   ctable->readColors(), count * sizeof(SkPMColor));
        } else {
            p->writeInt32(0);   // indicate no ctable
        }
    }

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
    status = p->writeBlob(size, mutableCopy, &blob);

    bitmap.lockPixels();
    const void* pSrc =  bitmap.getPixels();
    memcpy(blob.data(), pSrc, size);

    bitmap.unlockPixels();

    blob.release();
    return JNI_TRUE;
}
```

看看parcel的writeInPlace函数，就是返回了这个len长的数据在parcel里存储的位置。

```
void* Parcel::writeInplace(size_t len) {
    const size_t padded = pad_size(len);

    if ((mDataPos+padded) <= mDataCapacity) {
restart_write:
        uint8_t* const data = mData+mDataPos;

        // Need to pad at end?
        if (padded != len) {
            *reinterpret_cast<uint32_t*>(data+padded-4) &= mask[padded-len];
        }

        finishWrite(padded);
        return data;
    }

    status_t err = growData(padded);
    if (err == NO_ERROR) goto restart_write;
    return NULL;
}
```



## Bitmap里的fd是什么时候设置的？
Bitmap有个构造函数，这带了个fd，这个构造函数哪里调到了呢？

```
Bitmap::Bitmap(void* address, int fd,
            const SkImageInfo& info, size_t rowBytes, SkColorTable* ctable)
        : mPixelStorageType(PixelStorageType::Ashmem) {
    mPixelStorage.ashmem.address = address;
    mPixelStorage.ashmem.fd = fd;
    mPixelStorage.ashmem.size = ashmem_get_size_region(fd);
    ......
}
```

有以下两个函数，

```
GraphicsJNI::allocateAshmemPixelRef <--- AshmemPixelAllocator::allocPixelRef
GraphicsJNI::mapAshmemPixelRef
```

对比一下，不带fd的Bitmap构造函数，
```

<----- GraphicsJNI::allocateJavaPixelRef
<-----
1、Bitmap_createFromParcel
2、Bitmap_creator
3、JavaPixelAllocator::allocPixelRef
```

AshmemPixelAllocator什么时候用到呢？以下函数

```
Bitmap_copyAshmem
```

咱们看native层的decodeStream实现，看哈，这里假设Options为null，那么这里javaBitmap也为null了，因为javaBitmap是Options里的inBitmap这个字段。
这里的outputAllocator是JavaPixelAllocator。

```
static jobject doDecode(JNIEnv* env, SkStreamRewindable* stream, jobject padding, jobject options) {
    SkImageDecoder* decoder = SkImageDecoder::Factory(stream);

    JavaPixelAllocator javaAllocator(env);
    SkBitmap::Allocator* outputAllocator = &javaAllocator;

    decoder->setSkipWritingZeroes(outputAllocator == &javaAllocator);
    decoder->setAllocator(outputAllocator);

    SkBitmap decodingBitmap;
    decoder->decode(stream, &decodingBitmap, prefColorType, decodeMode);

    SkBitmap outputBitmap;
    outputBitmap.swap(decodingBitmap);

    // now create the java bitmap
    return GraphicsJNI::createBitmap(env, javaAllocator.getStorageObjAndReset(),
            bitmapCreateFlags, ninePatchChunk, ninePatchInsets, -1);
}

```
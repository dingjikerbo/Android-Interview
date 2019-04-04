
这里指定一个Len，然后writeInPlace，返回一个地址，是干嘛的？其实就是从parcel里为这个len长的数据找一块安身之地。
```
void* Parcel::writeInplace(size_t len) {
    uint8_t* const data = mData + mDataPos;
    return data;
}
```

parcel里写个文件描述符，也要创建一个flat_binder_object，只不过type是fd类型，存到handle字段里。
```
status_t Parcel::writeFileDescriptor(int fd, bool takeOwnership)
{
    flat_binder_object obj;
    obj.type = BINDER_TYPE_FD;
    obj.flags = 0x7f | FLAT_BINDER_FLAG_ACCEPTS_FDS;
    obj.binder = 0; /* Don't pass uninitialized stack data to a remote process */
    obj.handle = fd;
    obj.cookie = takeOwnership ? 1 : 0;
    return writeObject(obj, true);
}
```
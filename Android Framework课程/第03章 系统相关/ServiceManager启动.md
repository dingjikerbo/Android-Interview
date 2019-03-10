# ServiceManager的启动

ServiceManager是Binder IPC通信过程中的守护进程，本身也是一个Binder服务，但并没有采用libbinder中的多线程模型来与Binder驱动通信，而是自行编写了binder.c直接和Binder驱动来通信，并且只有一个循环binder_loop来进行读取和处理事务，这样的好处是简单而高效。
ServiceManager本身工作相对简单，其功能：查询和注册服务。 

init.rc中配置了ServiceManager，所以ServiceManager由INIT拉起来，并且
是critical级别的，如果serviceManager挂了，那么整个Android都要重启。

init进程拉起来ServiceManager之后，会执行到如下
service_manager.c

看其main函数，

```
int main(int argc, char **argv)
{
    struct binder_state *bs;

    // 打开binder驱动，申请128k字节大小的内存空间
    bs = binder_open(128*1024);

    // 成为上下文管理者，整个系统中只有一个这样的管理者
    binder_become_context_manager(bs);

    //进入无限循环，处理client端发来的请求，这里执行svcmgr_handler
    binder_loop(bs, svcmgr_handler);

    return 0;
}
```

再来看binder_open，在binder.c中，这是ServiceManager自己专有的，

```
struct binder_state *binder_open(size_t mapsize)
{
    struct binder_state *bs;
    struct binder_version vers;

    bs = malloc(sizeof(*bs));

    // 通过系统调用陷入内核，打开Binder设备驱动
    bs->fd = open("/dev/binder", O_RDWR);

    // 通过系统调用，ioctl获取binder版本信息
    if ((ioctl(bs->fd, BINDER_VERSION, &vers) == -1) ||
        (vers.protocol_version != BINDER_CURRENT_PROTOCOL_VERSION)) {
        return NULL; //内核空间与用户空间的binder不是同一版本
    }

    bs->mapsize = mapsize;
    // 通过系统调用，mmap内存映射，mmap必须是page的整数倍
    bs->mapped = mmap(NULL, mapsize, PROT_READ, MAP_PRIVATE, bs->fd, 0);

    return bs;
}

int binder_become_context_manager(struct binder_state *bs) {
    // 通过ioctl，传递BINDER_SET_CONTEXT_MGR指令
    return ioctl(bs->fd, BINDER_SET_CONTEXT_MGR, 0);
}
```

再来看binder_loop函数，很简单，在一个for循环里，首先读消息，这个BINDER_WRITE_READ既可以读也可以写，具体要看bwr.read_size和bwr.write_size，这里write_size为0，设置了read_size，所以这里只执行读操作。

```
void binder_loop(struct binder_state *bs, binder_handler func)
{
    int res;
    struct binder_write_read bwr;
    uint32_t readbuf[32];

    bwr.write_size = 0;
    bwr.write_consumed = 0;
    bwr.write_buffer = 0;

    readbuf[0] = BC_ENTER_LOOPER;

    // 将BC_ENTER_LOOPER命令发送给binder驱动，让Service Manager进入循环
    binder_write(bs, readbuf, sizeof(uint32_t));

    for (;;) {
        bwr.read_size = sizeof(readbuf);
        bwr.read_consumed = 0;
        bwr.read_buffer = (uintptr_t) readbuf;

        // 进入循环，不断地binder读写过程
        res = ioctl(bs->fd, BINDER_WRITE_READ, &bwr);

        // 解析binder信息
        res = binder_parse(bs, 0, (uintptr_t) readbuf, bwr.read_consumed, func);
    }
}
```

而这里的binder_write也是ioctl调用，

```
int binder_write(struct binder_state *bs, void *data, size_t len)
{
    struct binder_write_read bwr;
    int res;

    bwr.write_size = len;
    bwr.write_consumed = 0;
    bwr.write_buffer = (uintptr_t) data; //此处data为BC_ENTER_LOOPER
    bwr.read_size = 0;
    bwr.read_consumed = 0;
    bwr.read_buffer = 0;

    res = ioctl(bs->fd, BINDER_WRITE_READ, &bwr);
    return res;
}
```

再来看binder_parse,

```
int binder_parse(struct binder_state *bs, struct binder_io *bio,
                 uintptr_t ptr, size_t size, binder_handler func)
{
    int r = 1;
    uintptr_t end = ptr + (uintptr_t) size;

    while (ptr < end) {
        uint32_t cmd = *(uint32_t *) ptr;
        ptr += sizeof(uint32_t);
        switch(cmd) {
        case BR_NOOP:
            break;
        case BR_TRANSACTION_COMPLETE:
            break;
        case BR_INCREFS:
        case BR_ACQUIRE:
        case BR_RELEASE:
        case BR_DECREFS:
            ptr += sizeof(struct binder_ptr_cookie);
            break;
        case BR_TRANSACTION: {
            struct binder_transaction_data *txn = (struct binder_transaction_data *) ptr;
            binder_dump_txn(txn);
            if (func) {
                unsigned rdata[256/4];
                struct binder_io msg;
                struct binder_io reply;
                int res;

                bio_init(&reply, rdata, sizeof(rdata), 4);
                bio_init_from_txn(&msg, txn);
                res = func(bs, txn, &msg, &reply);
                binder_send_reply(bs, &reply, txn->data.ptr.buffer, res);
            }
            ptr += sizeof(*txn);
            break;
        }
        case BR_REPLY: {
            struct binder_transaction_data *txn = (struct binder_transaction_data *) ptr;
            binder_dump_txn(txn);
            if (bio) {
                bio_init_from_txn(bio, txn);
                bio = 0;
            } else {
                /* todo FREE BUFFER */
            }
            ptr += sizeof(*txn);
            r = 0;
            break;
        }
        case BR_DEAD_BINDER: {
            struct binder_death *death = (struct binder_death *)(uintptr_t) *(binder_uintptr_t *)ptr;
            ptr += sizeof(binder_uintptr_t);
            death->func(bs, death->ptr);
            break;
        }
        case BR_FAILED_REPLY:
            r = -1;
            break;
        case BR_DEAD_REPLY:
            r = -1;
            break;
        default:
            ALOGE("parse: OOPS %d\n", cmd);
            return -1;
        }
    }

    return r;
}
```

我们来看ServiceManager.addService的实现，这里关键是先拿到ServiceManager的binder句柄，关键函数是BinderInternal.getContextObject

```
private static IServiceManager getIServiceManager() {
    if (sServiceManager != null) {
        return sServiceManager;
    }

    // Find the service manager
    sServiceManager = ServiceManagerNative.asInterface(BinderInternal.getContextObject());
    return sServiceManager;
}

public static void addService(String name, IBinder service) {
    try {
        getIServiceManager().addService(name, service, false);
    } catch (RemoteException e) {
        Log.e(TAG, "error in addService", e);
    }
}
```

```
static jobject android_os_BinderInternal_getContextObject(JNIEnv* env, jobject clazz)
{
    sp<IBinder> b = ProcessState::self()->getContextObject(NULL);
    return javaObjectForIBinder(env, b);
}
```

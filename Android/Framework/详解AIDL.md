# 详解AIDL

创建一个简单的AIDL文件，

```
interface IMyAidlInterface {
    void test(int a, int b);
}
```

生成如下对应的Java文件，最外层是接口IMyAidlInterface，继承自IInterface接口，这个IInterface只有一个asBinder函数，即用于返回Stub和Proxy对应的IBinder。

这里生成了两个类，IMyAidlInterface.Stub以及IMyAidlInterface.Stub.Proxy，注意到Stub类是个抽象类，所以当创建Stub类时要实现IMyAidlInterface的函数。此外Stub继承自Binder，而Proxy只是实现了IMyAidlInterface接口，Proxy构造函数中有个IBinder，名为mRemote，其asBinder返回的就是这个mRemote。而IMyAidlInterface的实现就是给参数塞到Parcel中，然后通过mRemote transact出去。我们稍后要搞清楚这个Proxy构造函数中的IBinder是哪里传进来的。

再回过来看Stub类，由于继承自Binder，所以它自己就是一个IBinder对象，所以asBinder直接返回自己。看其构造函数只是attachInterface，即给传入的两个变量保存起来。再看asInterface，我们bindService的时候，在onServiceConnected中会获得Service返回的IBinder，我们拿着IBinder，调用IMyAidlInterface.Stub.asInterface即可拿到一个IMyAidlInterface类对象，看看这里的asInterface的实现。首先queryLocalInterface，这是IBinder接口的函数，这里查询的是之前通过attachInterface传入的两参数。我们注意，Binder和BinderProxy两个类，Binder是实体类，BinderProxy是代理类，两者都实现了IBinder接口，不过BinderProxy的queryLocalInterface直接返回null，Binder的queryLocalInterface由于构造函数中调过attachInterface，所以queryLocalInterface返回非空。回到asInterface函数中，如果IBinder是个Binder，那么说明此处的IBinder是实体的，直接转成接口即可用。否则表示IBinder是BinderProxy，需要外层封装一个壳返回给应用层。

```
public interface IMyAidlInterface extends android.os.IInterface {

    public static abstract class Stub extends android.os.Binder implements com.inuker.investment.IMyAidlInterface {
        private static final java.lang.String DESCRIPTOR = "com.inuker.investment.IMyAidlInterface";

        public Stub() {
            this.attachInterface(this, DESCRIPTOR);
        }

        public static com.inuker.investment.IMyAidlInterface asInterface(android.os.IBinder obj) {
            if ((obj == null)) {
                return null;
            }
            android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (((iin != null) && (iin instanceof com.inuker.investment.IMyAidlInterface))) {
                return ((com.inuker.investment.IMyAidlInterface) iin);
            }
            return new com.inuker.investment.IMyAidlInterface.Stub.Proxy(obj);
        }

        @Override
        public android.os.IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException {
            java.lang.String descriptor = DESCRIPTOR;
            switch (code) {
                case INTERFACE_TRANSACTION: {
                    reply.writeString(descriptor);
                    return true;
                }
                case TRANSACTION_test: {
                    data.enforceInterface(descriptor);
                    int _arg0;
                    _arg0 = data.readInt();
                    int _arg1;
                    _arg1 = data.readInt();
                    this.test(_arg0, _arg1);
                    reply.writeNoException();
                    return true;
                }
                default: {
                    return super.onTransact(code, data, reply, flags);
                }
            }
        }

        private static class Proxy implements com.inuker.investment.IMyAidlInterface {
            private android.os.IBinder mRemote;

            Proxy(android.os.IBinder remote) {
                mRemote = remote;
            }

            @Override
            public android.os.IBinder asBinder() {
                return mRemote;
            }

            public java.lang.String getInterfaceDescriptor() {
                return DESCRIPTOR;
            }

            @Override
            public void test(int a, int b) throws android.os.RemoteException {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(a);
                    _data.writeInt(b);
                    mRemote.transact(Stub.TRANSACTION_test, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        static final int TRANSACTION_test = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    }

    public void test(int a, int b) throws android.os.RemoteException;
}
```

总结一下，整个Binder通信架构分五层，最上面业务层，中间Stub和Proxy层，再下面是Binder和BinderProxy层，再往下就是Native层的Binder和BinderProxy，再往下就是Binder驱动。
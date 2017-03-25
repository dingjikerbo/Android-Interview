BinderHook从字面意思上理解就是给Binder下钩子，拦截Binder调用。应用场景有很多，比如我们不允许插件中获取WakeLock或调用某些我们不建议调用的接口，但是我们
无法保证他们遵守了这些约定，毕竟插件是他们开发的，当然我们可以给代码要过来进行审核，但是这显然违背了我们的初衷。有个观点我很有感触，国外的IT公司遇到问题
通常想到的是通过工具来解决，而国内通常是通过加班。我是个比较懒的人，喜欢找些一劳永逸的解决方案，而BinderHook就是我比较喜欢的，通过在系统底层下钩子，可以
拦截系统调用，动态监测调用情况，修改调用参数以及调用的返回结果，相当于打开了上帝视角。本文先介绍ServiceManager的BinderHook，所谓打蛇打七寸，ServiceManager
就可以算得上是Binder的七寸。

我们先看看ServiceManager的源码：

```
public final class ServiceManager {
    private static final String TAG = "ServiceManager";

    private static IServiceManager sServiceManager;
    private static HashMap<String, IBinder> sCache = new HashMap<String, IBinder>();

    private static IServiceManager getIServiceManager() {
        if (sServiceManager != null) {
            return sServiceManager;
        }

        sServiceManager = ServiceManagerNative.asInterface(BinderInternal.getContextObject());
        return sServiceManager;
    }

    public static IBinder getService(String name) {
        try {
            IBinder service = sCache.get(name);
            if (service != null) {
                return service;
            } else {
                return getIServiceManager().getService(name);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "error in getService", e);
        }
        return null;
    }
    
    ......
}

```

实现非常简单，一个IServiceManager，一个是Binder的cache。在getService的时候先看cache中是否找得到，没有的话调IServiceManager继续找，不过奇怪的是返回结果没有保存到cache里。
不过想想也正常，cache里的通常是系统服务，不会轻易注销的，而有些服务则不然，可能会被卸载的，那这里就没必要放在cache里了，而是每次用到都必须查一遍。

关于ServiceManager的Hook，有两个点，一个是IServiceManager，一个是cache。本文先谈IServiceManager的BinderHook。

这个sServiceManager是ServiceManager中的静态内部变量，可以通过反射获取，我们先拿到其原始Binder，生成一个代理的Binder，使其queryLocalInterface返回sServiceManager的代理。


# 9.1 Android Framework层用到了哪些跨进程通信方式，请举例说明？

# 9.2 应用程序是怎么支持binder机制的？

# 9.3 说说你对binder的理解


# 9.4 说说binder的oneway

```
oneway interface caller {
    void call();
}
```

如果是同一个client，在不同的线程调了多次call函数，如果service那边没处理完，就会自动给这些对call的调用序列化，串行化地一个个处理。如果不是oneway就不会这样，不是oneway的话，同一个client,在不同的线程掉call函数，service那边都会尽可能并行处理，可能开多个binder线程。

那对于oneway，如果不是同一个client呢？这么说吧，只要是对于这个service的同一个函数的call，不管client端来自于哪个进程，还是哪个线程，所有的请求都会被串行化。

那不同函数的call呢？结果很令人吃惊，一样会被串行化。

这么说吧，只要这个interface被定义为oneway的，那么对于这个interface的不管哪个函数的call，都会被串行化。比如应用进程A调用了fun1，结果fun1很耗时堵塞了，那么应用进程B调用fun2，虽然会马上返回，但是service会等fun1执行完了才执行fun2。

# 9.5 intent传递数据大小有限制么，怎么解决？


# 9.6 一次完整的IPC通信流程是怎样的？

# 9.7 Parcel的readStringBinder和writeStrongbinder的原理是什么？

# 9.8 pingBinder/isBinderAlive/DeathReceipt





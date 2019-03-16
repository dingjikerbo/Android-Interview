
做个试验，
- 一个Activity两次BindService，都用一个ServiceConnection，，结果onServiceConnected只会掉一次，而且service的onBind只调一次，onRebind没调
- 如果一个Activity两次bindService，用两个ServiceConnection，结果这两个ServiceConnection分别回调一次。onRebind没掉
- 如果用Activity和Application的context，两次bindService，用同一个ServiceConnection，结果会回调两次，onRebind没掉
- 如果用Activity和Application的context，两次bindService，用两个ServiceConnection，结果会回调两次，onRebind没掉

那么onRebind什么时候调的，一个ServiceConnection被不同的context bindService，不会调onRebind。

onServiceDisconnected在连接正常关闭的情况下是不会被调用的，比如你unbindService，这个是不会回调的。除非service进程被杀死了，比如自杀。

为什么onRebind没掉呢？因为service的onUnBind默认返回false，这样onRebind是不会调的。
给他改成返回true，

给onUnbind改成返回true,
- 一个Activity先bindService，然后unbind，然后再bindService，用的同一个ServiceConnection，结果onRebind会调的。如果这里没有unBind，是不会onRebind的，并且onServiceConnected只调一次。

- 一个Activity先bindService,然后unbind，然后再bindService，用的另一个ServiceConnection，结果onRebind会调到。如果这里没有unBind，是不会onRebind的，并且onServiceConnected调两次。

- 如果用Activity先bindService，然后unbind，然后用Application再bindService，用同一个ServiceConnection，结果onServiceConnected会回调两次，onRebind也会调

- 如果用Activity先bindService，然后unbind，然后用Application再bindService，用另一个ServiceConnection，结果onServiceConnected会回调两次，onRebind也会调



# 总结
onRebind调用条件，首先service的onUnbind要返回true，另外当应用的进程跟Service彻底unbind之后，再bindService，才会触发onRebind。什么叫彻底unBind，就是所有的context，所有的serviceConnection都unbind过了。

onServiceConnected触发条件是新的链路建立，新的链路包含两个元素，一个是context，一个是ServiceConnection。这两个有一个不同，就不是同一个链路。

首先获取serviceRecord，然后获取AppBindRecord，

对于一个service来说，ConnectionRecord是最直接的，这个要跟IServiceConnection挂钩，然后一个IServiceConnection可能被多个Context bindService吧，
```
int bindServiceLocked(IApplicationThread caller, IBinder token, ...){
    ServiceLookupResult res = retrieveServiceLocked(service, ...);

    ServiceRecord s = res.record;

    AppBindRecord b = s.retrieveAppBindingLocked(service, callerApp);
    ConnectionRecord c = new ConnectionRecord(b, null, connection, ...);

    IBinder binder = connection.asBinder();
    ArrayList<ConnectionRecord> clist = s.connections.get(binder);
    if (clist == null) {
        clist = new ArrayList<ConnectionRecord>();
        s.connections.put(binder, clist);
    }
    clist.add(c);
    b.connections.add(c);

    b.client.connections.add(c);
    if ((c.flags&Context.BIND_ABOVE_CLIENT) != 0) {
        b.client.hasAboveClient = true;
    }
    if (s.app != null) {
        updateServiceClientActivitiesLocked(s.app, c, true);
    }
    clist = mServiceConnections.get(binder);
    if (clist == null) {
        clist = new ArrayList<ConnectionRecord>();
        mServiceConnections.put(binder, clist);
    }
    clist.add(c);

    if ((flags&Context.BIND_AUTO_CREATE) != 0) {
        if (bringUpServiceLocked(s, service.getFlags(), callerFg, false) != null) {
            return 0;
        }
    }

    if (s.app != null && b.intent.received) {
        c.conn.connected(s.name, b.intent.binder);

        // If this is the first app connected back to this binding,
        // and the service had previously asked to be told when
        // rebound, then do so.
        if (b.intent.apps.size() == 1 && b.intent.doRebind) {
            requestServiceBindingLocked(s, b.intent, callerFg, true);
        }
    } else if (!b.intent.requested) {
        requestServiceBindingLocked(s, b.intent, callerFg, false);
    }
}
```
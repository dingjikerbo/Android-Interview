


咱们要搞清楚setAsynchronous的作用？看注释，如果是异步消息，就不会服从于sync barriers，不受他的影响。有些操作，比如view的刷新，会插入一个sync barrier到message queue里，去阻止后面的消息被分发，直到满足某些条件为止。


咱们先看enqueueMessage函数，先给message标为inUse，这的when表示message的触发时间，MessageQueue里有个单链表，保存所有message。这个needWake表示是否要唤醒eventQueue。

```
boolean enqueueMessage(Message msg, long when) {
    synchronized (this) {
        msg.markInUse();
        msg.when = when;
        Message p = mMessages;
        boolean needWake;
        if (p == null || when == 0 || when < p.when) {
            // New head, wake up the event queue if blocked.
            msg.next = p;
            mMessages = msg;
            needWake = mBlocked;
        } else {
            needWake = mBlocked && p.target == null && msg.isAsynchronous();
            Message prev;
            for (;;) {
                prev = p;
                p = p.next;
                if (p == null || when < p.when) {
                    break;
                }
                if (needWake && p.isAsynchronous()) {
                    needWake = false;
                }
            }
            msg.next = p; // invariant: p == prev.next
            prev.next = msg;
        }

        // We can assume mPtr != 0 because mQuitting is false.
        if (needWake) {
            nativeWake(mPtr);
        }
    }
    return true;
}
```


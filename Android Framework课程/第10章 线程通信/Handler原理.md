


咱们要搞清楚setAsynchronous的作用？看注释，如果是异步消息，就不会服从于sync barriers，不受他的影响。有些操作，比如view的刷新，会插入一个sync barrier到message queue里，去阻止后面的消息被分发，直到满足某些条件为止。

```
/**
 * Sets whether the message is asynchronous, meaning that it is not
 * subject to {@link Looper} synchronization barriers.
 * <p>
 * Certain operations, such as view invalidation, may introduce synchronization
 * barriers into the {@link Looper}'s message queue to prevent subsequent messages
 * from being delivered until some condition is met.  In the case of view invalidation,
 * messages which are posted after a call to {@link android.view.View#invalidate}
 * are suspended by means of a synchronization barrier until the next frame is
 * ready to be drawn.  The synchronization barrier ensures that the invalidation
 * request is completely handled before resuming.
 * </p><p>
 * Asynchronous messages are exempt from synchronization barriers.  They typically
 * represent interrupts, input events, and other signals that must be handled independently
 * even while other work has been suspended.
 * </p><p>
 * Note that asynchronous messages may be delivered out of order with respect to
 * synchronous messages although they are always delivered in order among themselves.
 * If the relative order of these messages matters then they probably should not be
 * asynchronous in the first place.  Use with caution.
 * </p>
 *
 * @param async True if the message is asynchronous.
 *
 * @see #isAsynchronous()
 */
```

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


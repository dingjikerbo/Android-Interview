

```
Looper.myQueue().addIdleHandler(new MessageQueue.IdleHandler() {
    @Override
    public boolean queueIdle() {
        Log.v("bush", String.format("queueIdle now"));
        return true;
    }
});
```

MessageQueue有一个IdleHandler列表，
```
public void addIdleHandler(@NonNull IdleHandler handler) {
    synchronized (this) {
        mIdleHandlers.add(handler);
    }
}
```

这个IdleHandler是在哪里用的，
```
Message next() {
    final long ptr = mPtr;
    if (ptr == 0) {
        return null;
    }

    int pendingIdleHandlerCount = -1;
    int nextPollTimeoutMillis = 0;
    for (;;) {
        nativePollOnce(ptr, nextPollTimeoutMillis);

        synchronized (this) {
            final long now = SystemClock.uptimeMillis();
            Message msg = mMessages;
            
            if (msg != null) {
                if (now < msg.when) {
                    nextPollTimeoutMillis = (int) Math.min(msg.when - now, Integer.MAX_VALUE);
                } else {
                    mBlocked = false;
                    mMessages = msg.next;
                    msg.next = null;
                    return msg;
                }
            } else {
                nextPollTimeoutMillis = -1;
            }

            // 走到这，说明当前没有可以处理的消息
            // 接下来在让线程休眠之前，处理一下idleHandlers

            if (pendingIdleHandlerCount < 0
                    && (mMessages == null || now < mMessages.when)) {
                pendingIdleHandlerCount = mIdleHandlers.size();
            }

            // 没有idleHandler可处理，那进入休眠吧
            if (pendingIdleHandlerCount <= 0) {
                mBlocked = true;
                continue;
            }

            // 这里有多少个不重要
            if (mPendingIdleHandlers == null) {
                mPendingIdleHandlers = new IdleHandler[Math.max(pendingIdleHandlerCount, 4)];
            }
            mPendingIdleHandlers = mIdleHandlers.toArray(mPendingIdleHandlers);
        }

        // 处理idleHandlers
        for (int i = 0; i < pendingIdleHandlerCount; i++) {
            final IdleHandler idler = mPendingIdleHandlers[i];
            mPendingIdleHandlers[i] = null; 

            boolean keep = false;
            keep = idler.queueIdle();

            if (!keep) {
                synchronized (this) {
                    mIdleHandlers.remove(idler);
                }
            }
        }

        // 处理完了，所以清零，避免重复执行idleHandler
        pendingIdleHandlerCount = 0;

        // 走到这里说明处理过idleHandler，那么在idleHandler里可能会sendMessage，
        // 所以这里要给这个值清零，在下一轮检查有不有消息
        // 如果还是没有消息，不用再重复执行idleHandler，直接休眠
        // 如果有消息，则直接返回消息
        nextPollTimeoutMillis = 0;
    }
}
```
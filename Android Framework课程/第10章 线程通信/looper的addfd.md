

```
int Looper::addFd(int fd, int ident, int events, Looper_callbackFunc callback, void* data) {
    return addFd(fd, ident, events, callback ? new SimpleLooperCallback(callback) : NULL, data);
}

int Looper::addFd(int fd, int ident, int events, const sp<LooperCallback>& callback, void* data) {
    Request request;
    request.fd = fd;
    request.ident = ident;
    request.events = events;
    request.seq = mNextRequestSeq++;
    request.callback = callback;
    request.data = data;
    if (mNextRequestSeq == -1) mNextRequestSeq = 0; // reserve sequence number -1

    struct epoll_event eventItem;
    request.initEventItem(&eventItem);

    ssize_t requestIndex = mRequests.indexOfKey(fd);
    if (requestIndex < 0) {
        int epollResult = epoll_ctl(mEpollFd, EPOLL_CTL_ADD, fd, & eventItem);
        mRequests.add(fd, request);
    } else {
        int epollResult = epoll_ctl(mEpollFd, EPOLL_CTL_MOD, fd, & eventItem);
        mRequests.replaceValueAt(requestIndex, request);
    }

    return 1;
}
```

这个addFd就是创建了一个Request对象，这个initEventItem就是初始化这个fd要监听的事件，然后加到epoll里，

```
int Looper::pollInner(int timeoutMillis) {
    ......

    struct epoll_event eventItems[EPOLL_MAX_EVENTS];
    int eventCount = epoll_wait(mEpollFd, eventItems, EPOLL_MAX_EVENTS, timeoutMillis);

    ......

    for (int i = 0; i < eventCount; i++) {
        int fd = eventItems[i].data.fd;
        uint32_t epollEvents = eventItems[i].events;
        if (fd == mWakeEventFd) {
            if (epollEvents & EPOLLIN) {
                awoken();
            } else {
                ALOGW("Ignoring unexpected epoll events 0x%x on wake event fd.", epollEvents);
            }
        } else {
            // 是fd触发事件了，pushResponse就是生成一个response，加到mResponses列表里
            // 下面会集中处理
            ssize_t requestIndex = mRequests.indexOfKey(fd);
            if (requestIndex >= 0) {
                int events = 0;
                if (epollEvents & EPOLLIN) events |= EVENT_INPUT;
                if (epollEvents & EPOLLOUT) events |= EVENT_OUTPUT;
                if (epollEvents & EPOLLERR) events |= EVENT_ERROR;
                if (epollEvents & EPOLLHUP) events |= EVENT_HANGUP;
                pushResponse(events, mRequests.valueAt(requestIndex));
            } 
        }
    }
Done: ;

    // Invoke all response callbacks.
    for (size_t i = 0; i < mResponses.size(); i++) {
        Response& response = mResponses.editItemAt(i);
        if (response.request.ident == POLL_CALLBACK) {
            int fd = response.request.fd;
            int events = response.events;
            void* data = response.request.data;

            // 会调到callback的handleEvent函数，如果返回0，就表示删除这个fd。
            int callbackResult = response.request.callback->handleEvent(fd, events, data);
            if (callbackResult == 0) {
                removeFd(fd, response.request.seq);
            }

            response.request.callback.clear();
            result = POLL_CALLBACK;
        }
    }
    return result;
}
```
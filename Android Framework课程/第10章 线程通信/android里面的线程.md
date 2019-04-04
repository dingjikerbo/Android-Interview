

Android Framework里的线程，入口函数都是这个_threadLoop，第一次执行的话，会调到readToRun这个函数，如果返回成功的话，就开始threadLoop了，不是第一次执行的话，就直接threadLoop了。

这个是个无限循环，除非出错了或者主动要求退出。

```
int Thread::_threadLoop(void* user) {
    bool first = true;

    do {
        bool result;
        if (first) {
            first = false;
            self->mStatus = self->readyToRun();
            result = self->threadLoop();
        } else {
            result = self->threadLoop();
        }
    } while(strong != 0);

    return 0;
}
```


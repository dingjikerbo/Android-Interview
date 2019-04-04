1， framework里AMS到应用的调用都用到了oneway。
2， aidl里带oneway和不带oneway编出来的类有何不同？
3,  oneway调用方不会挂起，直接返回，这个跟非oneway最大的区别
4， oneway实体方会阻塞，假如多个进程调用同一个binder实体的不同函数，假如其中一个函数阻塞了，那么其他调用也都阻塞。
5， oneway的实现原理？

# 假如多个进程调用同一个binder实体的不同函数，假如其中一个函数阻塞了，那么其他调用也都阻塞
这个是确定的


咱们在binder.c中搜索async_todo，因为异步transaction都加到这个binder_node的async_todo队列里，看到没，是binder_node的队列，所有关于这个binder_node
的异步调用，都会加到这个async_todo队列，

看参数，proc是目标进程，thread是目标进程中的目标线程，可能为Null。这个函数给一个transaction添加到目标进程的某个队列中，他会尝试在目标进程找到一个线程，唤醒它去
处理这个transaction，如果没找到线程，这个work就添加到进程的waitQueue。如果这里thread非空，transaction就会加到这个线程的waitlist里。

这个has_async_transaction表示当前binder_node有一个transaction正在处理中呢。
如果是oneway，那么这里的thread就是个null，

如果当前有异步任务没完成呢，则pending_async是true，thread是null，这个transaction就会加到binder_node的async_toto队列，

一般情况，Binder驱动程序都是将一个事务保存在一个线程的todo队列中，表示由该线程来处理该事务。每一个事务都关联这一个Binder实体对象， 表示该事务的目标处理对象，即要求与该Binder实体对象对应的Service组件在指定的线程中处理该事务。然而，当Binder驱动程序发现一个事务
是异步事务时，就会将它保存在目标Binder实体的一个异步事务队列中，这个异步事务队列就是由该目标Binder实体对象的async_todo来描述的。
异步事务的定义是那些单向的进程间通信请求，即不需要等待应答的进程间通信请求。由于不需要等待应答，所以Binder驱动程序认为异步事务的优先级
低于同步事务，具体表现为：同一时刻，一个Binder实体对象的所有异步事务至多只有一个会得到处理，其余的异步事务等待在异步事务队列中。

```
static bool binder_proc_transaction(struct binder_transaction *t,
                    struct binder_proc *proc,
                    struct binder_thread *thread)
{
    struct binder_node *node = t->buffer->target_node;
    struct binder_priority node_prio;
    bool oneway = !!(t->flags & TF_ONE_WAY);
    bool pending_async = false;

    if (oneway) {
        if (node->has_async_transaction) {
            pending_async = true;
        } else {
            node->has_async_transaction = true;
        }
    }
    
    if (!thread && !pending_async)
        thread = binder_select_thread_ilocked(proc);

    if (thread) {
        binder_transaction_priority(thread->task, t, node_prio,
                        node->inherit_rt);
        binder_enqueue_thread_work_ilocked(thread, &t->work);
    } else if (!pending_async) {
        binder_enqueue_work_ilocked(&t->work, &proc->todo);
    } else {
        binder_enqueue_work_ilocked(&t->work, &node->async_todo);
    }
    if (!pending_async)
        binder_wakeup_thread_ilocked(proc, thread, !oneway /* sync */);

    return true;
}

```

看看这个async_Todo什么时候取出来的，在这个BC_FREE_BUFFER, 首先从async_todo中取出第一个binder_work，然后加到thread的todo队列，

```
static int binder_thread_write(struct binder_proc *proc,
            struct binder_thread *thread,
            binder_uintptr_t binder_buffer, size_t size,
            binder_size_t *consumed)
{
    uint32_t cmd;

    while (ptr < end && thread->return_error.cmd == BR_OK) {
 
        switch (cmd) {
            ......
            case BC_FREE_BUFFER: {
                binder_uintptr_t data_ptr;
                struct binder_buffer *buffer;

                buffer = binder_buffer_lookup(proc, data_ptr);
                
                if (buffer->async_transaction && buffer->target_node) {
                  
                    if (list_empty(&buffer->target_node->async_todo))
                        buffer->target_node->has_async_transaction = 0;
                    else
                        list_move_tail(buffer->target_node->async_todo.next, &thread->todo);
                }
                
                binder_transaction_buffer_release(proc, buffer, NULL);
                binder_free_buf(proc, buffer);
                break;
            }
        }
    }
    return 0;
}
```

这个binder_wakeup_proc_ilocked就会选择一个线程去执行。

```
static void binder_wakeup_proc_ilocked(struct binder_proc *proc)
{
    struct binder_thread *thread = binder_select_thread_ilocked(proc);
    binder_wakeup_thread_ilocked(proc, thread, /* sync = */false);
}

```

这个BC_FREE_BUFFER什么时候调用呢？client端收到BR_REPLY的时候，或者server端收到BR_TRANSACTION的时候，

```
void IPCThreadState::freeBuffer(Parcel* parcel, const uint8_t* data,
                                size_t /*dataSize*/,
                                const binder_size_t* /*objects*/,
                                size_t /*objectsSize*/, void* /*cookie*/) {
    IPCThreadState* state = self();
    state->mOut.writeInt32(BC_FREE_BUFFER);
    state->mOut.writePointer((uintptr_t)data);
}
```



# 
这是binder_Write_read，
```
static long binder_ioctl(struct file *filp, unsigned int cmd, unsigned long arg)
{
    int ret;
    struct binder_proc *proc = filp->private_data;
    struct binder_thread *thread;
    unsigned int size = _IOC_SIZE(cmd);
    void __user *ubuf = (void __user *)arg;
    ret = wait_event_interruptible(binder_user_error_wait, binder_stop_on_user_error < 2);
    thread = binder_get_thread(proc);
    switch (cmd) {
    case BINDER_WRITE_READ: {
        struct binder_write_read bwr;
        copy_from_user(&bwr, ubuf, sizeof(bwr));

        binder_thread_write(proc, thread, bwr.write_buffer, bwr.write_size, &bwr.write_consumed);

        copy_to_user(ubuf, &bwr, sizeof(bwr));
        break;
    }
    return ret;
}
```

然后看binder_thread_Write，

```
static int binder_thread_write(struct binder_proc *proc,
            struct binder_thread *thread,
            binder_uintptr_t binder_buffer, size_t size,
            binder_size_t *consumed)
{
    uint32_t cmd;
    void __user *buffer = (void __user *)(uintptr_t)binder_buffer;

    while (ptr < end && thread->return_error == BR_OK) {
        ptr += sizeof(uint32_t);

        switch (cmd) {
        ......
        case BC_FREE_BUFFER: {
            binder_uintptr_t data_ptr;
            struct binder_buffer *buffer;
            ptr += sizeof(binder_uintptr_t);
            buffer = binder_buffer_lookup(proc, data_ptr);
            if (buffer->transaction) {
                buffer->transaction->buffer = NULL;
                buffer->transaction = NULL;
            }
            if (buffer->async_transaction && buffer->target_node) {
                if (list_empty(&buffer->target_node->async_todo))
                    buffer->target_node->has_async_transaction = 0;
                else
                    list_move_tail(buffer->target_node->async_todo.next, &thread->todo);
            }
            binder_transaction_buffer_release(proc, buffer, NULL);
            binder_free_buf(proc, buffer);
            break;
        }
        case BC_TRANSACTION:
        case BC_REPLY: {
            struct binder_transaction_data tr;
            copy_from_user(&tr, ptr, sizeof(tr));
            ptr += sizeof(tr);
            binder_transaction(proc, thread, &tr, cmd == BC_REPLY);
            break;
        }
        *consumed = ptr - buffer;
    }
    return 0;
}
```

继续调到了binder_Transaction，目前是BC_TRANSACTION，所以reply是0，

```
static void binder_transaction(struct binder_proc *proc,
                   struct binder_thread *thread,
                   struct binder_transaction_data *tr, int reply)
{
    struct binder_transaction *t;

    struct binder_ref *ref;
    ref = binder_get_ref(proc, tr->target.handle);
    target_node = ref->node;
    target_proc = target_node->proc;
    
    target_list = &target_proc->todo;
    target_wait = &target_proc->wait;

    t = kzalloc(sizeof(*t), GFP_KERNEL);
    tcomplete = kzalloc(sizeof(*tcomplete), GFP_KERNEL);

    // 如果当前不是BC_REPLY并且不是异步
    if (!reply && !(tr->flags & TF_ONE_WAY))
        t->from = thread;
    else
        t->from = NULL; // 如果是bc_Reply或者是异步，就不用关心from了

    t->to_proc = target_proc;
    t->to_thread = target_thread;
    t->code = tr->code;
    t->flags = tr->flags;
   
    t->buffer = binder_alloc_buf(target_proc, tr->data_size,
        tr->offsets_size, !reply && (t->flags & TF_ONE_WAY));
    t->buffer->transaction = t;
    t->buffer->target_node = target_node;
   
    offp = (binder_size_t *)(t->buffer->data +
                 ALIGN(tr->data_size, sizeof(void *)));
    copy_from_user(t->buffer->data, (const void __user *)(uintptr_t)
               tr->data.ptr.buffer, tr->data_size));

    copy_from_user(offp, (const void __user *)(uintptr_t)
               tr->data.ptr.offsets, tr->offsets_size));

    if (reply) {
        binder_pop_transaction(target_thread, in_reply_to);
    } else if (!(t->flags & TF_ONE_WAY)) {
        t->need_reply = 1;
        t->from_parent = thread->transaction_stack;
        thread->transaction_stack = t;
    } else {
        // 不是BC_REPLY并且是异步
        if (target_node->has_async_transaction) {
            // 如果目标binder_node当前有异步任务正在处理，就加到目标binder_node的async_todo里
            target_list = &target_node->async_todo;
            target_wait = NULL;
        } else {
            // 当前没有异步任务正在处理，就加到目标进程的todo里
            target_node->has_async_transaction = 1;
        }
    }
    t->work.type = BINDER_WORK_TRANSACTION;
    list_add_tail(&t->work.entry, target_list);
    tcomplete->type = BINDER_WORK_TRANSACTION_COMPLETE;
    list_add_tail(&tcomplete->entry, &thread->todo);
    if (target_wait)
        wake_up_interruptible(target_wait);
}
```
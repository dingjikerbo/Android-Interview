# Java线程

**Sleep/Wait区别**
 - 这两个方法来自不同的类分别是Thread和Object  
 - Sleep是静态方法，只对当前线程有效
 - 最主要是sleep方法没有释放锁，而wait方法释放了锁
 - wait，notify和notifyAll只能在synchronized同步块里面使用，而sleep没有这个限制
 - 如果线程阻塞在wait/sleep/join中收到中断，则会抛出InterruptedException异常

 当正在wait的线程被唤醒，要先获取锁，如果未获取到，则重新进入阻塞。

**Java线程中断机制**
Java的中断模型是其它对象设置了Java线程的中断标志位，该线程会在何时的时机检查该标志位，是否处理就不一定了。

不可中断的操作，包括进入synchronized段以及Lock.lock()，inputSteam.read()等，调用interrupt()对于这几个问题无效，因为它们都不抛出中断异常。如果拿不到资源，它们会无限期阻塞下去。对于阻塞的IO，直接关掉句柄通常会抛出异常。

对于inputStream等资源，有些(实现了interruptibleChannel接口)可以通过close()方法将资源关闭，对应的阻塞也会被放开。
对于Lock.lock()，可以改用Lock.lockInterruptibly()，可被中断的加锁操作，它可以抛出中断异常。等同于等待时间无限长的Lock.tryLock(long time, TimeUnit unit)。

**Java线程状态**
 - 新建
 - 就绪，即可运行状态，当调用thread.start()后进入就绪态，随时可能被CPU调度执行
 - 运行，线程只能从就绪态进入运行态
 - 阻塞，让出cpu。分三种，1）等待阻塞，wait 2）同步阻塞，synchronize 3) 其它阻塞，sleep/join/其它io请求，阻塞结束后进入就绪态。
 - 死亡


**yield/join区别**
yield表示愿意放弃cpu，进入就绪态。但是这个请求也许会被忽略。这个函数很少用到。
 - Yield是一个静态的原生(native)方法
 - Yield告诉当前正在执行的线程把运行机会交给线程池中拥有相同优先级的线程。
 - 它仅能使一个线程从运行状态转到就绪态，而不是等待或阻塞状态
 - Yield不能保证使得当前正在运行的线程迅速转换到就绪态
 - yield不会释放锁。

join是等待别的线程执行完毕。

**synchronized**
- 锁某个对象实例的：synchronized method(){}锁的是实例对象，即一个对象里如果有若干个类似的函数，只要一个线程访问了其中一个synchronized方法，其它线程不能同时访问这个对象中任何一个synchronized方法，不过非synchronized方法是可以访问的。不同实例对象的synchronized方法是不受影响的，也就是说，其它线程照样可以同时访问相同类的另一个对象实例中的synchronized方法。同样锁住对象实例的还有synchronize (this){}
- 锁某个类对象：synchronized static aStaticMethod{}防止多个线程同时访问这个类中的synchronized static 方法。它可以对类的所有对象实例起作用。同样锁住类对象的还有synchronized (Func.class) {}，但是注意这不影响类似于synchronized method(){}的函数，它们获取的是对象实例的锁。
- 锁任意实例对象：如synchronized (lock) {}

synchronized关键字是不能继承的，也就是说，基类的方法synchronized f(){} 在继承类中并不自动是synchronized f(){}，而是变成了f(){}。继承类需要你显式的指定它的某个方法为synchronized方法；

```
private class Func {
    synchronized void test1() {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("test1 over");
    }

    synchronized void test2() {
        System.out.println("hello test2");
    }
}

private class GoFunc extends Func {
    
}
```

创建两个GoFunc对象，在线程A执行test1，在线程B执行test2，结果是先打印hello test2，然后再打印hello test1。可见synchronized在GoFunc中没生效。

synchronized等待锁的时候是不响应中断的。




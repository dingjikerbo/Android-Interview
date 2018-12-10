# Java高级

Java内存模型分主内存和工作内存，所有的变量都存储在主内存中，工作内存是线程私有的，类似于高速缓存。线程的工作内存中保存了被该线程使用到的变量的主内存副本拷贝。线程对变量的所有操作（读取、赋值等）都必须在工作内存中进行，而不能直接读写主内存中的变量。不同的线程之间也无法直接访问对方工作内存中的变量，线程间变量值的传递均需要通过主内存来完成。

**volatile**
当一个变量定义为volatile之后，就具备两种特性：第一是保证此变量对所有线程的可见性，这里的可见性是指当一个线程修改了这个变量的值，新的值对于其它线程来说是可以立即得知的。更具体的说，是新值能立即同步到主内存，以及每次使用前立即从主内存刷新。而普通变量不能做到这一点，普通变量的值在线程中传递均需要通过主内存来完成。例如，线程A修改了一个普通变量的值，然后向主内存回写，线程B在线程A回写完成之后再从主内存读取，新变量值才会对线程B可见。volatile变量在各个线程的工作内存中不存在一致性问题，但是Java里面的运算并非原子操作，导致volatile变量的运算在并发下一样是不安全的。

volatile的第二个语义是禁止指令重排优化。举个例子，有个变量init表示是否已初始化。在A线程中，先做一些初始化配置，然后将init设为true。在B线程中，在while循环中等待init是否为true，如果是true，则去读取线程A中初始化好的配置信息。如果init没有指定volatile，则由于指令重排，A中init=true可能提到初始化配置前面执行，这样B读取配置时出错。volatile变量的赋值操作会插入一道内存屏障，指令重排无法越过内存屏障。

**GC Roots**
虚拟机栈中引用的对象
方法区中类的静态属性引用的对象
方法区中常量引用的对象
本地方法栈中JNI引用的对象

**强引用，软应用，弱引用，虚引用**
无法通过虚引用获取对象的实例，为一个对象设置虚引用的唯一目的就是能在这个对象被回收时收到一个系统通知。

**垃圾收集算法**
### 标记清除

### 标记复制
将内存划分成两块，每当一块用完，就将这块还活着的对象复制到另一块上，然后给这块空间清理一遍。这个问题是内存缩小成一半了。现在的商用虚拟机都是用这种方法收集新生代。假设每次GC时新生代对象只存活10%，那么给整个空间分成一个大的Eden，两个小的Survivor，比例为8:1:1。回收时，给Eden和其中一个Survivor的活着的对象复制到另一个Survivor，然后清理掉Eden和刚用的那个Survivor。不过我们没办法保证每次回收都只有不超过10%的对象存活，当Survivor不够用时，需要依赖其它内存(老年代)进行分配担保，即直接让那些对象进入老年代。

### 标记整理
老年代

当前的商业虚拟机的垃圾收集都是采用分代收集，新生代用标记复制，老年代用标记清除或标记整理。

最原始的Serial收集器在GC时会在虚拟机层面停掉所有的线程。后来的Parallel收集器，到CMS(Concurrent Mark Sweep)乃至Garbage First（G1)，用户停顿时间不断缩短，但仍无法完全消除。

**CMS原理**
基于标记清除的，分为4个步骤：初始标记，并发标记，重新标记，并发清除。

1， 初始标记仅仅是标记一下GC Roots能直接关联到的对象，注意这里是直接关联到的，所以很快。初始标记要stop the world的，单线程执行
2， 并发标记是进行GC Roots Tracing过程，不用stop the world，单线程执行
3， 重新标记是为了修正并发标记期间因用户程序继续运行导致标记产生变动的那一部分对象的标记记录，这个阶段的停顿时间一般比初始标记阶段稍长一些，但远比并发标记的时间短。
要stop the world，但是是多线程执行的，所以速度很快。这个阶段是修正原本的垃圾对象因新增引用而不用回收的情况。本阶段不处理新增的垃圾对象，而是留到下一次GC再处理。
4，并发清除，不用stop the world，单线程执行。

整个过程中耗时最长的并发标记和并发清除都可以和用户线程一起工作，所以总体来说，CMS 的GC是与用户线程并发执行的。

虽然不会stop the world，但是分掉了部分cpu资源，所以用户线程执行速度还是会受影响，尤其是系统负载较重时。

另外CMS由于是基于MarkSweep，所以容易有碎片。

新生对象一般在Eden区，大对象一般直接进入老年代，要避免大对象频繁分配释放，否则触发Full GC非常慢。长期存活的对象进入老年代。每次minor gc还存活的对象age就递增，达到某个阈值就进入老年代。

每次minor gc时如果突然存活对象增多，导致新生代无法容纳时，就会由老年代提供担保，如果老年代剩余空间不够担保的话，可能会触发一次full gc。

**DirectByteBuffer**
直接内存(Direct Memory)，可以使用native函数直接分配堆外内存，然后通过一个存储在java堆中的DirectByteBuffer对象作为这块内存的引用进行操作，这样能在一些场合显著提高性能，因为避免了在java堆和native堆中来回复制数据。这个直接内存是不受java堆大小限制的。
http://www.importnew.com/26334.html

**类加载机制**
虚拟机把描述类的数据从class文件加载到内存，并对数据进行校验，转换解析和初始化，最终形成可以被虚拟机直接使用的java类型，这就是虚拟机的类加载机制。

Java中类的加载，连接和初始化过程都是在程序运行期间完成的。

类的加载，包括从zip包，或网络数据流，或运行时动态生成，如动态代理。

虚拟机会保证一个类的构造函数在多线程环境下被正确的加锁、同步。

双亲委托

看Java的ClassLoader实现，首先判断该class是否已经加载，如果没有则交给parent先loadClass，如果没有parent就交给BootstrapClassLoader，如果这样都没找到，就调用本ClassLoader的findClass自己来找了，这个函数默认会直接抛出异常的，我们最好自定义ClassLoader，override这个findClass，实现类查找的逻辑。

这里注意一下，交给parent来loadClass，然后依次递归往上走到顶，如果都没有找到，则再依次往下调用各层ClassLoader的findClass，如果都没find到，则调用我们自定义ClassLoader的findClass，如果还找不到就抛异常。

```
protected Class<?> loadClass(String name, boolean resolve)
{
        synchronized (getClassLoadingLock(name)) {
            Class<?> c = findLoadedClass(name);
            if (c == null) {
                if (parent != null) {
                    c = parent.loadClass(name, false);
                } else {
                    c = findBootstrapClassOrNull(name);
                }
            }
            if (c == null) {
                c = findClass(name);
            }
        }
        if (resolve) {
            resolveClass(c);
        }
        return c;
    }
}
```

双亲委托的好处：
1，避免重复加载
2，更加安全，系统类不会被替换

另外，只有两个类的类名一致且被同一个加载器加载的类，Java虚拟机才会认为他们是同一个类。

**对象的生命周期**



# 对象是怎么创建的
虚拟机遇到一个new指令时，首先去常量池中找这个类的符号引用，检查这个类是否被加载、解析和初始化过，如果没有，就要先执行相应的类加载过程。类加载检查通过后，就要为新生对象分配内存，分配方式可能采用指针碰撞（基于标记整理的），或者空闲链表(基于标记清除的)。

为保证虚拟机中创建对象是线程安全的，有两种方案，一种是采用CAS保证更新操作的原子性，另一种是每个线程在java堆中预先分配一小块内存，称为TLAB(thread local allocation buffer)，这是线程私有的，分配内存优先在该buffer上进行，只有不够用需要分配新的TLAB时才需要同步锁定。

然后在执行对象的<init>方法，

**对象的销毁**
即便在可达性分析中不可达的对象，也不是非死不可，这时他们处于死缓状态。要真正宣告一个对象死亡，要经过两次标记。如果对象在进行可达性分析后发现没有与GC Roots相连的引用链，就会被第一次标记，如果对象覆盖了finalize()方法或者finalize()还没被调过，这个对象就会被放到一个F-Queue队列中，并在稍后由一个虚拟机自动建立的、低优先级的Finalizer线程去执行他它。这里的执行是指会触发finalize方法，但不承诺会等待它运行结束。因为如果它一直不结束，其它对象就处于永久等待了，导致真个内存回收系统崩溃。finalize是对象逃脱死亡命运的最后一次机会，稍后GC会对F-Queue中的对象进行第二次小规模的标记，如果对象要在finalized中成功拯救自己，只要重新与引用链上任何一个对象建立关联即可，那在第二次标记时就会被移出即将回收的集合，否则就真的被回收了。要注意，任何一个对象的finalize只会被系统调用一次。

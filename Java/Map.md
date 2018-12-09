# HashTable
底层数组+链表，线程安全，实现方式是锁住整个table，效率低。

# HashMap
底层数组+链表，JAVA8将链表换成红黑树，非线程安全。

# ConcurrentHashMap
底层采用数组+链表实现，JAVA8同样将链表换成红黑树，采用锁分段技术，允许多个修改并行进行，只有部分方法要锁整张表，如size()。

**锁分段技术**：首先将数据分成一段一段的存储，然后给每一段数据配一把锁，当一个线程占用锁访问其中一个段数据的时候，其他段的数据也能被其他线程访问，即降低锁的粒度。

# LinkedHashMap
通过维护一个运行于所有条目的双向链表，LinkedHashMap保证了元素迭代的顺序。该迭代顺序可以是插入顺序或者是访问顺序。是非线程安全的。LRUCache可用LinkedHashMap实现。

# TreeMap
TreeMap基于红黑树实现，根据key进行排序，可以指定comparator。非线程安全。
**红黑树**：是一颗平衡二叉树，每次写操作都要重新调整树结构使其保持平衡，添加、查找、删除都是O(lgn)。

# WeakHashMap
和HashMap一样也是个散列表，它的特殊之处在于WeakHashMap里的entry可能会被GC自动删除，即使程序员没有调用remove()或者clear()方法。这个特点特别适用于需要缓存的场景。在缓存场景下，由于内存是有限的，不能缓存所有对象；对象缓存命中可以提高系统效率，但缓存MISS也不会造成错误，因为可以通过计算重新得到。WeakHashMap内部是通过弱引用来管理entry的，弱引用的特性对应到 WeakHashMap 上意味着什么呢？将一对key, value放入到WeakHashMap里并不能避免该key值被GC回收，除非在 WeakHashMap之外还有对该key的强引用。


咱们以ThreadLocal的get函数为入口，首先获取当前Thread对象，

```
public T get() {
    Thread currentThread = Thread.currentThread();
    Values values = values(currentThread);
    if (values != null) {
        Object[] table = values.table;
        int index = hash & values.mask;
        if (this.reference == table[index]) {
            return (T) table[index + 1];
        }
    } else {
        values = initializeValues(currentThread);
    }

    return (T) values.getAfterMiss(this);
}
```

这个values就是当前Thread对象里的localValues，
```
 Values values(Thread current) {
    return current.localValues;
}
```

咱们假设这个values是null，会调initializeValues，

```
Values initializeValues(Thread current) {
    return current.localValues = new Values();
}
```

看看Values的构造函数，这个table是个Object数组，

```
Values() {
    initializeTable(INITIAL_SIZE);
    this.size = 0;
    this.tombstones = 0;
}

private void initializeTable(int capacity) {
    this.table = new Object[capacity * 2];
    this.mask = table.length - 1;
    this.clean = 0;
    this.maximumLoad = capacity * 2 / 3; // 2/3
}
```

咱们看getAfterMiss函数，

```
Object getAfterMiss(ThreadLocal<?> key) {
    Object[] table = this.table;
    int index = key.hash & mask;

    // If the first slot is empty, the search is over.
    if (table[index] == null) {
        Object value = key.initialValue();

        // If the table is still the same and the slot is still empty...
        if (this.table == table && table[index] == null) {
            table[index] = key.reference;
            table[index + 1] = value;
            size++;

            cleanUp();
            return value;
        }

        // The table changed during initialValue().
        put(key, value);
        return value;
    }

    // Keep track of first tombstone. That's where we want to go back
    // and add an entry if necessary.
    int firstTombstone = -1;

    // Continue search.
    for (index = next(index);; index = next(index)) {
        Object reference = table[index];
        if (reference == key.reference) {
            return table[index + 1];
        }

        // If no entry was found...
        if (reference == null) {
            Object value = key.initialValue();

            // If the table is still the same...
            if (this.table == table) {
                // If we passed a tombstone and that slot still
                // contains a tombstone...
                if (firstTombstone > -1
                        && table[firstTombstone] == TOMBSTONE) {
                    table[firstTombstone] = key.reference;
                    table[firstTombstone + 1] = value;
                    tombstones--;
                    size++;

                    // No need to clean up here. We aren't filling
                    // in a null slot.
                    return value;
                }

                // If this slot is still empty...
                if (table[index] == null) {
                    table[index] = key.reference;
                    table[index + 1] = value;
                    size++;

                    cleanUp();
                    return value;
                }
            }

            // The table changed during initialValue().
            put(key, value);
            return value;
        }

        if (firstTombstone == -1 && reference == TOMBSTONE) {
            // Keep track of this tombstone so we can overwrite it.
            firstTombstone = index;
        }
    }
}
```


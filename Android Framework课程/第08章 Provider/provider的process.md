
# 实验一
不设置android:process，不设置android:multiprocess

```
<provider
    android:authorities="inuker.com.test5"
    android:name=".MyProvider"
    android:exported="true"/>
```

日志如下：
```
MyProvider onCreate
MyApplication onCreate
```

# 实验二
设置了android:process，但是没设置android:multiprocess，默认为false。
```
<provider
    android:authorities="inuker.com.test5"
    android:name=".MyProvider"
    android:process=":hello"
    android:exported="true"/>
```

日志如下：
可见provider没有启动，那什么时候启动呢？可能是别人调他的时候，我们来试试，
```
MyApplication onCreate
```

当另一个app进程调用了insert函数，会打印如下日志，可见，provider在被用到的时候启动进程，执行onCreate，执行insert，最后才Application的onCreate。

```
inuker.com.test5:hello V: MyProvider onCreate
inuker.com.test5:hello V: Provider insert called
inuker.com.test5:hello V: uri = content://inuker.com.test5/person
inuker.com.test5:hello V: MyApplication onCreate
```

# 实验三
设置android:process，并且设置android:multiprocess为true，

```
<provider
    android:authorities="inuker.com.test5"
    android:name=".MyProvider"
    android:process=":hello"
    android:multiprocess="true"
    android:exported="true"/>
```

打印日志如下：
可见provider进程没启动，
```
inuker.com.test5 V: MyApplication onCreate
```

咱们用另一个app进程B调用insert函数，打印日志如下
可见跑在inuker.com.test5:hello进程下

```
inuker.com.test5:hello V: MyProvider onCreate
inuker.com.test5:hello V: Provider insert called
inuker.com.test5:hello V: uri = content://inuker.com.test5/person
inuker.com.test5:hello V: MyApplication onCreate
```

如果这两个app进程都设置了相同的android:sharedUserId="hello.world"，那么会发现provider会启动在调用者B进程里。

# 实验四
不设置android:process，只设置android:multiprocess为true，不设置两个app的android:sharedUserId相同。那么provider会跟随应用A一起启动。

日志可见provider先启动，进程和应用A的主进程相同。
```
22605-22605/? V: MyProvider onCreate
22605-22605/? V: MyApplication onCreate, process = inuker.com.test5
```

如果应用B调用insert，可见会调到A进程里，

```
23277-23277/inuker.com.test2 V: MyApplication onCreate, process = inuker.com.test2
22605-22605/inuker.com.test5 V: MyApplication onTrimMemory, level = 20
22605-23296/inuker.com.test5 V: Provider insert called
22605-23296/inuker.com.test5 V: uri = content://inuker.com.test5/person
```

如果两个app都设置了相同的shareUserID，那么provider就会创建两个实例，一个跟随应用A进程启动，一个在应用B insert的时候启动在B进程。

# 总结
一，shareUserId不同
1、不设置android:multiprocess，不设置android:process，provider跟随主应用进程启动而启动。
2、不设置android:multiprocess，设置android:process=":hello"，provider不跟随主应用进程启动而启动，而是被别人调到的时候才在hello进程启动。
3、设置android:multiprocess，不设置android:process，provider跟随主应用进程启动而启动，别人调的时候也不会启动新的实例
4、设置android:multiprocess，设置android:process，provider不会跟随主应用进程启动而启动，别人调的时候在hello进程启动，insert也跑在hello进程，不会启动新的实例

二，shareUserId相同
1、不设置android:multiprocess，不设置android:process，provider跟随主应用进程启动而启动。
2、不设置android:multiprocess，设置android:process=":hello"，provider不跟随主应用进程启动而启动，而是被别人调到的时候才在hello进程启动。
3、设置android:multiprocess，不设置android:process，provider跟随主应用进程启动而启动，别人调的时候会启动新的实例
4、设置android:multiprocess，设置android:process，provider不会跟随主应用进程启动而启动，别人调的时候在hello进程启动，insert也跑在hello进程，会启动新的实例
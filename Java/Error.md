Exception 和 Error 都是继承了Throwable类，在Java中只有Throwable 类型的实例才可以被抛出（throw）或者捕获（catch），它是异常处理机制的基本组成类型。 
Exception是程序正常运行中，可以预料的意外情况，可能并且应该被捕获，进行相应处理。 

# Error
Error是指在正常情况下，不大可能出现的情况，绝大部分的Error都会导致程序（比如 JVM 自身）处于非正常的、不可恢复状态。既然是非正常情况，所以不便于也不需要捕获，常见的比如 OutOfMemoryError之类，都是Error的子类。

常见的ERROR：NoClassDefFoundError、VirtualMachineError、OutOfMemoryError、StackOverflowError 

# Exception
Exception又分为可检查（checked）异常和不检查（unchecked）异常，可检查异常在源代码里必须显式地进行捕获处理，这是编译期检查的一部分。不检查异常就是所谓的运行时异常，类似 NullPointerException、ArrayIndexOutOfBoundsException 之类，通常是可以编码避免的逻辑错误，具体根据需要来判断是否需要捕获，并不会在编译期强制要求。

常见的Exception：IOException（Checked Exception） 
RuntimeException、NullPointerException、ClassCastException、SecurityException/ClassNotFoundException


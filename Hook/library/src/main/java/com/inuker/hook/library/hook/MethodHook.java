package com.inuker.hook.library.hook;

import android.os.Build;

import com.inuker.hook.library.compat.Memory;
import com.inuker.hook.library.compat.MethodCompat;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Created by workstation on 17/4/12.
 */

public class MethodHook {

    public static void hook(Method origin, Method replace) {
        setAccessFlags(origin);
        setAccessFlags(replace);

        Memory.memcpy(MethodCompat.getArtMethodAddress(origin),
                MethodCompat.getArtMethodAddress(replace),
                MethodCompat.getArtMethodSize());
    }

    private static void setAccessFlags(Method method) {
        Class<?> abstractMethodClaz = null;
        try {
            abstractMethodClaz = Method.class.getSuperclass();
            Field accessFlagsField = abstractMethodClaz.getDeclaredField("accessFlags");
            accessFlagsField.setAccessible(true);

            Integer accessFlags = (Integer) accessFlagsField.get(method);
            accessFlags &= ~Modifier.PUBLIC;
            accessFlags |= Modifier.PRIVATE;
            accessFlagsField.set(method, accessFlags);
        } catch (Throwable e) {
            e.printStackTrace();
        }

        method.setAccessible(true);
    }

    private static Method backupFor51AndBelow(Method origin) throws Throwable {
        // java.lang.reflect.ArtMethod
        Class<?> artMethodClass = Class.forName("java.lang.reflect.ArtMethod");
        Field accessFlagsField = artMethodClass.getDeclaredField("accessFlags");
        accessFlagsField.setAccessible(true);
        Constructor<?> artMethodConstructor = artMethodClass.getDeclaredConstructor();
        artMethodConstructor.setAccessible(true);
        Object newArtMethod = artMethodConstructor.newInstance();
        Constructor<Method> methodConstructor = Method.class.getDeclaredConstructor(artMethodClass);
        Method newMethod = methodConstructor.newInstance(newArtMethod);
        newMethod.setAccessible(true);
        Memory.memcpy(MethodCompat.getArtMethodAddress(newMethod), MethodCompat.getArtMethodAddress(origin),
                MethodCompat.getArtMethodSize());

        Integer accessFlags = (Integer) accessFlagsField.get(newArtMethod);
        accessFlags &= ~Modifier.PRIVATE;
        accessFlags |= Modifier.PUBLIC;
        accessFlagsField.set(newArtMethod, accessFlags);

        return newMethod;
    }

    private static Method backup6AndAbove(Method origin) throws Throwable {
        // AbstractMethod
        Class<?> abstractMethodClass = Method.class.getSuperclass();
        Field accessFlagsField = abstractMethodClass.getDeclaredField("accessFlags");
        Field artMethodField = abstractMethodClass.getDeclaredField("artMethod");
        accessFlagsField.setAccessible(true);
        artMethodField.setAccessible(true);

        // make the construct accessible, we can not just use `setAccessible`
        Constructor<Method> methodConstructor = Method.class.getDeclaredConstructor();
        Field override = AccessibleObject.class.getDeclaredField(
                Build.VERSION.SDK_INT == Build.VERSION_CODES.M ? "flag" : "override");
        override.setAccessible(true);
        override.set(methodConstructor, true);

        // clone the origin method
        Method newMethod = methodConstructor.newInstance();
        newMethod.setAccessible(true);
        for (Field field : abstractMethodClass.getDeclaredFields()) {
            field.setAccessible(true);
            field.set(newMethod, field.get(origin));
        }

        // allocate new artMethod struct, we can not use memory managed by JVM
//        ByteBuffer artMethod = ByteBuffer.allocateDirect((int) MethodInspect.getArtMethodSize());
//        Long artMethodAddr;
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
//            // Below Android N, the jdk implementation is not openjdk
//            Object memoryBlock = Reflection.get(MappedByteBuffer.class, null, "block", artMethod);
//            Class<?> memoryBlockClazz = Class.forName("java.nio.MemoryBlock");
//            artMethodAddr = (Long) Reflection.call(memoryBlockClazz, null, "toLong", memoryBlock, null, null);
//        } else {
//            artMethodAddr = (Long) Reflection.call(artMethod.getClass(), null, "address", artMethod, null, null);
//        }
//        Memory.memcpy(artMethodAddr, MethodInspect.getMethodAddress(origin),
//                MethodInspect.getArtMethodSize());

        // replace the artMethod of our new method
//        artMethodField.set(newMethod, artMethodAddr);

        // modify the access flag of the new method
//        Integer accessFlags = (Integer) accessFlagsField.get(origin);
//        int privateAccFlag = accessFlags & ~Modifier.PRIVATE | Modifier.PUBLIC;
//        accessFlagsField.set(newMethod, privateAccFlag);
//
//        final int ACC_FLAG_OFFSET = 4;
//        // 1. try big endian
//        artMethod.order(ByteOrder.BIG_ENDIAN);
//        int nativeAccFlags = artMethod.getInt(ACC_FLAG_OFFSET);
//        if (nativeAccFlags == accessFlags) {
//            // hit!
//            artMethod.putInt(ACC_FLAG_OFFSET, privateAccFlag);
//        } else {
//            // 2. try little endian
//            artMethod.order(ByteOrder.LITTLE_ENDIAN);
//            nativeAccFlags = artMethod.getInt(ACC_FLAG_OFFSET);
//            if (nativeAccFlags == accessFlags) {
//                artMethod.putInt(ACC_FLAG_OFFSET, privateAccFlag);
//            } else {
//                // the offset is error!
//                throw new RuntimeException("native set access flags error!");
//            }
//        }

        return newMethod;
    }

    private static Method backUp(Method origin) {
        try {
            if (Build.VERSION.SDK_INT < 23) {
                // 5.1及以下
                return backupFor51AndBelow(origin);
            } else {
                return backup6AndAbove(origin);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }
}

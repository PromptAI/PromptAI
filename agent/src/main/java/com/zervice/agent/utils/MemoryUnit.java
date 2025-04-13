package com.zervice.agent.utils;


/**
 * 内存大小转换
 * @author chen
 * @date 2022/11/14
 */
public class MemoryUnit {

    /**
     * 获取GB的字节
     * @param size  必须是大于0
     */
    public static long gigaByte(int size) {
        return megaByte(1024 * size);
    }

    /**
     * 获取MB的字节
     * @param size  必须是大于0
     */
    public static long megaByte(int size) {
        return  kiloByte(size * 1024);
    }

    /**
     * 获取kb的字节
     * @param size  必须是大于0
     */
    public static long kiloByte(int size) {
        return 1024 * (long) size;
    }
}

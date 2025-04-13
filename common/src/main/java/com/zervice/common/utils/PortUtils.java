package com.zervice.common.utils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.net.ServerSocket;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class PortUtils {
    private static final int START_PORT_RANGE = 19000;
    private static Cache<Integer, Long> recentlyReturned;
    static {
        recentlyReturned = CacheBuilder.newBuilder()
                .expireAfterAccess(3L, TimeUnit.MINUTES)
                .maximumSize(100L)
                .expireAfterWrite(3L, TimeUnit.MINUTES)
                .build();
    }

    public PortUtils() {
    }

    public static int getAPort() {
        int MAX_TRY = 100;
        Random r = new Random();

        while(MAX_TRY-- > 0) {
            try {
                ServerSocket ss = new ServerSocket(START_PORT_RANGE + r.nextInt(10000));
                int port = ss.getLocalPort();
                ss.close();
                if (recentlyReturned.getIfPresent(port) == null) {
                    recentlyReturned.put(port, System.currentTimeMillis());
                    return port;
                }
            } catch (Exception e) {
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException ie) {
                    break;
                }
            }
        }

        throw new IllegalStateException("No available ports ");
    }
}

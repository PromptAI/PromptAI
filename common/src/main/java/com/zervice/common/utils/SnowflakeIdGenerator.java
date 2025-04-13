package com.zervice.common.utils;

import com.google.common.base.Preconditions;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Date;
import java.util.Random;

/**
 * Using twitter's snowflake's algorithm to generate a 64-bit unique id
 *
 * id is composed of:
 *   time - 41 bits (millisecond precision w/ a custom epoch gives us 69 years)
 *   configured machine id - 10 bits - gives us up to 1024 machines
 *   sequence number - 12 bits - rolls over every 4096 per machine (with protection to avoid rollover in the same ms)
 *
 *
 */
public class SnowflakeIdGenerator {

    //   id format  =>
//   timestamp |datacenter | sequence
//   41        |10         |  12
    private static final long sequenceBits = 12;
    private static final long datacenterIdBits = 10L;
    private static final long maxDatacenterId = -1L ^ (-1L << datacenterIdBits);

    private static final long datacenterIdShift = sequenceBits;
    private static final long timestampLeftShift = sequenceBits + datacenterIdBits;

    private static final long twepoch = 1288834974657L;
    private long datacenterId;
    private static final long sequenceMax = 4096;

    private volatile long lastTimestamp = -1L;
    private volatile long sequence = 0L;


    public SnowflakeIdGenerator() {
        this(_getDatacenterId());
    }

    public SnowflakeIdGenerator(long datacenterId) {
        Preconditions.checkArgument(datacenterId < maxDatacenterId);
        this.datacenterId = datacenterId;
    }


    public synchronized long generateLongId() {
        long timestamp = System.currentTimeMillis();
        if (timestamp < lastTimestamp) {
            throw new IllegalStateException(
                    String.format("Clock moved backwards. Refusing to generate id for %d ms", lastTimestamp-timestamp));
        }

        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) % sequenceMax;
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        }
        else {
            sequence = 0;
        }
        lastTimestamp = timestamp;
        return ((timestamp - twepoch) << timestampLeftShift) |
                (datacenterId << datacenterIdShift) |
                sequence;
    }

    public static void main(String[] args) throws Exception {
        long id = 1273160920792568594L;
        id >>= timestampLeftShift;
        long timestamp = id += twepoch;

        Date date = new Date(timestamp);
        System.out.println("Time is - " + date);
    }

    protected long tilNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }

    private static long _getDatacenterId() {
        try {
            InetAddress ip = InetAddress.getLocalHost();
            NetworkInterface network = NetworkInterface.getByInetAddress(ip);
            long id;
            if (network == null) {
                id = _getRandomDatacenterId();
            } else {
                byte[] mac = network.getHardwareAddress();
                if (mac == null) {
                    id = _getRandomDatacenterId();
                } else {
                    id = ((0x000000FF & (long) mac[mac.length - 1]) | (0x0000FF00 & (((long) mac[mac.length - 2]) << 8))) >> 6;

                }
            }
            return id;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static long _getRandomDatacenterId() {
        return Math.abs(new Random(System.currentTimeMillis()).nextInt() % maxDatacenterId);
    }

    public static long getTimeFromId(long id) {
        return (id >> timestampLeftShift) + twepoch;
    }

}

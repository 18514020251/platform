package com.xcvk.platform.id.generator;

/**
 * 雪花ID生成器
 *
 * <p>基于时间戳、数据中心ID、工作节点ID和序列号生成全局唯一ID。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-18
 */
public class SnowflakeIdGenerator {

    /**
     * 自定义起始时间戳：2024-01-01 00:00:00
     */
    private static final long START_TIMESTAMP = 1704067200000L;

    private static final long SEQUENCE_BITS = 12;
    private static final long WORKER_BITS = 5;
    private static final long DATACENTER_BITS = 5;

    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_BITS);
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_BITS);

    private static final long WORKER_SHIFT = SEQUENCE_BITS;
    private static final long DATACENTER_SHIFT = SEQUENCE_BITS + WORKER_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_BITS + DATACENTER_BITS;

    private final long workerId;
    private final long datacenterId;

    private long sequence = 0L;
    private long lastTimestamp = -1L;

    public SnowflakeIdGenerator(long workerId, long datacenterId) {
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException("workerId out of range");
        }
        if (datacenterId < 0 || datacenterId > MAX_DATACENTER_ID) {
            throw new IllegalArgumentException("datacenterId out of range");
        }

        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }

    /**
     * 生成下一个全局唯一ID
     *
     * <p>当发生同毫秒高并发生成时，会通过序列号保证唯一性；
     * 当序列号耗尽时，会等待进入下一毫秒。</p>
     *
     * @return 全局唯一ID
     */
    public synchronized long nextId() {
        long currentTimestamp = currentTimeMillis();

        if (currentTimestamp < lastTimestamp) {
            throw new IllegalStateException("Clock moved backwards, refusing to generate id");
        }

        if (currentTimestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                currentTimestamp = waitUntilNextMillis(currentTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = currentTimestamp;

        return ((currentTimestamp - START_TIMESTAMP) << TIMESTAMP_SHIFT)
                | (datacenterId << DATACENTER_SHIFT)
                | (workerId << WORKER_SHIFT)
                | sequence;
    }

    private long waitUntilNextMillis(long currentTimestamp) {
        while (currentTimestamp <= lastTimestamp) {
            currentTimestamp = currentTimeMillis();
        }
        return currentTimestamp;
    }

    private long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}
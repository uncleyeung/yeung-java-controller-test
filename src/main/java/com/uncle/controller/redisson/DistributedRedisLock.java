package com.uncle.controller.redisson;

import com.sun.corba.se.impl.orbutil.concurrent.Mutex;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

/**
 * @author 杨戬
 * @className DistributedRedisLock
 * @email yangb@chaosource.com
 * @date 19-8-15 17:36
 */

public class DistributedRedisLock {
   /* private DistributedRedisLock() {
    }

    public static DistributedRedisLock buildDistributedRedisLock() {
        return new DistributedRedisLock();
    }*/

    private RedissonClient redissonClient;

    public RedissonClient getRedissonClient() {
        return redissonClient;
    }

    public void setRedissonClient(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    private static final String LOCK_TITLE = "redisLock_";

    public Mutex acquire(String lockName) {
        Mutex mutex = new Mutex();
        String key = LOCK_TITLE + lockName;
        RLock mylock = redissonClient.getLock(key);
        boolean b = true;
        try {
            //3L==等待时间 600L==过期时间
            b = mylock.tryLock(3L, 600L, TimeUnit.SECONDS);
            System.err.println("======lock====== tryLock_result " + b);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.err.println("======lock======" + Thread.currentThread().getName());
        if (b) {
            return mutex;
        } else {
            return null;
        }
    }

    public void release(String lockName) {
        String key = LOCK_TITLE + lockName;
        RLock mylock = redissonClient.getLock(key);
        mylock.unlock();
        System.err.println("======unlock======" + Thread.currentThread().getName());
    }
}

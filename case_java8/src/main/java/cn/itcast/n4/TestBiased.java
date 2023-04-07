package cn.itcast.n4;

import lombok.extern.slf4j.Slf4j;
import org.openjdk.jol.info.ClassLayout;

import java.io.IOException;
import java.util.Vector;
import java.util.concurrent.locks.LockSupport;

// -XX:-UseCompressedOops -XX:-UseCompressedClassPointers -XX:BiasedLockingStartupDelay=0 -XX:+PrintFlagsFinal
//-XX:-UseBiasedLocking tid=0x000000001f173000  -XX:BiasedLockingStartupDelay=0 -XX:+TraceBiasedLocking
@Slf4j(topic = "c.TestBiased")
public class TestBiased {

    /*
    [t1] - 29	00000000 00000000 00000000 00000000 00011111 01000101 01101000 00000101
    [t2] - 29	00000000 00000000 00000000 00000000 00011111 01000101 11000001 00000101
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        test4();

    }

    private static void test5() throws InterruptedException {

        log.debug("begin");
        for (int i = 0; i < 6; i++) {
            Dog d = new Dog();
            log.debug(ClassLayout.parseInstance(d).toPrintableSimple());
            Thread.sleep(1000);
        }
    }

    static Thread t1, t2, t3;

    private static void test4() throws InterruptedException {
        Vector<Dog> list = new Vector<>();

        int loopNumber = 39;
        t1 = new Thread(() -> {
            for (int i = 0; i < loopNumber; i++) {
                Dog d = new Dog();
                list.add(d);
                synchronized (d) {
                    log.debug(i + "\t" + ClassLayout.parseInstance(d).toPrintableSimple());
                }
            }
            LockSupport.unpark(t2);
        }, "t1");
        t1.start();

        t2 = new Thread(() -> {
            LockSupport.park();
            log.debug("===============> ");
            for (int i = 0; i < loopNumber; i++) {
                Dog d = list.get(i);
                log.debug(i + "\t" + ClassLayout.parseInstance(d).toPrintableSimple());
                synchronized (d) {
                    log.debug(i + "\t" + ClassLayout.parseInstance(d).toPrintableSimple());
                }
                log.debug(i + "\t" + ClassLayout.parseInstance(d).toPrintableSimple());
            }
            LockSupport.unpark(t3);
        }, "t2");
        t2.start();

        t3 = new Thread(() -> {
            LockSupport.park();
            log.debug("===============> ");
            for (int i = 0; i < loopNumber; i++) {
                Dog d = list.get(i);
                log.debug(i + "\t" + ClassLayout.parseInstance(d).toPrintableSimple());
                synchronized (d) {
                    log.debug(i + "\t" + ClassLayout.parseInstance(d).toPrintableSimple());
                }
                log.debug(i + "\t" + ClassLayout.parseInstance(d).toPrintableSimple());
            }
        }, "t3");
        t3.start();

        t3.join();
        log.debug(ClassLayout.parseInstance(new Dog()).toPrintableSimple());
    }

    private static void test3() throws InterruptedException {

        Vector<Dog> list = new Vector<>();
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 30; i++) {
                Dog d = new Dog();
                list.add(d);
                synchronized (d) {
                    log.debug(i + "\t" + ClassLayout.parseInstance(d).toPrintableSimple());
                }
            }
            synchronized (list) {
                list.notify();
            }
        }, "t1");
        t1.start();


        Thread t2 = new Thread(() -> {
            synchronized (list) {
                try {
                    list.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            log.debug("===============> ");
            for (int i = 0; i < 30; i++) {
                Dog d = list.get(i);
                log.debug(i + "\t" + ClassLayout.parseInstance(d).toPrintableSimple());
                synchronized (d) {
                    log.debug(i + "\t" + ClassLayout.parseInstance(d).toPrintableSimple());
                }
                log.debug(i + "\t" + ClassLayout.parseInstance(d).toPrintableSimple());
            }
        }, "t2");
        t2.start();
    }

    // 测试撤销偏向锁
    private static void test2() throws InterruptedException {

        Dog d = new Dog();
        Thread t1 = new Thread(() -> {
            synchronized (d) {
                log.debug(ClassLayout.parseInstance(d).toPrintableSimple());
                //  log.debug("d的hashcode：{}", d.hashCode());
                //  log.debug(ClassLayout.parseInstance(d).toPrintableSimple());
            }
            synchronized (TestBiased.class) {
                TestBiased.class.notify();
            }
        }, "t1");
        t1.start();

        // log.debug("d的hashcode：{}", d.hashCode());
        Thread t2 = new Thread(() -> {
            synchronized (TestBiased.class) {
                try {
                    TestBiased.class.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            log.debug(ClassLayout.parseInstance(d).toPrintableSimple());
            synchronized (d) {
                // log.debug("d的hashcode：{}", d.hashCode());
                log.debug(ClassLayout.parseInstance(d).toPrintableSimple());
            }
            log.debug(ClassLayout.parseInstance(d).toPrintableSimple());
        }, "t2");
        t2.start();

        // 1. 加锁(偏向锁，轻量级锁)期间如果要求 hashcode，则锁直接变成重量级锁
        // 2. 已上偏向锁，但锁对象没被锁住的时候求 hashcode，偏向锁会被撤销，变成轻量级锁
        // 3. 没有其他线程时偏向锁可以提高性能，一旦出现其他线程使用同一个锁，偏向锁就会撤销变成轻量级锁
        // 4. 如果偏向锁还处于使用中就被其他线程访问，那样竞争线程会进入阻塞，直至运行至安全点偏向锁被撤销然后变成重量级锁
    }

    // 测试偏向锁
    private static void test1() {
        Dog d = new Dog();
        log.debug(ClassLayout.parseInstance(d).toPrintableSimple());

        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        log.debug(ClassLayout.parseInstance(new Dog()).toPrintableSimple());
    }

    private static void mytest1() throws InterruptedException {
        Dog d = new Dog();
        Thread t1 = new Thread(() -> {
            log.debug("t1运行中, t1的threadId: {}", Thread.currentThread().getId() );
            log.debug(ClassLayout.parseInstance(d).toPrintableSimple());
            synchronized (d) {
                log.debug(ClassLayout.parseInstance(d).toPrintableSimple());
            }
        }, "t1");
        t1.start();

        t1.join();
        log.debug("主线程等待t1运行完，准备启动t2运行...");
        log.debug(ClassLayout.parseInstance(d).toPrintableSimple());
        Thread t2 = new Thread(() -> {
            log.debug("t2运行中, t2的threadId: {}", Thread.currentThread().getId() );
            log.debug(ClassLayout.parseInstance(d).toPrintableSimple());
            synchronized (d) {
                log.debug(ClassLayout.parseInstance(d).toPrintableSimple());
            }
            log.debug(ClassLayout.parseInstance(d).toPrintableSimple());
        }, "t2");
        t2.start();

        // 分析结果可知，偏向锁锁在一个对象上后，即使线程结束了，该锁也没有被去除。即该过程是不可逆的。
    }

    private static void mytest2(){
        Dog d = new Dog();
        Thread t1 = new Thread(() -> {
            log.debug("t1运行中, t1的threadId: {}", Thread.currentThread().getId() );
            log.debug(ClassLayout.parseInstance(d).toPrintableSimple());
            synchronized (d) {
                log.debug(ClassLayout.parseInstance(d).toPrintableSimple());
                try {
                    Thread.sleep(7000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            log.debug("睡眠后的偏向锁状态...");
            log.debug(ClassLayout.parseInstance(d).toPrintableSimple());
        }, "t1");
        t1.start();

        Thread t2 = new Thread(() -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            log.debug("t2运行中, t2的threadId: {}", Thread.currentThread().getId() );
            log.debug(ClassLayout.parseInstance(d).toPrintableSimple());
            synchronized (d) {
                log.debug(ClassLayout.parseInstance(d).toPrintableSimple());
            }
            log.debug(ClassLayout.parseInstance(d).toPrintableSimple());
        }, "t2");
        t2.start();
    }
}

class Dog {

}


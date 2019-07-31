package com.atguigu.gmall.auth;

import java.util.concurrent.locks.ReentrantLock;

/*
 * 同步锁：了解
 *   lock() 上锁
 *   unlock() 释放锁
 */
public class TicketThreadTest {
    public static void main(String[] args) {

		TicketMyRun mr = new TicketMyRun();
		Thread t1 = new Thread(mr, "窗口1：");
		Thread t2 = new Thread(mr, "窗口2：");
		Thread t3 = new Thread(mr, "窗口3：");

		t1.start();
		t2.start();
		t3.start();
	}
}

class TicketMyRun implements Runnable {
	int num = 100;
	ReentrantLock lock = new ReentrantLock();

	@Override
	public void run() {
		while (true) {
			//创建同步锁
			lock.lock();//上锁
				try {
					// 可能出现线程安全问题的代码
					if (num <= 0) {
						// 没票了
						return;
					} else {
						try {
							Thread.currentThread().sleep(100);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						// 卖票
						System.out.println(Thread.currentThread().getName() + "卖出了票：" + num);
						num--;
					}
				} catch (Exception e) {
					e.printStackTrace();
				} finally{
					//释放锁
					lock.unlock();
				}
		}
	}

}
package com.atguigu.gmall.auth;

public class ThreadTest {
    public static void main(String[] args) {

        TickitSale tickitSale = new TickitSale();

        Thread thread1 = new Thread(tickitSale,"窗口1");
        Thread thread2 = new Thread(tickitSale,"窗口2");
        Thread thread3 = new Thread(tickitSale,"窗口3");

        thread1.start();
        thread2.start();
        thread3.start();
    }
}

class TickitSale implements Runnable{
    int total = 1000;
    @Override
    public void run() {

        for (int i = 1;i<=total;i++){
            System.out.println(Thread.currentThread().getName()+"买到了一编号为"+i+"的票");
        }
    }
}

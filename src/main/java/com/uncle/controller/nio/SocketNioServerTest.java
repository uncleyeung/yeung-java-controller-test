package com.uncle.controller.nio;

import lombok.SneakyThrows;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author 杨戬
 * @className SocketNioServerTest
 * @email uncle.yeung.bo@gmail.com
 * @date 20-4-2 10:51
 */
public class SocketNioServerTest {
    public static void main(String[] args) {
        //无选择器
        //noSelector();
        //有选择器
        selector();
    }

    /**
     * 无选择器
     */
    private static void noSelector() {
        try {
            //创建一个服socket并打开
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            //监听绑定8090端口
            serverSocketChannel.socket().bind(new InetSocketAddress(8090));
            //设置为非阻塞模式
            serverSocketChannel.configureBlocking(false);
            while (true) {
                //获取请求连接
                SocketChannel socketChannel = serverSocketChannel.accept();
                if (socketChannel != null) {
                    ByteBuffer buf1 = ByteBuffer.allocate(1024);
                    socketChannel.read(buf1);
                    buf1.flip();
                    if (buf1.hasRemaining()) {
                        System.out.println(">>>服务端收到数据：" + new String(buf1.array()));
                    }
                    buf1.clear();
                    //构造返回的报文，分为头部和主体，实际情况可以构造复杂的报文协议，这里只演示，不做特殊设计。
                    ByteBuffer header = ByteBuffer.allocate(6);
                    header.put("[head]".getBytes());
                    ByteBuffer body = ByteBuffer.allocate(1024);
                    body.put("i am body!".getBytes());
                    header.flip();
                    body.flip();
                    ByteBuffer[] bufferArray = {header, body};
                    socketChannel.write(bufferArray);

                    socketChannel.close();
                } else {
                    Thread.sleep(1000);
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 有选择器
     */
    private static void selector() {
        try {
            ExecutorService executor = Executors.newSingleThreadExecutor();//线程池
            Selector selector = Selector.open();
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.socket().bind(new InetSocketAddress(8090));
            serverSocketChannel.configureBlocking(false);
            //向通道注册选择器，并且注册接受事件
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            //主线程死循环等待新连接到来
            //为新的连接创建新的线程
            executor.submit(new ConnectIOnHandler(selector));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class ConnectIOnHandler extends Thread {
    private Selector selector;

    public ConnectIOnHandler(Selector selector) {
        this.selector = selector;
    }

    @SneakyThrows
    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted() && selector.isOpen()) {
            //获取已经准备好的通道数量
            int readyChannels = selector.selectNow();
            //如果没准备好，重试
            if (readyChannels == 0) {
                continue;
            }
            //获取准备好的通道中的事件集合
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();
                SocketChannel channel = null;
                if (key.isAcceptable()) {
                    //在自己注册的事件中写业务逻辑，
                    //我这里注册的是accept事件，
                    //这部分逻辑和上面非选择器服务端代码一样。
                    ServerSocketChannel serverSocketChannel1 = (ServerSocketChannel) key.channel();
                    channel = serverSocketChannel1.accept();
                    channel.configureBlocking(false);
                    channel.register(selector, SelectionKey.OP_READ);

                } else if (key.isConnectable()) {
                } else if (key.isReadable()) {
                    channel = (SocketChannel) key.channel();
                    ByteBuffer buf1 = ByteBuffer.allocate(1024);
                    channel.read(buf1);
                    buf1.flip();
                    if (buf1.hasRemaining()) {
                        System.out.println(">>>服务端收到数据：" + new String(buf1.array()));
                    }
                    buf1.clear();

                    ByteBuffer header = ByteBuffer.allocateDirect(6);
                    header.put("[head]".getBytes());
                    ByteBuffer body = ByteBuffer.allocateDirect(1024);
                    body.put("i am body!".getBytes());
                    header.flip();
                    body.flip();
                    ByteBuffer[] bufferArray = {header, body};
                    channel.write(bufferArray);
                    channel.close();
                } else if (key.isWritable()) {

                }
                //注意每次迭代末尾的keyIterator.remove()调用。
                //Selector不会自己从已选择键集中移除SelectionKey实例。必须在处理完通道时自己移除。
                //下次该通道变成就绪时，Selector会再次将其放入已选择键集中
                keyIterator.remove();
            }
        }
    }
}
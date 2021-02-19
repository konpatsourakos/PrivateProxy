package privacy.proxy.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static privacy.proxy.Utils.getServerSocket;

public class Server {


        //TODO: benchmark magic number. Starting at 2 times the cpu cores
        private int NumberOfThread = 8;

        // simple server handler. Each connected client will be handled by a new thread.
        // Accept is a single thread, this should be fine since, it does minimal work
        // and the bottleneck here should be the network buffer
        // TODO: benchmark to identify inefficiencies/extend to mutilthread accept loop
        public void start(InetSocketAddress address, ProxyHandler proxyHandler) {
                try (ServerSocket serverSocket = getServerSocket(address)) {
                        ExecutorService workPool = newCachedThreadPool(NumberOfThread);
                        while (true) {
                                try {
                                        Socket socket = serverSocket.accept();
                                        workPool.submit(() -> {
                                                proxyHandler.handle(socket);
                                        });
                                } catch (IOException e) {
                                        System.err.println("Exception while handling connection");
                                        e.printStackTrace();
                                }
                        }
                } catch (Exception e) {
                        System.err.println("Could not create socket at " + address);
                        e.printStackTrace();
                }
        }

        // Kill connections that are stale for too long if we have too many threads
        // TODO: benchmark magic number on threadpool
        private ExecutorService newCachedThreadPool(int maximumNumberOfThreads) {
                return new ThreadPoolExecutor(maximumNumberOfThreads/2, maximumNumberOfThreads,
                        60L, TimeUnit.SECONDS,
                        new SynchronousQueue<>());
        }

        public static void main( String[] args) {
                new Server().start(new InetSocketAddress("127.0.0.1", 8443),
                        new ProxyHandler(new TunnelHandler()));
        }
}

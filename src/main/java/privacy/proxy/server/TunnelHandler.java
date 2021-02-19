package privacy.proxy.server;



import com.google.common.annotations.VisibleForTesting;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;


public class TunnelHandler {
    // TODO: benchmark this magic number
    private static final int BUFFER_SIZE = 1024;

    private void closeSocket(Socket socket) {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Set up forwarding between the client socket and the remote host
     * @param clientSocket the client socket we want to tunnel
     * @param server the server we want to connect to
     * @param port the remote port we want to connect to
     * @throws IOException
     * @throws InterruptedException
     */
    public void handle(Socket clientSocket, String server, int port) throws IOException, InterruptedException {
        // setup tunnel connection on the tcp layer
        Socket serverSocket = new Socket(server, port);
        serverSocket.setKeepAlive(true);

        // connect the two streams. The client will connect with tls to the remote host to shield it's privacy
        forward(clientSocket.getInputStream(), serverSocket.getOutputStream(), () -> closeSocket(clientSocket));
        forward(serverSocket.getInputStream(), clientSocket.getOutputStream(), () -> closeSocket(serverSocket));
    }

    /**
     * Simple function to link an input and an output stream. Runs on it's own thread to keep tunnel alive
     * @param in the input stream to pipe from
     * @param out the output stream to pipe to
     * @param cleanup calls a lambda once the stream is ended to cleanup resources
     * @return a thread that performs the
     */
    @VisibleForTesting
    public Thread forward(InputStream in, OutputStream out, Runnable cleanup) {
        byte[] buffer = new byte[BUFFER_SIZE];
        Thread t = new Thread(() -> {
            try {
                int i = 0;
                while (true) {
                    int bytesRead = in.read(buffer);
                    System.out.println(new String(Arrays.copyOfRange(buffer,0, 100)));
                    System.out.println("read data " + i++);
                    if (bytesRead == -1) break; // End of stream is reached --> exit
                    out.write(buffer, 0, bytesRead);
                    out.flush();
                }
                System.out.println("END OF TUNNEL");
                cleanup.run();
            } catch (IOException ex) {
                ex.printStackTrace();
                //TODO make a callback on exception.
            }
        });
        t.setDaemon(true);
        t.start();
        return t;
    }
}

package privacy.proxy.server;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.Semaphore;


public class ProxyHandlerTest {

    Semaphore completed;
    ProxyHandler handler;
    @Mock
    Socket socket;
    class MockTunnelHandler extends TunnelHandler {
        @Override
        public void handle(Socket clientSocket, String server, int port) throws IOException, InterruptedException {
            completed.release();
        }
    }

    @Before
    public void setup() {
        handler = new ProxyHandler(new MockTunnelHandler());
        MockitoAnnotations.initMocks(this);
    }

    @Test(timeout = 1000)
    public void testHandler_HappyCase() throws IOException, InterruptedException {
        completed = new Semaphore(0);
        String str = "CONNECT api.giphy.com:443\n\r\n\r";
        Mockito.when(socket.getInputStream()).thenReturn(new ByteArrayInputStream(str.getBytes()));
        OutputStream out = new ByteArrayOutputStream(str.length()+1);
        Mockito.when(socket.getOutputStream()).thenReturn(out);
        handler.handle(socket);
        completed.acquire();
        assert (out.toString().contains("200 OK"));
    }

    @Test(timeout = 1000)
    public void testHandler_wrongPort() throws IOException, InterruptedException {
        completed = new Semaphore(0);
        String str = "CONNECT api.giphy.com.com:80\n\r\n\r";
        Mockito.when(socket.getInputStream()).thenReturn(new ByteArrayInputStream(str.getBytes()));
        OutputStream out = new ByteArrayOutputStream(str.length()+1);
        Mockito.when(socket.getOutputStream()).thenReturn(out);
        handler.handle(socket);
        assert (out.toString().contains("401 Unauthorized"));
    }

    @Test(timeout = 1000)
    public void testHandler_wrongDomain() throws IOException, InterruptedException {
        completed = new Semaphore(0);
        String str = "CONNECT unknown.domain:443\n\r\n\r";
        Mockito.when(socket.getInputStream()).thenReturn(new ByteArrayInputStream(str.getBytes()));
        OutputStream out = new ByteArrayOutputStream(str.length()+1);
        Mockito.when(socket.getOutputStream()).thenReturn(out);
        handler.handle(socket);
        assert (out.toString().contains("401 Unauthorized"));
    }

    @Test(timeout = 1000)
    public void testHandler_wrongVerb() throws IOException {
        completed = new Semaphore(0);
        String str = "GET api.giphy.com:443\n\r\n\r";
        Mockito.when(socket.getInputStream()).thenReturn(new ByteArrayInputStream(str.getBytes()));
        OutputStream out = new ByteArrayOutputStream(str.length()+1);
        Mockito.when(socket.getOutputStream()).thenReturn(out);
        handler.handle(socket);
        assert !completed.tryAcquire();
    }
}

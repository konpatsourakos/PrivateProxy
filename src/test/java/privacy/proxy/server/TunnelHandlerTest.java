package privacy.proxy.server;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.Semaphore;

public class TunnelHandlerTest {
    TunnelHandler handler;

    @Before
    public void setup() {
        handler = new TunnelHandler();
    }

    @Test(timeout = 1000)
    public void testForwardStream() throws InterruptedException {
        Semaphore completed = new Semaphore(0);
        String inputStr = UUID.randomUUID().toString();
        OutputStream out = new ByteArrayOutputStream(inputStr.length()+1);

        handler.forward(new ByteArrayInputStream(inputStr.getBytes()), out, completed::release);
        completed.acquire();

        assert (out.toString().equals(inputStr));
    }
}

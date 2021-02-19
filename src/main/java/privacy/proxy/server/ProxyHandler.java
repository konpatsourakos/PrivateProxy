package privacy.proxy.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static privacy.proxy.Utils.getHeaderLines;
import static privacy.proxy.Utils.proxyAcceptMessage;
import static privacy.proxy.Utils.proxyDenyMessage;

public class ProxyHandler {
    final private Charset encoding = StandardCharsets.UTF_8;
    final private Set<String> allowedHosts = Collections.singleton("api.giphy.com");
    final private Set<Integer> allowedPorts = Collections.singleton(443);
    TunnelHandler tunnelHandler;

    ProxyHandler(TunnelHandler tunnelHandler) {
        this.tunnelHandler = tunnelHandler;
    }

    public void handle(Socket socket) {
        try {
            socket.setKeepAlive(true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    socket.getInputStream(), encoding.name()));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                    socket.getOutputStream(), encoding.name()));

            List<String> headers = getHeaderLines(reader);
            // TODO: This could be cleaner, use a library to handle HTTP headers
            if (!headers.get(0).startsWith("CONNECT ")) {
                fail(writer, socket);
                return;
            }
            String host = headers.get(0).split("[ :]")[1];
            int port = Integer.parseInt(headers.get(0).split("[ :]")[2]);
            if (!allowedHosts.contains(host) || !allowedPorts.contains(port)) {
                fail(writer, socket);
                return;
            }
            // TODO: add authentication. We should use a separate thread pool
            //  for such operations (talking to sql/file operations) and a callback
            writer.write(proxyAcceptMessage(encoding));
            writer.flush();
            tunnelHandler.handle(socket, host, port);
        } catch (InterruptedException e) {
            System.err.println("Exception while creating response");
            e.printStackTrace();
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            System.err.println("Exception while creating response");
            e.printStackTrace();
        }
    }

    private void fail(BufferedWriter writer, Socket socket) throws IOException {
        writer.write(proxyDenyMessage(encoding));
        writer.flush();
        socket.close();
    }
}

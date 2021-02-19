package privacy.proxy.client;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static privacy.proxy.Utils.getClientSocket;
import static privacy.proxy.Utils.getHeaderLines;
import static privacy.proxy.Utils.readLines;
import static privacy.proxy.Utils.setKeyStoreProperties;

public class TestClient {

    private final Charset encoding = StandardCharsets.UTF_8;

    public static void main(String[] args) throws Exception {
        new TestClient().start();
    }

    private void start() throws Exception {
        String host = "api.giphy.com";
        int port = 443;
        SSLSocketFactory factory =
                (SSLSocketFactory)SSLSocketFactory.getDefault();

        setKeyStoreProperties();
        Socket socket = getClientSocket(new InetSocketAddress("127.0.0.1", 8443));
        socket.setKeepAlive(true);
        doTunnelHandshake(socket, host, 443);

        // setup ssl with the host, TODO: verify certificate matches the host (nitpick)
        SSLSocket finalSocket =
                (SSLSocket) factory.createSocket(socket, host, port, true);
        finalSocket.setKeepAlive(true);
        finalSocket.startHandshake();

        doGetRequest(finalSocket);

        // print the response
        BufferedReader in = new BufferedReader(new InputStreamReader(finalSocket.getInputStream()));
        for (String line : readLines(in, true)) {
            System.out.println(line);
        }
        in.close();
        finalSocket.close();
        socket.close();
    }

    /*
     * Do a http2 compatible GET on the giphy server
     */
    private void doGetRequest(SSLSocket finalSocket) throws IOException {
        String msg = "GET /v1/gifs/search?q=signal&api_key=dc6zaTOxFJmzC HTTP/1.1\r\n" +
                "Host: api.giphy.com\r\n" +
                "Connection: Upgrade, HTTP2-Settings\r\n" +
                "Upgrade: h2c\r\n\r\n";

        finalSocket.getOutputStream().write(msg.getBytes());
        finalSocket.getOutputStream().flush();
    }


    /*
     * Follow the tunnel protocol
     */
    private void doTunnelHandshake(Socket tunnel, String host, int port) throws IOException {
        OutputStream out = tunnel.getOutputStream();
        String msg = "CONNECT " + host + ":" + port + " HTTP/1.0\n"
                + "\r\n\r\n";
        out.write(msg.getBytes(StandardCharsets.UTF_8));
        out.flush();


        BufferedReader reader = new BufferedReader(new InputStreamReader(
                tunnel.getInputStream(), encoding.name()));
        List<String> headers = getHeaderLines(reader);

        // verify successful connection
        if (!headers.get(0).contains("200 OK")) {
            throw new IOException("Unable to connect to proxy, error message: " + headers.get(0));
        }
    }
}

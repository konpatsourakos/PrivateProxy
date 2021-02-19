package privacy.proxy;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Useful functions to use across client/server
 */
public class Utils {
    private static final String keyStore = "./keystore.jks";
    private static final String pass = "pass_for_self_signed_cert";

    public static Socket getClientSocket(InetSocketAddress address)
            throws Exception {
        Path keyStorePath = Paths.get(keyStore);
        char[] keyStorePassword = pass.toCharArray();

        // Create socket with custom key store
        Socket socket = getSslContext(keyStorePath, keyStorePassword)
                .getSocketFactory().createSocket(address.getAddress(), address.getPort());

        // We don't need the password anymore → Overwrite it
        Arrays.fill(keyStorePassword, '0');

        return socket;
    }

    public static ServerSocket getServerSocket(InetSocketAddress address)
            throws Exception {

        // Backlog is the maximum number of pending connections on the socket,
        // 0 means that an implementation-specific default is used
        int backlog = 0;

        Path keyStorePath = Paths.get(keyStore);
        char[] keyStorePassword = pass.toCharArray();

        // Bind the socket to the given port and address
        ServerSocket serverSocket = getSslContext(keyStorePath, keyStorePassword)
                .getServerSocketFactory()
                .createServerSocket(address.getPort(), backlog, address.getAddress());

        // We don't need the password anymore → Overwrite it
        Arrays.fill(keyStorePassword, '0');

        return serverSocket;
    }

    private static SSLContext getSslContext(Path keyStorePath, char[] keyStorePass)
            throws Exception {

        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(new FileInputStream(keyStorePath.toFile()), keyStorePass);

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
        keyManagerFactory.init(keyStore, keyStorePass);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        // Null means using default implementations for TrustManager and SecureRandom
        sslContext.init(keyManagerFactory.getKeyManagers(), null, null);
        return sslContext;
    }

    public static List<String> getHeaderLines(BufferedReader reader) throws IOException {
        return readLines(reader, false);
    }

    public static List<String> readLines(BufferedReader reader, boolean getBody)
            throws IOException {
        ArrayList<String> lines = new ArrayList<>();
        String line = reader.readLine();
        // An empty line marks the end of the request's header
        while (!line.isEmpty()) {
            lines.add(line);
            line = reader.readLine();
        }
        while(getBody && (line=reader.readLine())!=null) {
            lines.add(line);
        }
        return lines;
    }

    public static String proxyDenyMessage(Charset encoding) {
        String body = "Connection refused. You don't have permission to connect to the requested address";
        long contentLength = body.getBytes(encoding).length;

        return "HTTP/1.1 401 Unauthorized\r\n" +
                String.format("Content-Length: %d\r\n", contentLength) +
                String.format("Content-Type: text/plain; charset=%s\r\n", encoding.displayName()) +
                "\r\n" + body;
    }

    public static String proxyAcceptMessage(Charset encoding) {
        return "HTTP/1.0 200 OK\r\n" +
                String.format("Content-Length: %d\r\n", 0) +
                String.format("Content-Type: text/plain; charset=%s\r\n", encoding.displayName()) +
                "\r\n";
    }

    public static void setKeyStoreProperties() {
        System.setProperty("javax.net.ssl.keyStore", keyStore);
        System.setProperty("javax.net.ssl.keyStorePassword", pass);
        System.setProperty("javax.net.ssl.trustStore", keyStore);
        System.setProperty("javax.net.ssl.trustStorePassword", pass);
    }
}

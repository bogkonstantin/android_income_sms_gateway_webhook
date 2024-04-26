package tech.bogomolov.incomingsmsgateway.SSLSocketFactory;

import android.annotation.SuppressLint;
import android.net.SSLCertificateSocketFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class TLSSocketFactory extends SSLSocketFactory {

    private final SSLSocketFactory factory;

    @SuppressLint("SSLCertificateSocketFactoryGetInsecure")
    public TLSSocketFactory(boolean ignoreSsl) throws KeyManagementException, NoSuchAlgorithmException {
        if (ignoreSsl) {
            factory = SSLCertificateSocketFactory.getInsecure(0, null);
        } else {
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, null, null);
            factory = context.getSocketFactory();
        }
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return factory.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return factory.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket() throws IOException {
        return enableTLSOnSocket(factory.createSocket());
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        return enableTLSOnSocket(factory.createSocket(s, host, port, autoClose));
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        return enableTLSOnSocket(factory.createSocket(host, port));
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
        return enableTLSOnSocket(factory.createSocket(host, port, localHost, localPort));
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return enableTLSOnSocket(factory.createSocket(host, port));
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return enableTLSOnSocket(factory.createSocket(address, port, localAddress, localPort));
    }

    private Socket enableTLSOnSocket(Socket socket) {
        if((socket instanceof SSLSocket)) {
            String[] supportedProtocols = ((SSLSocket)socket).getSupportedProtocols();
            ((SSLSocket)socket).setEnabledProtocols(supportedProtocols);
        }
        return socket;
    }
}

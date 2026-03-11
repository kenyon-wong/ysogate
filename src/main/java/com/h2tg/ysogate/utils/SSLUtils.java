package com.h2tg.ysogate.utils;

import com.h2tg.ysogate.config.JndiConfig;
import sun.security.x509.*;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * SSL 工具类, 用于生成 SSLContext 以支持 LDAPS 协议
 *
 * 支持两种模式:
 * 1. 加载外部 JKS 文件 (通过 -keystore/-storepass 参数指定)
 * 2. 运行时生成自签名证书 (默认)
 */
public class SSLUtils {

    /**
     * 创建 SSLContext
     * 优先从 JKS 文件加载, 如果未指定则生成自签名证书
     *
     * @return 配置好的 SSLContext
     * @throws Exception 如果创建失败
     */
    public static SSLContext createSSLContext() throws Exception {
        if (JndiConfig.keystore != null) {
            return createSSLContextFromJKS(JndiConfig.keystore, JndiConfig.storepass);
        }
        return createSelfSignedSSLContext();
    }

    /**
     * 从 JKS 文件加载证书创建 SSLContext
     *
     * @param jksPath   JKS 文件路径
     * @param password  JKS 密码
     * @return SSLContext
     * @throws Exception 如果加载失败
     */
    public static SSLContext createSSLContextFromJKS(String jksPath, String password) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream(jksPath)) {
            keyStore.load(fis, password.toCharArray());
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, password.toCharArray());

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), null, new SecureRandom());

        return sslContext;
    }

    /**
     * 使用自签名证书创建 SSLContext
     *
     * @return SSLContext
     * @throws Exception 如果创建失败
     */
    public static SSLContext createSelfSignedSSLContext() throws Exception {
        // 生成 RSA 密钥对
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048, new SecureRandom());
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        // 生成自签名 X509 证书 (包含 SAN 扩展)
        X509Certificate cert = generateSelfSignedCert(keyPair);

        // 构建 KeyStore
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        keyStore.setKeyEntry("ldaps", keyPair.getPrivate(), "changeit".toCharArray(),
                new java.security.cert.Certificate[]{cert});

        // 初始化 KeyManagerFactory
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, "changeit".toCharArray());

        // 初始化 TrustManager (信任所有证书，因为是自签名)
        TrustManager[] trustManagers = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }
        };

        // 创建 SSLContext
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), trustManagers, new SecureRandom());

        return sslContext;
    }

    /**
     * 创建 SSLServerSocketFactory
     *
     * @return SSLServerSocketFactory
     * @throws Exception 如果创建失败
     */
    public static SSLServerSocketFactory createSSLServerSocketFactory() throws Exception {
        return createSSLContext().getServerSocketFactory();
    }

    /**
     * 生成包含 SAN 扩展的自签名 X509 V3 证书
     *
     * SAN 中包含 127.0.0.1, 0.0.0.0, localhost
     *
     * @param keyPair RSA 密钥对
     * @return 自签名 X509 证书
     * @throws Exception 如果生成失败
     */
    private static X509Certificate generateSelfSignedCert(KeyPair keyPair) throws Exception {
        long now = System.currentTimeMillis();
        Date notBefore = new Date(now);
        Date notAfter = new Date(now + 365L * 24 * 60 * 60 * 1000); // 有效期 1 年

        X500Name issuer = new X500Name("CN=ysogate, O=H2TG, C=CN");

        X509CertInfo certInfo = new X509CertInfo();

        certInfo.set(X509CertInfo.VERSION, new CertificateVersion(CertificateVersion.V3));
        certInfo.set(X509CertInfo.SERIAL_NUMBER,
                new CertificateSerialNumber(new BigInteger(64, new SecureRandom())));

        AlgorithmId algId = AlgorithmId.get("SHA256withRSA");
        certInfo.set(X509CertInfo.ALGORITHM_ID, new CertificateAlgorithmId(algId));
        certInfo.set(X509CertInfo.ISSUER, issuer);
        certInfo.set(X509CertInfo.SUBJECT, issuer);
        certInfo.set(X509CertInfo.VALIDITY, new CertificateValidity(notBefore, notAfter));
        certInfo.set(X509CertInfo.KEY, new CertificateX509Key(keyPair.getPublic()));

        // 添加 Subject Alternative Names (SAN) 扩展
        GeneralNames generalNames = new GeneralNames();
        generalNames.add(new GeneralName(new IPAddressName("127.0.0.1")));
        generalNames.add(new GeneralName(new IPAddressName("0.0.0.0")));
        generalNames.add(new GeneralName(new DNSName("localhost")));

        SubjectAlternativeNameExtension sanExt = new SubjectAlternativeNameExtension(generalNames);
        CertificateExtensions extensions = new CertificateExtensions();
        extensions.set(SubjectAlternativeNameExtension.NAME, sanExt);
        certInfo.set(X509CertInfo.EXTENSIONS, extensions);

        // 创建并签名证书
        X509CertImpl cert = new X509CertImpl(certInfo);
        cert.sign(keyPair.getPrivate(), "SHA256withRSA");

        return cert;
    }
}

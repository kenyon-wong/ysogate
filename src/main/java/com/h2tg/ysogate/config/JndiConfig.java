package com.h2tg.ysogate.config;

public class JndiConfig
{
    public static String ip = "127.0.0.1";
    public static int rmiPort = 1099;
    public static int ldapPort = 1389;
    public static int ldapsPort = 1636;
    public static int httpPort = 3456;
    public static String url;
    public static String codebase;
    public static String file;
    // 固定路径模式: 所有请求都路由到指定路径
    public static String fixedPath;
    // 绕过 trustSerialData
    public static boolean onlyRef = false;
    public static boolean ldap2rmi = false;
    // LDAPS 证书 JKS 文件路径
    public static String keystore;
    // LDAPS JKS 密码
    public static String storepass = "changeit";
}

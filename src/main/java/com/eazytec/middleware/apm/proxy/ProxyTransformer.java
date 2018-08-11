package com.eazytec.middleware.apm.proxy;

import com.eazytec.middleware.apm.AgentInit;
import javassist.*;
import com.eazytec.middleware.apm.match.Execution;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ProxyTransformer implements ClassFileTransformer{

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        byte[] byteCode = classfileBuffer;
        className = className.replace('/', '.');
        //排除包路径
        Set<String> packages = AgentInit.excludePackages;
        if (null != packages && !packages.isEmpty()) {
            for (String packageName : packages) {
                if (className.startsWith(packageName)) {
                    return byteCode;
                }
            }
        }
        //排除关键词
        Set<String> words = AgentInit.excludeKeyWords;
        if (null != words && !words.isEmpty()) {
            for (String word : words) {
                if (className.contains(word)) {
                    return byteCode;
                }
            }
        }

        if (null == loader) {
            loader = Thread.currentThread().getContextClassLoader();
        }
        byteCode = matchClass(loader, className, byteCode);
        return byteCode;
    }

    //match class
    private byte[] matchClass(ClassLoader loader, String className, byte[] byteCode) {
        CtClass cc = null;
        ClassPool cp = ClassPool.getDefault();
        try {
            cc = cp.get(className);
        } catch (NotFoundException e) {
            cp.insertClassPath(new LoaderClassPath(loader));
            try {
                cc = cp.get(className);
            } catch (NotFoundException e1) {
                System.err.println("ProxyTransformer matchClass not found  class :" + className);
            }
        }

        if (null == cc) {
            return byteCode;
        }

        if (!cc.isInterface()) {
            //注解
            Execution execution = AgentInit.regular.matchAnnotation(cc,loader);
            if(execution != null){
                byteCode = matchMethod(Collections.singletonList(execution),cc, className, byteCode);
            }
            else{
                List<Execution> list = AgentInit.regular.matchClassName(cc,loader);
                if(list != null && list.size() > 0){
                    byteCode = matchMethod(list,cc, className, byteCode);
                }
            }
        }

        return byteCode;
    }

    //match method
    private byte[] matchMethod(List<Execution> executions ,CtClass cc, String className, byte[] byteCode) {
        try {
            CtMethod[] methods = cc.getDeclaredMethods();
            if (null != methods && methods.length > 0) {
                //TODO： 方法名称，代理实现 通过配置 main方法
                for (CtMethod m : methods) {
                    if(!checkModify(m)){
                        continue;
                    }
                    for (Execution execution : executions) {
                        if(AgentInit.regular.matchMethod(m,execution)){
                            MethodProxy.aroundProxy(className, cc, m, execution.getAdvice());
                            break;
                        }
                    }
                }
                byteCode = cc.toBytecode();
            }
            cc.detach();
        } catch (Exception e) {
            System.err.println("ProxyTransformer matchMethod:" +e.getMessage());
        }
        return byteCode;
    }

    private boolean checkModify(CtMethod m ){
        Set<Integer> modifies = AgentInit.allowedMethodModifies;
        if (null == modifies || modifies.isEmpty()) {
            return true;
        }
        for (Integer modify : modifies) {
            if (modify.equals(m.getModifiers())) {
                return true;
            }
        }
        return false;
    }
}
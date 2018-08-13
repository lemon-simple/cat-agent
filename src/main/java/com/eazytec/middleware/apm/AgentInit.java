package com.eazytec.middleware.apm;

import com.eazytec.middleware.apm.match.Execution;
import com.eazytec.middleware.apm.match.Pointcut;
import com.eazytec.middleware.apm.match.Regular;
import javassist.bytecode.AccessFlag;

import java.util.HashSet;
import java.util.Set;

public class AgentInit {

    public static final Regular regular = new Regular();

    public static final Set<String> excludePackages = new HashSet<>();
    public static final Set<String> excludeKeyWords = new HashSet<>();
    public static final Set<Integer> allowedMethodModifies = new HashSet<>();

    static {
        excludePackages.add("org.aspectj.");
        excludePackages.add("sun.reflect.");
        excludePackages.add("com.eazytec.middleware.apm.");
        excludePackages.add("javax.");
        excludePackages.add("java.");
        excludePackages.add("com.sun.");
        excludePackages.add("com.alibaba.dubbo.common.bytecode.");

        excludeKeyWords.add("$"); //TODO: 不支持内部类
        excludeKeyWords.add("__");
     /*   excludeKeyWords.add("$Proxy");
        excludeKeyWords.add("CGLIB$$");*/

        allowedMethodModifies.add(AccessFlag.PUBLIC);

        regular.add(
                Pointcut.create("spring-controller","com.eazytec.middleware.apm.advice.CatAroundAdvice")
                .execution(Execution.createByAnnotation("org.springframework.stereotype.Controller"))
        );

        regular.add(
                Pointcut.create("spring-service","com.eazytec.middleware.apm.advice.CatAroundAdvice")
                .execution(Execution.createByAnnotation("org.springframework.stereotype.Service"))
        );

        regular.add(
                Pointcut.create("dubbo-client","com.eazytec.middleware.apm.plugin.dubbo.DubboClientAroundAdvice")
                .execution(Execution.createByClassName("com.alibaba.dubbo.rpc.cluster.support.AbstractClusterInvoker").methodName("invoke"))
        );

        regular.add(
                Pointcut.create("dubbo-server","com.eazytec.middleware.apm.plugin.dubbo.DubboServerAroundAdvice")
                .execution(Execution.createByClassName("com.alibaba.dubbo.rpc.proxy.AbstractProxyInvoker").methodName("invoke"))
        );

        regular.add(
                Pointcut.create("http-client","com.eazytec.middleware.apm.plugin.http.HttpClientAroundAdvice")
                .execution(Execution.createByInterface("org.apache.http.client.HttpClient"))
        );

        regular.add(
                Pointcut.create("http-server","com.eazytec.middleware.apm.plugin.http.HttpServerAroundAdvice")
                .execution(Execution.createByClassName("org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter")
                        .methodName("handleInternal")
                        .paramTypes("javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse,org.springframework.web.method.HandlerMethod"))
        );

        regular.add(
                Pointcut.create("db-mysql","com.eazytec.middleware.apm.plugin.db.MysqlClientAroundAdvice")
                .execution(Execution.createByClassName("com.mysql.jdbc.PreparedStatement").methodName("execute"))
                .execution(Execution.createByClassName("com.mysql.jdbc.PreparedStatement").methodName("executeQuery"))
                .execution(Execution.createByClassName("com.mysql.jdbc.PreparedStatement").methodName("executeUpdate"))
        );

    }

}

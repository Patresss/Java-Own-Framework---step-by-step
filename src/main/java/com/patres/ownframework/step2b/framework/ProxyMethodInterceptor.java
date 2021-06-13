package com.patres.ownframework.step2b.framework;

import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

public class ProxyMethodInterceptor implements MethodInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(ProxyMethodInterceptor.class);

    @Override
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
        try {
            logger.info("TRANSACTION:   BEGIN");
            final Object invoke = proxy.invokeSuper(obj, args);
            logger.info("TRANSACTION:   COMMIT");
            return invoke;
        } catch (Exception e) {
            logger.info("TRANSACTION:   ROLLBACK");
            throw e;
        }
    }
}

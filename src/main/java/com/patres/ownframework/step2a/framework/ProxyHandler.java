package com.patres.ownframework.step2a.framework;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class ProxyHandler implements InvocationHandler {

    private static final Logger logger = LoggerFactory.getLogger(ProxyHandler.class);
    private final Object objectToHandle;

    public ProxyHandler(Object objectToHandle) {
        this.objectToHandle = objectToHandle;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            logger.info("TRANSACTION:   BEGIN");
            final Object invoke = method.invoke(objectToHandle, args);
            logger.info("TRANSACTION:   COMMIT");
            return invoke;
        } catch (Exception e) {
            logger.info("TRANSACTION:   ROLLBACK");
            throw e;
        }
    }

}

package com.patres.ownframework.step4.framework;

import com.patres.ownframework.step4.framework.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ProxyHandler implements InvocationHandler {

    private static final Logger logger = LoggerFactory.getLogger(ProxyHandler.class);
    private final Object objectToHandle;

    public ProxyHandler(Object objectToHandle) {
        this.objectToHandle = objectToHandle;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (isTransactional(method)) {
            return handleTransaction(method, args);
        }
        return method.invoke(objectToHandle, args);
    }

    private Object handleTransaction(Method method, Object[] args) throws IllegalAccessException, InvocationTargetException {
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

    private boolean isTransactional(Method method) {
        try {
            return objectToHandle.getClass().getMethod(method.getName(), method.getParameterTypes()).isAnnotationPresent(Transactional.class);
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

}

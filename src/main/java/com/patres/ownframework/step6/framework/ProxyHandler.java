package com.patres.ownframework.step6.framework;

import com.patres.ownframework.step6.framework.annotation.Cacheable;
import com.patres.ownframework.step6.framework.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProxyHandler implements InvocationHandler {

    private static final Logger logger = LoggerFactory.getLogger(ProxyHandler.class);
    private final Object objectToHandle;

    private final Map<List<Object>, Object> cacheContainer = new HashMap<>();

    public ProxyHandler(Object objectToHandle) {
        this.objectToHandle = objectToHandle;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (isCacheable(method)) {
            final Object result = cacheContainer.get(createCacheKey(method, args));
            if (result != null) {
                return result;
            }
        }
        if (isTransactional(method)) {
            return handleTransaction(method, args);
        }
        return calculateResult(method, args);
    }

    private Object handleTransaction(Method method, Object[] args) throws IllegalAccessException, InvocationTargetException {
        try {
            beginTransaction();
            final Object invoke = calculateResult(method, args);
            commitTransaction();
            return invoke;
        } catch (Exception e) {
            rollbackTransaction();
            throw e;
        }
    }

    private Object calculateResult(Method method, Object[] args) throws IllegalAccessException, InvocationTargetException {
        final Object result = method.invoke(objectToHandle, args);
        if (isCacheable(method)) {
            cacheContainer.put(createCacheKey(method, args), result);
        }
        return result;
    }

    private boolean isTransactional(Method method) {
        try {
            return objectToHandle.getClass().getMethod(method.getName(), method.getParameterTypes()).isAnnotationPresent(Transactional.class);
        } catch (NoSuchMethodException e) {
            return false;
        }
    }


    private boolean isCacheable(Method method) {
        try {
            return objectToHandle.getClass().getMethod(method.getName(), method.getParameterTypes()).isAnnotationPresent(Cacheable.class);
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private List<Object> createCacheKey(Method method, Object[] args) {
        return List.of(method, Arrays.asList(args));
    }

    private void beginTransaction() {
        logger.debug("BEGIN TRANSACTION");
    }

    private void commitTransaction() {
        logger.debug("COMMIT TRANSACTION");
    }

    private void rollbackTransaction() {
        logger.error("ROLLBACK TRANSACTION");
    }
    
}

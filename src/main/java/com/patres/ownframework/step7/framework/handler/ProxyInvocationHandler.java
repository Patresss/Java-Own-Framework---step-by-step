package com.patres.ownframework.step7.framework.handler;

import com.patres.ownframework.step7.framework.exception.FrameworkException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

public class ProxyInvocationHandler implements InvocationHandler {

    private final Object objectToHandle;
    private final CacheableHandler cacheHandler;
    private final TransactionalHandler transactionHandler;

    public ProxyInvocationHandler(final Object objectToHandle) {
        this.objectToHandle = objectToHandle;
        this.cacheHandler = new CacheableHandler(objectToHandle);
        this.transactionHandler = new TransactionalHandler(objectToHandle);
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) {
        if (cacheHandler.isSupported(method)) {
            final Optional<Object> cachedResult = cacheHandler.takeResultIfExist(method, args);
            if (cachedResult.isPresent()) {
                return cachedResult.get();
            }
        }

        if (transactionHandler.isSupported(method)) {
            return transactionHandler.executeWithTransaction(() -> calculateResult(method, args));
        }

        return calculateResult(method, args);
    }

    private Object calculateResult(final Method method, final Object[] args) {
        try {
            final Object result = method.invoke(objectToHandle, args);
            if (cacheHandler.isSupported(method)) {
                cacheHandler.addResultToCache(method, args, result);
            }
            return result;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new FrameworkException(e);
        }
    }

}

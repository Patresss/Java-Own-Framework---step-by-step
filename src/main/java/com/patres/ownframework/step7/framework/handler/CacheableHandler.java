package com.patres.ownframework.step7.framework.handler;

import com.patres.ownframework.step7.framework.annotation.Cacheable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;

public class CacheableHandler extends AbstractProxyHandler {

    private static final Logger logger = LoggerFactory.getLogger(CacheableHandler.class);

    private final Map<List<Object>, Object> cacheContainers = new HashMap<>();

    public CacheableHandler(final Object objectToHandle) {
        super(objectToHandle, Cacheable.class);
    }

    public List<Object> createKeyCache(final Method method, final Object[] args) {
        return List.of(method, Arrays.asList(args));
    }

    public Optional<Object> takeResultIfExist(final Method method, Object[] args) {
        final List<Object> keyCache = createKeyCache(method, args);
        if (cacheContainers.containsKey(keyCache)) {
            final Object result = cacheContainers.get(keyCache);
            logger.debug("Taking result from the cache | key: {}, value: {}", keyCache, result);
            return Optional.ofNullable(result);
        }
        return Optional.empty();
    }

    public void addResultToCache(Method method, Object[] args, Object result) {
        final List<Object> keyCache = createKeyCache(method, args);
        logger.debug("Adding result to the cache | key: {}, value: {}", keyCache, result);
        cacheContainers.put(keyCache, result);
    }

}

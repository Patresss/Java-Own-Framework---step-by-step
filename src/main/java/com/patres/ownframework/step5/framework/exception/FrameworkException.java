package com.patres.ownframework.step5.framework.exception;

public class FrameworkException extends RuntimeException {

    public FrameworkException(String message) {
        super(message);
    }

    public FrameworkException(Throwable throwable) {
        super("Unknown exception", throwable);
    }

}

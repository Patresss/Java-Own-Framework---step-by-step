# [PL] Java Own Framework - krok po kroku: [Polska wersja](https://github.com/Patresss/Java-Own-Framework---step-by-step/blob/main/README_pl.md)
# [ENG] Java Own Framework - step by step


The goal of this project is to show how a dependency injection framework such as Spring works.

The repository will demonstrate step by step how to build your own framework. Of course this is only a simplified form.
Spring has been developed for almost 20 years by hundreds of developers, so we will barely come close. However, the repository will show the concept of such a framework and prove that there is no magic hidden there.

### Videos:
* The Magic of Spring • Patryk Piechaczek • Devoxx Poland 2021 - https://www.youtube.com/watch?v=5gttHK04lQ4
* JDD 2022: Patryk Piechaczek - The Magic of Spring - how does it really work? - https://www.youtube.com/watch?v=p_zTjkos8p0
* [PL] Bydgoszcz JUG - Meetup #39 - https://www.youtube.com/live/pyA6-tVg2yI?feature=share&t=3719

In the repository you will find [packages](https://github.com/Patresss/Java-Own-Framework---step-by-step/tree/main/src/main/java/com/patres/ownframework) with each step, and below is a description of them:

---
## List of steps
* [Step 1 - Building an application without a framework](#step-1---building-an-application-without-a-framework-code)
* [Step 2 (Theory) - JDK Dynamic Proxy (a) vs CGLib (b)](#step-2-theory---jdk-dynamic-proxy-a-vs-cglib-b)
* [Step 2a - Dynamic Proxy](#step-2a---dynamic-proxy-code)
* [Step 2b - CGLib](#step-2b---cglib-code)
* [Step 3 - Application Context](#step-3---application-context-code)
* [Step 4 - Create proxy in `ApplicationContext`](#step-4---create-a-proxy-in-applicationcontext-code)
* [Step 5 - Implement other annotations](#step-5---implement-other-annotations-code)
* [Step 6 - Scope](#step-6---scope-code)
* [Step 7 - Refactoring](#step-7---refactoring-code)
* [Summary](#summary)

---
## Step 1 - Building an application without a framework [[code](https://github.com/Patresss/Java-Own-Framework---step-by-step/tree/main/src/main/java/com/patres/ownframework/step1)] 
* Creating the skeleton application:
  * Dao: `CompanyDao` with implementation of `CompanyDaoImpl`
  * Service: `CompanyService` with an implementation of `CompanyServiceImpl`
  * Model: `Company`

* Manual transaction management
```java
    @Override
    public void createCompany(Company company) {
        try {
            beginTransaction();

            logger.info("SERVICE:   START - create company");
            companyDao.createCompany(company);
            logger.info("SERVICE:   END - create company");

            commitTransaction();
        } catch (Exception e) {
            rollbackTransaction();
        }
    }
```

## Step 2 (theory) - JDK Dynamic Proxy (a) vs CGLib (b)

### What is a Proxy
A proxy is an intermediary that easily allows us to do something before and after a specific method:

```java
class MyClass {
    
    void method() {
        // ...
    }
}

doSmthBefore();
myClass.method();
doSmthAfter();

```

### Dynamic Proxy
* To create a proxy using Dynamic Proxy, we do not need to add anything to the project. The `Proxy` class that is responsible for creating the Proxy is found in the JDK.
* Works on an interface basis i.e. if we want to create a bean of `MyServiceImpl` class, it must implement the `MyService` interface.
* Creates `$Proxy` classes.

### CGLib
* CGLib is an external library.
* It is based on class extensions, so the proxy will not work on final methods.
* Creating instances and calling methods using CGLib is faster than Dynamic Proxy.
* It creates `MyClass$$EnhancerBySpringCGLIB` classes.

![](https://github.com/Patresss/Java-Own-Framework---step-by-step/blob/main/images/springaop-process.png)
Source: https://www.baeldung.com/spring-aop-vs-aspectj

### Default type
The default type in Spring is Dynamic Proxy.


![](https://github.com/Patresss/Java-Own-Framework---step-by-step/blob/main/images/Default%20type%20-%20Spring%20doc.JPG)
Source: https://docs.spring.io/spring-framework/docs/5.3.x/reference/html/core.html#aop-introduction-proxies

However, when we create a new application in Spring Initializr we may be surprised. Even though we add an interface we will see (in debug) a class created by CGLib `X$$EnhancerBySpringCGLIB`.

Is the documentation lying? No, the default Spring configuration uses Dynamic Proxy, but Spring Boot (2.x) configuration uses CGLib. The reason for this is: [prevent nasty proxy issues](https://github.com/spring-projects/spring-boot/issues/5423). And what is this "nasty problem"? When a developer tries to inject an implementation instead of an interface we would get an error:

```java
@Autowired
CompanyServiceImpl companyService;

// vs 

@Autowired
CompanyService companyService;

```

```java
***************************
APPLICATION FAILED TO START
***************************

Description:

The bean 'companyServiceImpl' could not be injected as a 'com.patres.framework.service.CompanyServiceImpl' because it is a JDK dynamic proxy that implements:
	com.patres.framework.service.CompanyService


Action:

Consider injecting the bean as one of its interfaces or forcing the use of CGLib-based proxies by setting proxyTargetClass=true on @EnableAsync and/or @EnableCaching.
```

More about that: https://www.programmersought.com/article/87046285018/

## Step 2a - Dynamic Proxy [[code](https://github.com/Patresss/Java-Own-Framework---step-by-step/tree/main/src/main/java/com/patres/ownframework/step2a)]
In this project, we will be using Dynamic Proxy.

### Creating an `InvocationHandler`.
The implementation of `InvocationHandler` is necessary for this. The interface has 1 method:

```java
public Object invoke(Object proxy, Method method, Object[] args)
```
It takes 3 parameters:
* proxy - The proxy instance where the method was called (We will not use it, we will act directly on the object. Otherwise, we would call an infinite recursion).
* method - The `Method` instance that is called by the proxy.
* args - The arguments passed to the method (`method`).


An example of a proxy implementation that does not do anything yet:
```java
public class ProxyHandler implements InvocationHandler {

    private final Object objectToHandle;

    public ProxyHandler(Object objectToHandle) {
        this.objectToHandle = objectToHandle;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return method.invoke(objectToHandle, args);
    }

}
```

* We add an instance of the object we want to handle to the constructor - that is, our implementation (`CompanyServiceImpl`).
* `method.invoke(objectToHandle, args)` - This does exactly what our `CompanyServiceImpl` instance would do if we called it directly. The `method` method is called from an `objectToHandle` instance with `args` arguments.

### Create a proxy
To create a proxy with a handler, use the `Proxy` class from the JDK:

```java
        final CompanyDao companyDao = new CompanyDaoImpl();
        final CompanyService companyServiceProxy = (CompanyService) Proxy.newProxyInstance(
                Step2aApp.class.getClassLoader(),
                new Class[]{CompanyService.class},
                new ProxyHandler(new CompanyServiceImpl(companyDao))
        );
```

The `newProxyInstance` method takes 3 parameters:
* loader - Class loader that will define the proxy
* interfaces - A list of interfaces
* h - Invocation handler: the class to handle this proxy (in our case `ProxyHandler`)


### Transaction handling
Suppose we want to create a proxy to help us handle transactions. Without it, we would have to begin and commit the transaction each time. To avoid code duplication, we can use a proxy to do this.


```java
public class ProxyHandler implements InvocationHandler {

    private static final Logger logger = LoggerFactory.getLogger(ProxyHandler.class);
    private final Object objectToHandle;

    public ProxyHandler(Object objectToHandle) {
        this.objectToHandle = objectToHandle;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            beginTransaction();
            final Object invoke = method.invoke(objectToHandle, args);
            commitTransaction();
            return invoke;
        } catch (Exception e) {
            rollbackTransaction();
            throw e;
        }
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
```

In the `invoke` method:
* at the beginning we open a transaction
* call the method
* commit the changes
* or revert them in case of an error

---
**Please note**

For the purposes of this project, we will not be implementing a real transaction using `EntityManager` - we will simplify this to a simple logger.

---

Done - the proxy that handles transactions is already working!
```java
2021-06-13 16:45:39,642 [main] DEBUG         ProxyHandler:32 		 - BEGIN TRANSACTION
2021-06-13 16:45:39,643 [main] INFO    CompanyServiceImpl:20 		 - SERVICE:   START - create company
2021-06-13 16:45:39,644 [main] INFO        CompanyDaoImpl:13 		 - DAO:   START - create company
2021-06-13 16:45:39,645 [main] INFO        CompanyDaoImpl:15 		 - DAO:   END - create company
2021-06-13 16:45:39,645 [main] INFO    CompanyServiceImpl:22 		 - SERVICE:   END - create company
2021-06-13 16:45:39,645 [main] DEBUG         ProxyHandler:36 		 - COMMIT TRANSACTION
```
## Step 2b - CGLib [[code](https://github.com/Patresss/Java-Own-Framework---step-by-step/tree/main/src/main/java/com/patres/ownframework/step2b)]
As I mentioned earlier, we will be using Dynamic Proxy in this project. However, I will present the creation of the proxy using CGLib as a matter of interest.

### Creating a `MethodInterceptor`.
Similar to Dynamic Proxy, we need to create a class that will manage the transaction. For this purpose, we will create a class that implements `MethodInterceptor`. It has a single method:

```java
public Object intercept(Object obj, java.lang.reflect.Method method, Object[] args,
                               MethodProxy proxy) throws Throwable;
```
With 4 arguments:
* obj - `this` from the extended class. Different from Dynamic Proxy: here we get the object we want to handle in a parameter, we do not have to add it to the constructor. This is obvious, because in case of Dynamic Proxy we act on the interface, so we do not have access to the instance.
* `method` - The captured method
* `args` - Arguments passed to the method (`method`)
* `proxy` - Used to call the parent method (`super`)


Example implementation:

```java
public class ProxyMethodInterceptor implements MethodInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(ProxyMethodInterceptor.class);

    @Override
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
        try {
            beginTransaction();
            final Object invoke = proxy.invokeSuper(obj, args);
            commitTransaction();
            return invoke;
        } catch (Exception e) {
            rollbackTransaction();
            throw e;
        }
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
```

---
**Please note**

We call the method using the parent class `invokeSuper`, otherwise we would run into infinite recursion.

---

### Creating a proxy
Creating a proxy using CGLib is done in a few steps:

```java
public class Step2bApp {

    private static final Logger logger = LoggerFactory.getLogger(Step2bApp.class);

    public static void main(String[] args) {
        /* 1 */ final Enhancer enhancer = new Enhancer();
        /* 2 */ enhancer.setSuperclass(CompanyService.class);
        /* 3 */ enhancer.setCallback(new ProxyMethodInterceptor());

        /* 4 */ CompanyService companyService = (CompanyService) enhancer.create(new Class[]{CompanyDao.class}, new Object[]{new CompanyDao()});
        companyService.createCompany(new Company());
    }
}
```
1. Initialization of the `Enhencer`.
2. Defining the class (in the case of CGLiB, the interface is not required).
3. Setting up the callback that will manage the proxy (in our case, it will manage transactions).
4. Initialization of the proxy: we need to pass as arguments an array of types and an array of instances with the specified types.

---
**Please note**

To run CGLib in Java 16, we need to add JVM option  - `--illegal-access=permit` - https://github.com/cglib/cglib/issues/191

---

## Step 3 - Application Context [[code](https://github.com/Patresss/Java-Own-Framework---step-by-step/tree/main/src/main/java/com/patres/ownframework/step3)]
We already know how the proxy works, so it is time to inject dependencies. What do we want to achieve? An Application Context that will allow us to retrieve beans using interfaces.

```java
public class Step3App {

    public static void main(String[] args) {
        final ApplicationContext applicationContext = new ApplicationContext(Step3App.class);
        final CompanyService companyServiceProxy = applicationContext.getBean(CompanyService.class);

        companyServiceProxy.createCompany(new Company());
    }
}
```
### Annotations
We need 2 annotations for this: one for defining beans and one for injecting them:
* `Autowired` - In Spring there are several ways to inject dependencies. However, it is recommended to do it through the constructor, so that is what we will set the target as well.
  ```java
  @Target(value = ElementType.CONSTRUCTOR)
  @Retention(RetentionPolicy.RUNTIME)
  public @interface Autowired {1
  }
  ```
  
* `Component` - To let our framework know which classes it should manage we will create `@Component` annotations. In the case of Spring, there are several such annotations, but for the sake of simplicity, we will create just one.
  ```java
  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  public @interface Component {
  }
  ```

### Exception
All exceptions related to the framework, we will wrap in `FrameworkException`.
```java
public class FrameworkException extends RuntimeException {

    public FrameworkException(String message) {
        super(message);
    }

    public FrameworkException(Throwable throwable) {
        super("Unknown exception", throwable);
    }

}
```

### Context
Now we come to perhaps the most difficult part of the project, which is creating the `ApplicationContext`.

At the very beginning we will create a constructor that will retrieve a `Package`. Based on it, it will search for the classes to be managed by the framework:
* Those with the `@Component` annotation.
* and they must not be interfaces, since we want to create an instance based on that class.

For this purpose we can use the library `reflections`.
```java
private final Set<Class<?>> componentBeans;

public ApplicationContext(Class<?> applicationClass) {
    final Reflections reflections = new Reflections(applicationClass.getPackage().getName());
    this.componentBeans = reflections.getTypesAnnotatedWith(Component.class).stream()
            .filter(clazz -> !clazz.isInterface())
            .collect(Collectors.toSet());
}
```
* [1] Our context will have one public method to get the bean.
* [2] We assumed that in the future we will create proxies using Dynamic Proxy, so this argument will have to be an interface.
* [3] To create an instance, we must first look for a suitable implementation. As with Spring, we cannot have more than one implementation of an interface, because the framework would not know which one to use (`NoUniqueBeanDefinitionException` - without `@Qualifier`).
* [4] Knowing the implementation, we can create a new instance.
* [5] The first step, will be to look for a constructor. As in Spring: if we have only one constructor, it is easy. In case we have more constructors, we look for the one with the `@Autowired` annotation.
* [6] However, the constructor itself is not enough. Now we have to look for its arguments. These arguments are the other beans that need to be retrieved from [1] using recursion.
* [7] With the valid constructor and parameters, we can finally create a new instance.

```java

/* 1 */
public <T> T getBean(Class<T> clazz) {
  /* 2 */
  if (!clazz.isInterface()) {
    throw new FrameworkException("Class " + clazz.getName() + " should be an interface");
  }

  /* 3 */
  final Class<T> implementation = findImplementationByInterface(clazz);

  /* 4 */
  return createBean(implementation);
}

@SuppressWarnings("unchecked")
/* 3 */
private <T> Class<T> findImplementationByInterface(Class<T> interfaceItem) {
  final Set<Class<?>> classesWithInterfaces = componentBeans.stream()
          .filter(componentBean -> List.of(componentBean.getInterfaces()).contains(interfaceItem))
          .collect(Collectors.toSet());

  if (classesWithInterfaces.size() > 1) {
    throw new FrameworkException("There are more than 1 implementation: " + interfaceItem.getName());
  }

  return (Class<T>) classesWithInterfaces.stream()
          .findFirst()
          .orElseThrow(() -> new FrameworkException("The is no class with interface: " + interfaceItem));
}

/* 4 */
private <T> T createBean(Class<T> implementation) {
  try {
    /* 5 */
    final Constructor<T> constructor = findConstructor(implementation);

    /* 6 */
    final Object[] parameters = findConstructorParameters(constructor);

    /* 7 */
    return constructor.newInstance(parameters);
  } catch (FrameworkException e) {
    throw e;
  } catch (Exception e) {
    throw new FrameworkException(e);
  }
}

@SuppressWarnings("unchecked")
/* 5 */
private <T> Constructor<T> findConstructor(Class<T> clazz) {
  final Constructor<T>[] constructors = (Constructor<T>[]) clazz.getConstructors();
  if (constructors.length == 1) {
    return constructors[0];
  }

  final Set<Constructor<T>> constructorsWithAnnotation = Arrays.stream(constructors)
          .filter(constructor -> constructor.isAnnotationPresent(Autowired.class))
          .collect(Collectors.toSet());

  if (constructorsWithAnnotation.size() > 1) {
    throw new FrameworkException("There are more than 1 constructor with Autowired annotation: " + clazz.getName());
  }

  return constructorsWithAnnotation.stream()
          .findFirst()
          .orElseThrow(() -> new FrameworkException("Cannot find constructor with annotation Autowired: " + clazz.getName()));
}

/* 6 */
private <T> Object[] findConstructorParameters(Constructor<T> constructor) {
  final Class<?>[] parameterTypes = constructor.getParameterTypes();
  return Arrays.stream(parameterTypes)
          .map(this::getBean)
          .toArray(Object[]::new);
}

```
## Step 4 - Create a proxy in `ApplicationContext` [[code](https://github.com/Patresss/Java-Own-Framework---step-by-step/tree/main/src/main/java/com/patres/ownframework/step4)]
As you may have already noticed, we are not creating any proxy in `ApplicationContext`. Therefore, it is now time to combine step 3 and 4:

### Proxy
Instead of directly returning a new instance, we can wrap it in a proxy. For this we will need an interface, which we can pass from the `getBean` method
```java
    private <T> T createBean(Class<T> clazz, Class<T> implementation) {
        try {
            final Constructor<T> constructor = findConstructor(implementation);
            final Object[] parameters = findConstructorParameters(constructor);
            final T bean = constructor.newInstance(parameters);

            final Object proxy = Proxy.newProxyInstance(
                    ApplicationContext.class.getClassLoader(),
                    new Class[]{clazz},
                    new ProxyHandler(bean));
            return clazz.cast(proxy);
        } catch (FrameworkException e) {
            throw e;
        } catch (Exception e) {
            throw new FrameworkException(e);
        }
    }
```
### Transactional
We have wrapped each class with the `ProxyHandler` created earlier. This means that every method from `ApplicationContext` will run in a transaction, and we do not want that. To avoid this, we can create a new annotation:

```java
@Target(value = ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Transactional {
}
```

All that remains is handle it in `ProxyHandler`. Before calling a method in a transaction, we must first check that it has the `@Transactional` annotation:

```java
@Override
public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    if (isTransactional(method)) {
        return handleTransaction(method, args);
    }
    return method.invoke(objectToHandle, args);
}
```

```java
private boolean isTransactional(Method method) {
    try {
        return objectToHandle.getClass().getMethod(method.getName(), method.getParameterTypes()).isAnnotationPresent(Transactional.class);
    } catch (NoSuchMethodException e) {
        return false;
    }
}
```

---
**Please note**

We do not check if the method from the argument has a transaction:
```java
private boolean isTransactional(Method method) {
    return method.isAnnotationPresent(Transactional.class);
    }
```

Since the `method` instance refers to a method from the interface, and `@Transactional` we want to add in the methods of the implemented class, so we retrieve it from `objectToHandle`.

---

### Why do annotations (e.g. `@Transactional`) sometimes not work?
For annotations to work, they must go through a proxy. Which means that methods with a specific annotation must be public and cannot be called in the same bean. In the case of CGLib, the method cannot be final because it must be overridden.

The following code will not work (both in our project and in Spring) because the `createWithTransaction` method is called directly in the same class, so it will not go through the proxy.

```java
@Override
public void createCompany(Company company) {
    logger.info("SERVICE:   START - create company");
    createWithTransaction(company);
    logger.info("SERVICE:   END - create company");
}

@Transactional
public void createWithTransaction(Company company) {
    logger.info("SERVICE:   START - createWithTransaction");
    companyDao.createCompany(company);
    logger.info("SERVICE:   END - createWithTransaction");
}
```


## Step 5 - Implement other annotations [[code](https://github.com/Patresss/Java-Own-Framework---step-by-step/tree/main/src/main/java/com/patres/ownframework/step5)]
Creating `@Transactional` was just an example. Our own framework may have many other useful annotations. Therefore, in this step we will try to implement the `@Cacheable` annotation.

```java
@Target(value = ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Cacheable {
}
```
Usage example: suppose that if we have already generated a token for a given `Company` once, we can use it again - without generating it again.
```java
@Override
@Cacheable
public String generateToken(Company company) {
    return UUID.randomUUID().toString();
}
```
To do this, we need to modify our `ProxyHandler`. Let's start by adding a map that will store the method and argument list as the key and the method result as the value.

```java
private final Map<List<Object>, Object> cacheContainer = new HashMap<>();


private List<Object> createCacheKey(Method method, Object[] args) {
    return List.of(method, Arrays.asList(args));
}
```
Next, as with `@Transactional` we will create a function that determines whether a method is `@Cacheable`:

```java
private boolean isCacheable(Method method) {
    try {
        return objectToHandle.getClass().getMethod(method.getName(), method.getParameterTypes()).isAnnotationPresent(Cacheable.class);
    } catch (NoSuchMethodException e) {
        return false;
    }
}
```
Now we just need to check if the method has already been called with the same arguments. If so, we retrieve the value from the map without calling the method.
```java
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
```

And add the result to the above map.
```java
private Object calculateResult(Method method, Object[] args) throws IllegalAccessException, InvocationTargetException {
    final Object result = method.invoke(objectToHandle, args);
    if (isCacheable(method)) {
        cacheContainer.put(createCacheKey(method, args), result);
    }
    return result;
}
```

Done - the `@Cacheable` annotation has been implemented!

## Step 6 - Scope [[code](https://github.com/Patresss/Java-Own-Framework---step-by-step/tree/main/src/main/java/com/patres/ownframework/step6)]
When we implemented the `ProxyHandler` we created a new bean every time we called the `getBean` method. However, following Spring's example, we will implement one more Scope - `SINGLETON` and make it the default.

```java
public enum Scope {

    SINGLETON, PROTOTYPE

}

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Component {

  Scope scope() default Scope.SINGLETON;
}
```

With this annotation, we are ready to modify the `ApplicationContext` class. Let's start by adding a map that will store our singletons.

```java
private final Map<Class<?>, Object> singletonBeans = new ConcurrentHashMap<>();
```
If `scope` is equal to `SINGLETON`:
* and the class exists in the map then we return the already created bean
* otherwise we create the bean and add it to the map.


```java
public <T> T getBean(Class<T> clazz) {
    if (!clazz.isInterface()) {
        throw new FrameworkException("Class " + clazz.getName() + " should be an interface");
    }
    final Class<T> implementation = findImplementationByInterface(clazz);
    
    final Component annotation = implementation.getAnnotation(Component.class);
    if (annotation.scope() == Scope.SINGLETON) {
        return (T) singletonBeans.computeIfAbsent(clazz, it -> createBean(clazz, implementation));
    }
    return createBean(clazz, implementation);
}
```

## Step 7 - Refactoring [[code](https://github.com/Patresss/Java-Own-Framework---step-by-step/tree/main/src/main/java/com/patres/ownframework/step7)]
Our framework has been implemented. We can now refactor it. We added `@Cacheable` to `ProxyHandler` and it got messy. Let's move the transaction and cache related methods into separate classes:
```java
public abstract class AbstractProxyHandler {

  private static final Logger logger = LoggerFactory.getLogger(AbstractProxyHandler.class);

  private final Object objectToHandle;
  private final Class<? extends Annotation> annotation;

  public AbstractProxyHandler(final Object objectToHandle, final Class<? extends Annotation> annotation) {
    this.objectToHandle = objectToHandle;
    this.annotation = annotation;
  }

  public boolean isSupported(final Method method) {
    try {
      return objectToHandle.getClass().getMethod(method.getName(), method.getParameterTypes()).isAnnotationPresent(annotation);
    } catch (NoSuchMethodException e) {
      logger.error("Method is not supported", e);
      return false;
    }
  }

}

public class TransactionalHandler extends AbstractProxyHandler {

    private static final Logger logger = LoggerFactory.getLogger(TransactionalHandler.class);

    public TransactionalHandler(final Object objectToHandle) {
        super(objectToHandle, Transactional.class);
    }

    public Object executeWithTransaction(final Supplier<Object> resultSupplier) {
        beginTransaction();
        try {
            Object result = resultSupplier.get();
            commitTransaction();
            return result;
        } catch (Exception e) {
            rollbackTransaction();
            throw e;
        }
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

public class CacheableHandler extends AbstractProxyHandler {

    private final Map<List<Object>, Object> cacheContainers = new ConcurrentHashMap<>();

    public CacheableHandler(final Object objectToHandle) {
        super(objectToHandle, Cacheable.class);
    }

    public List<Object> createKeyCache(final Method method, final Object[] args) {
        return List.of(method, Arrays.asList(args));
    }

    public Object takeResultOrCalculate(final Method method, Object[] args, final Supplier<Object> resultSupplier) {
        final List<Object> keyCache = createKeyCache(method, args);
        return cacheContainers.computeIfAbsent(keyCache, key -> resultSupplier.get());
    }

}
```

We can now slim down the `ProxyHandler` class and rename it:

```java
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
            return cacheHandler.takeResultOrCalculate(method, args, () -> calculateResult(method, args));
        }
        return calculateResult(method, args);
    }

    private Object calculateResult(final Method method, final Object[] args) {
        if (transactionHandler.isSupported(method)) {
            return transactionHandler.executeWithTransaction(() -> invokeMethod(method, args));
        }
        return invokeMethod(method, args);
    }

    private Object invokeMethod(final Method method, final Object[] args) {
        try {
            return method.invoke(objectToHandle, args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new FrameworkException(e);
        }
    }

}

```

## Summary
Done, our framework has been implemented! It is still missing a lot of functionality (transaction propagation, `@Bean`, `@Qualifier`, `@Configuration`, remaining scopes and many other functionalities). However, the idea of the framework has been demonstrated. The project showed:
* How to implement dependency injection.
* What a proxy is
* How to create and use your own annotations
* What and why is needed to make annotations work

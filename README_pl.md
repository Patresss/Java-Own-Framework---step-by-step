# Java Own Framework - krok po kroku

Celem tego projektu jest pokazanie, w jaki sposób działa dependency injection framework np. Spring.

Repozytorium zademonstruje krok po kroku, w jaki sposób zbudować własny framework. Oczywiście jest to tylko uproszczona forma.
Spring jest rozwijany od prawie 20 lat przez setki programistów, więc ledwo zbliżymy się do niego. Jednak repozytorium
pokaże koncept takiego frameworka i udowodni, że nie kryje się tam żadna magia.

### Filmy:
* The Magic of Spring • Patryk Piechaczek • Devoxx Poland 2021 - https://www.youtube.com/watch?v=5gttHK04lQ4
* JDD 2022: Patryk Piechaczek - The Magic of Spring - how does it really work? - https://www.youtube.com/watch?v=p_zTjkos8p0
* [PL] Bydgoszcz JUG - Meetup #39 - https://www.youtube.com/live/pyA6-tVg2yI?feature=share&t=3719

W repozytorium znajdziecie [pakiety](https://github.com/Patresss/Java-Own-Framework---step-by-step/tree/main/src/main/java/com/patres/ownframework) z poszczególnymi krokami, a poniżej jest ich omówienie:

---
## Spis kroków
* [Krok 1 - Budowa aplikacji bez frameworka](#krok-1---budowa-aplikacji-bez-frameworka-kod)
* [Krok 2 (teoria) - JDK Dynamic Proxy (a) vs CGLib (b)](#krok-2-teoria---jdk-dynamic-proxy-a-vs-cglib-b)
* [Krok 2a - Dynamic Proxy](#krok-2a---dynamic-proxy-kod)
* [Krok 2b - CGLib](#krok-2b---cglib-kod)
* [Krok 3 - Application Context](#krok-3---application-context-kod)
* [Krok 4 - Tworzenie proxy w `ApplicationContext`](#krok-4---tworzenie-proxy-w-applicationcontext-kod)
* [Krok 5 - Implementacja innych adnotacji](#krok-5---implementacja-innych-adnotacji-kod)
* [Krok 6 - Scope](#krok-6---scope-kod)
* [Krok 7 - Refactoring](#krok-7---refactoring-kod)
* [Zakończenie](#zakończenie)
---

## Krok 1 - Budowa aplikacji bez frameworka [[kod](https://github.com/Patresss/Java-Own-Framework---step-by-step/tree/main/src/main/java/com/patres/ownframework/step1)]
* Stworzenie szkieletu aplikacji
  * Dao: `CompanyDao` z implementacją `CompanyDaoImpl`
  * Service: `CompanyService` z implementacją `CompanyServiceImpl`
  * Modelu `Company`

* Manualne zarządzanie transakcją
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

## Krok 2 (teoria) - JDK Dynamic Proxy (a) vs CGLib (b)

### Czym jest Proxy
Proxy jest pośrednikiem, który w łatwy sposób pozwala nam na wykonanie czegoś przed i po konkretnej metodzie:

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
* Do stworzenia proxy przy pomocy Dynamic Proxy, nie potrzebujemy niczego dodawać do projektu. `Proxy`- klasa, która odpowiada za tworzenie Proxy znajduje się w JDK.
* Działa na podstawie interfejsu tzn. jeżeli chcemy stworzyć bean klasy `MyServiceImpl`, musi ona implementować interfejs `MyService`.
* Tworzy klasy `$Proxy`.

### CGLib
* CGLib jest zewnętrzną biblioteką.
* Bazuje ona na rozszerzaniu klas, więc proxy nie będzie działało na metodach finalnych.
* Tworzenie instancji oraz wywoływanie metod przy użyciu CGLib jest szybsze od Dynamic Proxy.
* Tworzy klasy `MyClass$$EnhancerBySpringCGLIB`.

![](https://github.com/Patresss/Java-Own-Framework---step-by-step/blob/main/images/springaop-process.png)
Żródło: https://www.baeldung.com/spring-aop-vs-aspectj

### Domyślny typ
Domyślnym typem w Springu jest Dynamic Proxy.


![](https://github.com/Patresss/Java-Own-Framework---step-by-step/blob/main/images/Default%20type%20-%20Spring%20doc.JPG)
Żródło: https://docs.spring.io/spring-framework/docs/5.3.x/reference/html/core.html#aop-introduction-proxies

Jednak gdy stworzymy nową aplikację w Spring Initializr to możemy się zdziwić. Pomimo tego, że dodamy interfejs to w debugu zobaczymy klasę stworzoną przez CGLib `X$$EnhancerBySpringCGLIB`

Czy dokumentacja kłamie? Nie, domyślna konfiguracja Springa używa Dynamic Proxy, ale już domyśla konfiguracja Spring Boot (2.x) używa CGLib. Powodem tego jest: [prevent nasty proxy issues](https://github.com/spring-projects/spring-boot/issues/5423). A czym jest ten "paskudny problem"? Gdy programista spróbuje wstrzyknąć implementację zamiast interfejsu otrzymałby bład:
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

Więcej na ten temat: https://www.programmersought.com/article/87046285018/

## Krok 2a - Dynamic Proxy [[kod](https://github.com/Patresss/Java-Own-Framework---step-by-step/tree/main/src/main/java/com/patres/ownframework/step2a)]
W tym projekcie będziemy używać Dynamic Proxy. 

### Tworzenie `InvocationHandler`
Niezbędna do tego jest implementacja `InvocationHandler`. Interfejs posiada 1 metodę:
```java
public Object invoke(Object proxy, Method method, Object[] args)
```
Przyjmuje on 3 parametry:
* proxy — Instancja proxy, w której została wywołana metoda (nie będziemy jej używać, będziemy działali bezpośrednio na obiekcie. W przeciwnym wypadku wywołalibyśmy nieskonczoną rekurencję).
* method — Instancja `Method`, która jest wywoływana przez proxy.
* args — Argmenty przekazywane do metody (`method`).


Przykład implementacji proxy, która jeszcze nic nie robi:
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

* Do konstruktora dodajemy instancje obiektu, którą chcemy obsłużyć — czyli naszą implementację (`CompanyServiceImpl`).
* `method.invoke(objectToHandle, args)` — Robi dokładnie to samo co robiłaby nasza instancja `CompanyServiceImpl`, gdybyśmy ją wywoływali bezpośrednio. Metoda `method` jest wywoływana z instancji `objectToHandle` z argumentami `args`.

### Tworzenie proxy
Aby stworzyć proxy z handlerem, należy użyć klasę `Proxy` z JDK:
```java
        final CompanyDao companyDao = new CompanyDaoImpl();
        final CompanyService companyServiceProxy = (CompanyService) Proxy.newProxyInstance(
                Step2aApp.class.getClassLoader(),
                new Class[]{CompanyService.class},
                new ProxyHandler(new CompanyServiceImpl(companyDao))
        );
```
Metoda `newProxyInstance` przyjmuje 3 parametry:
* loader — Class loader, który zdefiniuje proxy
* interfaces — Listę interfejsów 
* h — Invocation handler: klasa, która ma obsłużyć to proxy (w naszym przypadku `ProxyHandler`)


### Obsługa transakcji
Załóżmy, że chcemy stworzyć proxy, aby pomagał nam w obsłudze transakcji. Bez tego musielibyśmy za każdym razem otwierać i zatwierdzać transakcję. Aby uniknąć duplikacji kodu, możemy do tego wykorzystać proxy.

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

W metodzie `invoke`:
* na początku otwieramy transakcję 
* wywołujemy metodę, która nas interesuje
* zatwierdzamy zmiany
* lub je wycofujemy w przypadku błędu

---
**Zwróć uwagę**

Na potrzeby tego projektu, nie będziemy implementować prawdziwej transakcji przy użyciu EntityManager — uprościmy to do zwykłego loggera.

---

Gotowe — proxy, które obsługuje transakcje już działa!
```java
2021-06-13 16:45:39,642 [main] DEBUG         ProxyHandler:32 		 - BEGIN TRANSACTION
2021-06-13 16:45:39,643 [main] INFO    CompanyServiceImpl:20 		 - SERVICE:   START - create company
2021-06-13 16:45:39,644 [main] INFO        CompanyDaoImpl:13 		 - DAO:   START - create company
2021-06-13 16:45:39,645 [main] INFO        CompanyDaoImpl:15 		 - DAO:   END - create company
2021-06-13 16:45:39,645 [main] INFO    CompanyServiceImpl:22 		 - SERVICE:   END - create company
2021-06-13 16:45:39,645 [main] DEBUG         ProxyHandler:36 		 - COMMIT TRANSACTION
```

## Krok 2b - CGLib [[kod](https://github.com/Patresss/Java-Own-Framework---step-by-step/tree/main/src/main/java/com/patres/ownframework/step2b)]
Jak już wspomniałem, w tym projekcie będziemy używać Dynamic Proxy. Jednak przedstawię tworzenie proxy przy pomocy CGLib w ramach ciekawostki.

### Tworzenie `MethodInterceptor`
Podobnie jak w przypadku Dynamic Proxy musimy stworzyć klasę, która będzie zarządzała transakcją. W tym celu stworzymy klasę, którą implementuje `MethodInterceptor`. Posiada ona jedną metodę:
```java
public Object intercept(Object obj, java.lang.reflect.Method method, Object[] args,
                               MethodProxy proxy) throws Throwable;
```
z 4 argumentami:
* obj - `this` z rozszerzonej klasy. Inaczej niż w przypadku Dynamic Proxy: tutaj obiekt, którego chcemy obsłużyć dostajemy w parametrze, nie musimy go dodawać do konstruktora. Jest to oczywiste, bo w przypadku Dynamic Proxy działamy na interfejsie, więc nie mamy dostępu do instancji.
* method – przechwycona metoda.
* args – Argumenty przekazywane do metody (`method`)
* proxy – Służy do wywołania nadrzędnej metody (`super`)


Przykładowa implementacja:
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
**Zwróć uwagę**

Metodę wywołujemy za pomocą klasy nadrzędnej `invokeSuper`, w przeciwnym wypadku wpadlibyśmy w nieskończoną rekurencję.

---

### Tworzenie proxy
Tworzenie proxy przy pomocy CGLib odbywa się w kilku krokach:
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
1. Inicjalizacja `Enhencer`.
2. Definiowanie klasy (w przypadku CGLiB interfejs nie jest wymagany).
3. Ustawienie Collbacka, który będzie zarządzała proxy (w naszym przypadku, będzie zarządzała transakcjami).
6. Inicjalizacja proxy: jako argumentu musimy podać tablicę typów i tablicę instancji o określonych typach.

---
**Zwróć uwagę**

Aby uruchomić CGLib w Java 16, musimy dodać JVM option - `--illegal-access=permit` - https://github.com/cglib/cglib/issues/191

---

## Krok 3 - Application Context [[kod](https://github.com/Patresss/Java-Own-Framework---step-by-step/tree/main/src/main/java/com/patres/ownframework/step3)]
Wiemy już jak działa proxy, więc czas na wstrzykiwanie zależności. Co chcemy osiągnąć? Application Context, który pozwoli nam na pobieranie beanów przy pomocy interfejsów. 
```java
public class Step3App {

    public static void main(String[] args) {
        final ApplicationContext applicationContext = new ApplicationContext(Step3App.class);
        final CompanyService companyServiceProxy = applicationContext.getBean(CompanyService.class);

        companyServiceProxy.createCompany(new Company());
    }
}
```
### Adnotacje
Potrzebujemy do tego 2 adnotacji: jedną do definiowania beanów oraz drugą do ich wstrzykiwania:
* `Autowired` - W Springu istnieje kilka sposobów na wstrzyknięcie zależności. Jednak zalecane jest, aby to robić przez konstruktor, dlatego taki też ustawimy target.
  ```java
  @Target(value = ElementType.CONSTRUCTOR)
  @Retention(RetentionPolicy.RUNTIME)
  public @interface Autowired {
  }
  ```

* `Component` - Aby dać znać naszemu frameworkowi, które klasy mają być przez niego zarządzane stworzymy adnotacje `@Component`. W przypadku Springa jest kilka takich adnotacji, jednak dla uproszenia stworzymy tylko jedną.
  ```java
  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  public @interface Component {
  }
  ```

### Wyjątek
Wszystkie wyjątki związane z frameworkiem, będziemy opakowywać w `FrameworkException`
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
Teraz przejdziemy chyba do najtrudniejszej części projektu, czyli do tworzenia `ApplicationContext`

Na samym początku stworzymy konstruktor, który będzie pobierał `Package`. Na jego podstawie będzie wyszukiwać klasy, które mają być zarządzane przez framework, czyli:
* te z adnotacją `@Component`
* oraz nie mogą to być interfejse, ponieważ chcemy stworzyć instancję na podstawie tej klasy.

W tym celu możemy użyć bibliotekę `reflections`
```java
private final Set<Class<?>> componentBeans;

public ApplicationContext(Class<?> applicationClass) {
    final Reflections reflections = new Reflections(applicationClass.getPackage().getName());
    this.componentBeans = reflections.getTypesAnnotatedWith(Component.class).stream()
            .filter(clazz -> !clazz.isInterface())
            .collect(Collectors.toSet());
}
```

* [1] Nasz context będzie posiadał jedną publiczną metodę do wyciągania beana. 
* [2] Założyliśmy, że w przyszłości będziemy tworzyć proxy za pomocą Dynamic Proxy, dlatego ten argument będzie musiał być interfejsem. 
* [3] Aby utworzyć instancję, musimy najpierw poszukać odpowiedniej implementacji. Podobnie jak w przypadku Springa, nie możemy posiadać więcej niż jedną implementację interfejsu, ponieważ framework nie wiedziałby, której miałby użyć (`NoUniqueBeanDefinitionException` - bez `@Qualifier`.
* [4] Znając implementację, możemy brać się za tworzenie nowej instancji.
* [5] Pierwszym krokiem, będzie poszukanie konstruktora. Podobnie jak w przypadku Springa: jeżeli mamy tylko jeden konstruktor to sprawa jest prosta. W przypadku gdy tych konstruktorów mamy więcej, szukamy tego z adnotacją `@Autowired`.
* [6] Jednak sam konstruktor to nie wszystko. Teraz musimy poszukać jego argumentów. A tymi argumentami są pozostałe beany, które należy pobrać z [1] używając rekurencji.
* [7] Mając odpowiedni konstruktor i parametry możemy w końcu stworzyć nową instancję.

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

## Krok 4 - Tworzenie proxy w `ApplicationContext` [[kod](https://github.com/Patresss/Java-Own-Framework---step-by-step/tree/main/src/main/java/com/patres/ownframework/step4)]
Jak już pewnie zauważyłeś, w `ApplicationContext` nie tworzymy żadnego proxy. Dlatego teraz czas połączyć krok 3 i 4:

### Proxy
Zamiast bezpośrednio zwracać nową instancję, możemy ją opakować w proxy. Potrzebny do tego będzie interfejs, który możemy przekazać z metody `getBean`
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
Teraz opakowaliśmy każdą klasę stworzonym wcześniej `ProxyHandler`. Oznacza to, że każda metoda z `ApplicationContext` będzie uruchomiana w transakcji, a tego nie chcemy. Aby tego uniknąć, możemy stworzyć nową adnotację:
```java
@Target(value = ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Transactional {
}
```

Pozostaje nam ją tylko obsłużyć w `ProxyHandler`. Przed wywołaniem metody w transakcji, musimy najpierw sprawdzić, czy ma ona adnotację `@Transactional`:

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
**Zwróć uwagę**

Nie sprawdzamy, czy metoda z argumentu ma transakcję:
```java
private boolean isTransactional(Method method) {
    return method.isAnnotationPresent(Transactional.class);
    }
```

Ponieważ instancja `method` dotyczy metody z interfejsu, a `@Transactional` chcemy dodawać w metodach zaimplementowanej klasy, dlatego pobieramy ja z `objectToHandle`.

---

### Dlaczego adnotacje (np. `@Transactional`) czasami nie działają?
Aby adnotacje działały, muszą przejść przez proxy. Co oznacza, że metody z konkretną adnotacją musi być publiczna i nie może być wywołana w tym samym beanie. W przypadku CGLib metoda nie może być finalna, ponieważ musi zostać ona nadpisana. 

Poniższy kod nie zadziała (zarówno u nas jak i w Springu), ponieważ metoda `createWithTransaction` jest wywoływana bezpośrednio w tej samej klasie, czyli nie przejdzie przez proxy.

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

## Krok 5 - Implementacja innych adnotacji [[kod](https://github.com/Patresss/Java-Own-Framework---step-by-step/tree/main/src/main/java/com/patres/ownframework/step5)]
Stworzenie `@Transactional` było jedynie przykładem. Nasz własny framework może mieć wiele innych użytecznych adnotacji. Dlatego w tym kroku postaramy się zaimplementować adnotację `@Cacheable`.

```java
@Target(value = ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Cacheable {
}
```
Przykład użycia: Załóżmy, że jeżeli już raz wygenerowaliśmy token dla danej `Company`, to możemy użyć go ponownie — bez generowania go jeszcze raz.
```java
@Override
@Cacheable
public String generateToken(Company company) {
    return UUID.randomUUID().toString();
}
```

Aby to zrobić, musimy nieco zmodyfikować nasz `ProxyHandler`. Zacznijmy od dodania mapy, która będzie przechowywała metodę i listę argumentów jako klucz oraz wynik metody jako wartość.

```java
private final Map<List<Object>, Object> cacheContainer = new HashMap<>();


private List<Object> createCacheKey(Method method, Object[] args) {
    return List.of(method, Arrays.asList(args));
}
```

Następnie podobnie jak w przypadku `@Transactional` stworzymy funkcję, która determinuje to, czy metoda jest `@Cacheable`:
```java
private boolean isCacheable(Method method) {
    try {
        return objectToHandle.getClass().getMethod(method.getName(), method.getParameterTypes()).isAnnotationPresent(Cacheable.class);
    } catch (NoSuchMethodException e) {
        return false;
    }
}
```

Teraz wystarczy sprawdzić, czy metoda już była wywołana z tymi samymi argumentami. Jeżeli tak, to pobieramy wartość z mapy bez wywoływania metody.
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

Oraz dodać wynik do wyżej wymienionej mapy.
```java
private Object calculateResult(Method method, Object[] args) throws IllegalAccessException, InvocationTargetException {
    final Object result = method.invoke(objectToHandle, args);
    if (isCacheable(method)) {
        cacheContainer.put(createCacheKey(method, args), result);
    }
    return result;
}
```

Gotowe — adnotacja `@Cacheable` została zaimplementowana!


## Krok 6 - Scope [[kod](https://github.com/Patresss/Java-Own-Framework---step-by-step/tree/main/src/main/java/com/patres/ownframework/step6)]
Gdy implementowaliśmy `ProxyHandler` to za każdym razem, gdy wywoływaliśmy metodę `getBean` tworzyliśmy nowy bean. Jednak wzorem Springa zaimplementujemy jeszcze jeden Scope - `SINGLETON` i uczynimy go domyślnym.

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

Mając gotową adnotację, jesteśmy gotowi, aby zmodyfikować klasę `ApplicationContext`. Zacznijmy od dodania mapy, która będzie przechowywała nasze singletony.

```java
private final Map<Class<?>, Object> singletonBeans = new ConcurrentHashMap<>();
```
Jeżeli `scope` jest równy `SINGLETON`:
* oraz klasa istnieje w mapie to zwracamy stworzony już wcześniej bean
* w przeciwnym wypadku tworzymy beana oraz dodajemy go do mapy.

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

## Krok 7 - Refactoring [[kod](https://github.com/Patresss/Java-Own-Framework---step-by-step/tree/main/src/main/java/com/patres/ownframework/step7)]
Nasz framework został zaimplementowany. Możemy go teraz zrefactoryzować. Do `ProxyHandler` dodaliśmy `@Cacheable` i zrobił się bałagan. Przenieśmy metody związane z transakcjami i cache do osobnych klas:
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

Klasę `ProxyHandler` możemy teraz odchudzić i zmienić jej nazwę:
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

## Zakończenie
Gotowe, nasz framework został zaimplementowany! Brakuje w nim jeszcze sporo funkcjonalności (propagacje transakcji, `@Bean`, `@Qualifier`, `@Configuration`, pozostałe scopy i wiele innych funkcjonalności). Jednak sama idea frameworka została pokazana. Projekt pokazał:
* Jak zaimplementować dependency injection
* Czym jest proxy
* Jak stworzyć i użyć własnych adnotacje
* Co i dlaczego jest potrzebne, aby adnotacje działały

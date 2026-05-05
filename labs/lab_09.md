# Лабораторна робота №9
## Створення програм з кількома підпроцесами

---

### Тема

Створення програм з кількома підпроцесами (багатопотокове програмування): потоки, синхронізація та паралельне виконання у Java.

---

### Мета

Набути практичних навичок розробки багатопотокових застосунків мовою Java шляхом реалізації потоків виконання, механізмів синхронізації та паралельних обчислень з використанням `ExecutorService`, `synchronized`, `volatile` та `CompletableFuture`.

---

### Теоретичні відомості

#### Потоки виконання (Threads)

Потік (*thread*) — найменша одиниця виконання у межах процесу. У Java кожен потік є екземпляром класу `java.lang.Thread`. Два способи створення потоку:

```java
// Спосіб 1: успадкування Thread
class CounterThread extends Thread {
    private final int id;
    public CounterThread(int id) { this.id = id; }

    @Override
    public void run() {
        for (int i = 0; i < 5; i++) {
            System.out.println("Потік " + id + ": " + i);
        }
    }
}

// Спосіб 2: реалізація Runnable (перевагається)
Thread t = new Thread(() -> System.out.println("Лямбда-потік!"));
t.start();

// Запуск
new CounterThread(1).start();
```

**Важливо:** `thread.run()` виконує метод у поточному потоці. `thread.start()` створює новий системний потік і запускає в ньому `run()`.

#### Стани потоку

Потік знаходиться в одному з станів: `NEW` → `RUNNABLE` → (`BLOCKED` | `WAITING` | `TIMED_WAITING`) → `TERMINATED`.

#### `ExecutorService` — пул потоків

Ручне керування потоками ненадійне. Рекомендується використовувати пул:

```java
import java.util.concurrent.*;

// Фіксований пул з 4 потоками
ExecutorService executor = Executors.newFixedThreadPool(4);

for (int i = 0; i < 10; i++) {
    final int taskId = i;
    executor.submit(() -> System.out.println("Задача " + taskId));
}

executor.shutdown();                        // зупинка після завершення задач
executor.awaitTermination(30, TimeUnit.SECONDS);
```

#### `Callable` та `Future`

```java
Callable<Double> task = () -> {
    Thread.sleep(1000);
    return Math.PI;
};

Future<Double> future = executor.submit(task);
System.out.println("Обчислення... ");
double result = future.get(); // блокує до отримання результату
System.out.println("PI ~ " + result);
```

#### `CompletableFuture` (Java 8+)

```java
CompletableFuture.supplyAsync(() -> fetchTemperature())
    .thenApply(temp -> "Температура: " + temp + "°C")
    .thenAccept(System.out::println)
    .exceptionally(ex -> { System.err.println(ex.getMessage()); return null; })
    .join();
```

#### Синхронізація: `synchronized`

При одночасному доступі кількох потоків до спільних даних виникають **гонки даних** (*data races*). Для захисту використовується `synchronized`:

```java
public class SafeCounter {
    private int count = 0;

    // Синхронізований метод — захищає весь метод
    public synchronized void increment() { count++; }

    // Синхронізований блок — мінімальна критична секція
    public void decrement() {
        synchronized (this) { count--; }
    }

    public synchronized int get() { return count; }
}
```

#### `volatile`

`volatile` гарантує **видимість** змін між потоками (але не атомарність складних операцій):

```java
volatile boolean running = true;

// Потік, що перевіряє прапорець зупинки
while (running) { /* виконання */ }

// Інший потік
running = false; // з volatile — гарантовано побачить перший потік
```

#### Атомарні класи

```java
import java.util.concurrent.atomic.*;

AtomicInteger counter = new AtomicInteger(0);
counter.incrementAndGet();                  // атомарний інкремент
counter.compareAndSet(5, 10);               // порівняти і замінити
```

#### Блокування: `Lock` та `ReentrantLock`

```java
import java.util.concurrent.locks.*;

Lock lock = new ReentrantLock();
lock.lock();
try {
    // критична секція
} finally {
    lock.unlock(); // завжди у finally
}
```

#### Віртуальні потоки (Virtual Threads, Java 21+)

```java
// Надлегкі потоки керовані JVM (не ОС)
Thread vt = Thread.ofVirtual().start(() -> System.out.println("Віртуальний потік"));
ExecutorService vtExecutor = Executors.newVirtualThreadPerTaskExecutor();
```

Віртуальні потоки дозволяють мати мільйони одночасних «потоків» без перевантаження ОС.

#### Приклад з репозиторію java-iot

Клас [`ReactionEngine.java`](https://github.com/AndriyLutskiv/java-iot/blob/main/projects/p06-reaction/src/main/java/ua/crowpi/projects/p06/ReactionEngine.java) координує роботу ігрових раундів через час та стимули у кількох потоках. Клас [`GracefulShutdown.java`](https://github.com/AndriyLutskiv/java-iot/blob/main/core/src/main/java/ua/crowpi/core/util/GracefulShutdown.java) демонструє коректне завершення роботи через shutdown hook та зупинку потоків. Рекомендується проаналізувати ці класи.

---

### Хід виконання роботи

1. Відкрити та проаналізувати `ReactionEngine.java` та `GracefulShutdown.java` (посилання вище). Визначити: які механізми синхронізації використано, як керується зупинка потоків.
2. Реалізувати два потоки, що паралельно збільшують спільний лічильник (без синхронізації). Запустити 1000 разів та зафіксувати ненульову ймовірність гонки.
3. Виправити гонку через `synchronized`, потім через `AtomicInteger`. Порівняти коректність та продуктивність.
4. Реалізувати пул потоків для паралельного виконання задач відповідно до варіанту.
5. Реалізувати `Callable`-задачу, що повертає результат через `Future`. Виміряти час виконання послідовно та паралельно.
6. Продемонструвати `volatile`: запустити потік у циклі з прапорцем зупинки, переконатися, що без `volatile` потік може не зупинитися.
7. Реалізувати виробника-споживача (`Producer-Consumer`) через `BlockingQueue<String>`: один потік-виробник, один потік-споживач.
8. Реалізувати завдання відповідно до варіанту з використанням `CompletableFuture`.
9. Продемонструвати `Thread.sleep`, `Thread.join`, `interrupt`. Обробити `InterruptedException`.
10. Написати 3 тести, що перевіряють потокобезпечність (використати `CountDownLatch` або `CyclicBarrier` для синхронізації початку).
11. Оформити звіт: вихідний код, діаграма взаємодії потоків, результати порівняльного вимірювання часу.

---

### Індивідуальні завдання

Розробити багатопотокову програму (2–3 класи). Обсяг: до 150 рядків коду. Обов'язково: коректна синхронізація, відсутність deadlock, коректне завершення потоків.

**Варіант 1.** Реалізувати паралельне завантаження даних: 5 `Callable<String>` симулюють завантаження з різних джерел (через `Thread.sleep` з різними затримками). Зібрати результати через `List<Future<String>>`, вивести у порядку завершення через `CompletionService`.

**Варіант 2.** Реалізувати «Виробник-споживач» для IoT-даних (натхненний `ReactionEngine.java`): один потік-«датчик» кожні 500 мс додає показання у `BlockingQueue<Double>` (ємність 10), потік-«реєстратор» забирає та записує у файл. Завершення через `volatile boolean running`.

**Варіант 3.** Реалізувати паралельне сортування: масив з 10 000 елементів розбити на N частин (N = кількість ядер), відсортувати кожну частину у окремому `CompletableFuture`, потім злити. Порівняти час із `Arrays.sort`.

**Варіант 4.** Реалізувати «Банк» з потокобезпечними рахунками: клас `BankAccount` з `ReentrantLock`. N потоків виконують випадкові перекази між рахунками. Перевірити інваріант: сума всіх рахунків до = сума після всіх переказів.

**Варіант 5.** Реалізувати паралельний пошук у файлах: N потоків шукають рядок у N різних файлах (або частинах одного файлу). Використати `ExecutorService` та `Future<Long>` (кількість збігів). Вивести загальний результат.

**Варіант 6.** Реалізувати «Ліфт»: `ElevatorSimulator` з `BlockingQueue<Integer>` (запити поверхів). Один потік-«ліфт» обробляє запити; кілька потоків-«пасажирів» додають запити. Вивести лог переміщень.

**Варіант 7.** Реалізувати `PeriodicTaskRunner` (натхненний `GracefulShutdown.java`): `ScheduledExecutorService` виконує задачу кожні N секунд. Коректне завершення через shutdown hook (`Runtime.getRuntime().addShutdownHook`). Перевірити: при Ctrl+C — задача завершується чисто.

**Варіант 8.** Реалізувати `ParallelWordCount`: `Files.lines(path)` розбити на частини; кожна частина обробляється у окремому потоці через `CompletableFuture.supplyAsync`; результати (`Map<String, Long>`) об'єднати методом `thenCombine`.

**Варіант 9.** Реалізувати «Семафорна парковка»: `Semaphore` на 3 місця; 10 потоків-«автомобілів» намагаються «припаркуватися» (`acquire`) на випадковий час. Вивести лог: хто зайшов, хто чекає, хто вийшов.

**Варіант 10.** Реалізувати потокобезпечний кеш: `Cache<K, V>` на `ConcurrentHashMap` з TTL (час до застарівання). `ScheduledExecutorService` кожну хвилину видаляє прострочені записи. Методи: `put`, `get`, `size`.

**Варіант 11.** Реалізувати `RaceGame`: N «гравців» (потоки) стартують одночасно через `CountDownLatch`. Кожен «гравець» — цикл з `Thread.sleep(random)`. Перший, хто завершить 10 ітерацій, — переможець. Вивести час кожного гравця.

**Варіант 12.** Реалізувати паралельну обробку зображення (симуляція): 2D-масив `int[][]` пікселів. `ForkJoinPool` рекурсивно ділить масив на рядки, кожен рядок обробляє (`pixel * 2` — підвищення яскравості). Порівняти час з послідовною обробкою.

**Варіант 13.** Реалізувати «Читачі-письменники» (`ReadWriteLock`): клас `SharedDocument` зі спільним рядком, захищеним `ReentrantReadWriteLock`. 5 потоків-«читачів» та 2 потоки-«письменники». Вивести лог конкурентного доступу.

**Варіант 14.** Реалізувати `TaskPipeline` з трьома етапами: `Stage1` читає з джерела → передає у `BlockingQueue`; `Stage2` трансформує → передає у `BlockingQueue`; `Stage3` записує у файл. Кожен етап — окремий потік.

**Варіант 15.** Реалізувати «Аукціон»: клас `Auction` з `volatile double currentBid`. N потоків-«учасників» роблять ставки через `compareAndExchange` (`VarHandle`) або `AtomicReference`. Переможець — найвища ставка після завершення аукціону. Перевірити відсутність гонки.

---

### Контрольні запитання

1. Що таке потік виконання (thread)? Яка різниця між потоком та процесом?
2. Чим `Thread.start()` відрізняється від `Thread.run()`? Що відбудеться при виклику `run()` без `start()`?
3. Що таке гонка даних (data race)? Наведіть приклад коду, що демонструє гонку.
4. Що таке `synchronized`? Яка різниця між синхронізованим методом та синхронізованим блоком?
5. Що таке `volatile`? Яку гарантію воно дає і яку — ні?
6. Що таке `AtomicInteger`? Як `compareAndSet` дозволяє реалізувати атомарні операції без `synchronized`?
7. Що таке `ExecutorService`? Яка різниця між `Executors.newFixedThreadPool` та `newCachedThreadPool`?
8. Що таке `Future.get()`? Що відбудеться, якщо задача кинула виключення?
9. Що таке `CompletableFuture`? Чим він відрізняється від `Future` з точки зору функціональної композиції?
10. Що таке `deadlock`? Опишіть умови його виникнення та способи запобігання.
11. Що таке `BlockingQueue`? Для якого паттерну він найчастіше використовується?
12. Що таке `Semaphore`? Наведіть приклад задачі, де він є зручним.
13. Що таке `CountDownLatch`? Чим він відрізняється від `CyclicBarrier`?
14. Що таке віртуальні потоки (Virtual Threads)? У чому їхня перевага над платформними потоками?
15. Що таке `ThreadLocal`? Для яких задач він використовується?

---

### Перелік посилань

1. Goetz, B., Peierls, T., Bloch, J., Bowbeer, J., Holmes, D., Lea, D. *Java Concurrency in Practice*. Boston : Addison-Wesley, 2006. 403 p.
2. Horstmann, C. S. *Core Java, Volume II — Advanced Features* (12th ed.). Hoboken : Pearson, 2022. 832 p.
3. Bloch, J. *Effective Java* (3rd ed.). Boston : Addison-Wesley, 2018. 412 p.
4. Oracle Corporation. *Concurrency — Java Tutorials* [Електронний ресурс]. URL: https://docs.oracle.com/javase/tutorial/essential/concurrency/ (дата звернення: 2025-09-01).
5. Oracle Corporation. *Virtual Threads — JEP 444* [Електронний ресурс]. URL: https://openjdk.org/jeps/444 (дата звернення: 2025-09-01).
6. Oracle Corporation. *java.util.concurrent Package API* [Електронний ресурс]. URL: https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/concurrent/package-summary.html (дата звернення: 2025-09-01).
7. Schildt, H. *Java: The Complete Reference* (12th ed.). New York : McGraw-Hill, 2021. 1248 p.

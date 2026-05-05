# Лабораторна робота №3
## Об'єктно-орієнтоване програмування на Java

---

### Тема

Об'єктно-орієнтоване програмування на Java: класи, об'єкти, інкапсуляція, наслідування, поліморфізм та інтерфейси.

---

### Мета

Набути практичних навичок проєктування та реалізації класових ієрархій мовою Java шляхом розробки програм з використанням принципів інкапсуляції, наслідування та поліморфізму.

---

### Теоретичні відомості

#### Класи та об'єкти

Клас у Java — це шаблон, що описує стан (поля) та поведінку (методи) об'єктів. Об'єкт — екземпляр класу, що займає ділянку в купі (*heap*).

```java
public class Sensor {
    // Поля (стан)
    private final String name;
    private double lastValue;

    // Конструктор
    public Sensor(String name) {
        this.name = name;
    }

    // Гетер
    public double getLastValue() { return lastValue; }

    // Сетер з перевіркою
    public void setLastValue(double value) {
        if (Double.isNaN(value)) throw new IllegalArgumentException("Значення не може бути NaN");
        this.lastValue = value;
    }

    // Перевизначення методу toString
    @Override
    public String toString() {
        return "Sensor{name='" + name + "', value=" + lastValue + '}';
    }
}
```

#### Інкапсуляція

Поля оголошуються `private`; доступ — через публічні методи-гетери/сетери. Це дозволяє контролювати стан об'єкту та змінювати внутрішню реалізацію без порушення контракту класу.

#### Наслідування

```java
// Базовий клас (суперклас)
public abstract class AbstractDevice {
    protected final String id;

    public AbstractDevice(String id) { this.id = id; }

    // Абстрактний метод — підкласи зобов'язані реалізувати
    public abstract String getStatus();

    // Конкретний метод зі спільною логікою
    public String getId() { return id; }
}

// Підклас
public class TemperatureSensor extends AbstractDevice {
    private double celsius;

    public TemperatureSensor(String id) { super(id); }

    @Override
    public String getStatus() {
        return String.format("T=%.1f°C", celsius);
    }
}
```

Ключові слова: `extends` (наслідування класу), `super(...)` (виклик конструктора суперкласу), `@Override` (анотація, підтверджує перевизначення).

#### Інтерфейси

Інтерфейс визначає контракт поведінки. Один клас може реалізовувати кілька інтерфейсів.

```java
public interface Readable {
    double read(); // публічний та абстрактний за замовчуванням
}

public interface Loggable {
    void log(String message);

    // Дефолтний метод (дозволений з Java 8)
    default void logInfo(String msg) { log("[INFO] " + msg); }
}

public class PressureSensor extends AbstractDevice
        implements Readable, Loggable {

    @Override public double read() { /* ... */ return 0; }
    @Override public void log(String message) { System.out.println(message); }
    @Override public String getStatus() { return "ok"; }

    public PressureSensor(String id) { super(id); }
}
```

#### Поліморфізм

Посилання на базовий тип або інтерфейс може вказувати на об'єкт будь-якого сумісного підкласу. Виклик методу відбувається через **динамічне зв'язування** (late binding):

```java
AbstractDevice[] devices = {
    new TemperatureSensor("T-01"),
    new PressureSensor("P-01")
};

for (AbstractDevice d : devices) {
    // Викликається реалізація відповідного підкласу
    System.out.println(d.getStatus());
}
```

#### Записи (records, Java 16+)

Для незмінних об'єктів-даних зручно використовувати `record`:

```java
public record Reading(String sensorId, double value, long timestamp) {}

// Компілятор автоматично генерує: конструктор, гетери, equals, hashCode, toString
Reading r = new Reading("T-01", 36.6, System.currentTimeMillis());
System.out.println(r.value()); // гетер без "get" префікса
```

#### Запечатані класи (sealed classes, Java 17+)

```java
public sealed interface Shape permits Circle, Rectangle {}
public record Circle(double radius) implements Shape {}
public record Rectangle(double w, double h) implements Shape {}
```

#### Приклад з репозиторію java-iot

Інтерфейс [`CrowPiProject.java`](https://github.com/AndriyLutskiv/java-iot/blob/main/core/src/main/java/ua/crowpi/core/CrowPiProject.java) демонструє контракт для всіх IoT-проєктів. Клас [`ThermometerProject.java`](https://github.com/AndriyLutskiv/java-iot/blob/main/projects/p01-thermometer/src/main/java/ua/crowpi/projects/p01/ThermometerProject.java) реалізує цей інтерфейс. Клас [`AlarmFsm.java`](https://github.com/AndriyLutskiv/java-iot/blob/main/projects/p04-alarm/src/main/java/ua/crowpi/projects/p04/AlarmFsm.java) демонструє поліморфізм через скінченний автомат. Рекомендується вивчити ці файли як зразок побудови класових ієрархій.

---

### Хід виконання роботи

1. Відкрити та проаналізувати інтерфейс `CrowPiProject.java` (посилання вище). Визначити: скільки методів оголошено, які параметри та типи повернення, чи є дефолтні методи.
2. Переглянути клас `ThermometerProject.java` — виявити, які методи інтерфейсу реалізовано, як використовуються поля та конструктор.
3. Спроєктувати власну класову ієрархію відповідно до варіанту: визначити базовий клас (або інтерфейс), 2–3 підкласи, поля та методи.
4. Реалізувати базовий клас або інтерфейс: оголосити поля з `private`/`protected`, написати конструктор, гетери, `toString`, `equals`, `hashCode`.
5. Реалізувати підкласи з використанням `extends`/`implements`. Перевизначити абстрактні методи з анотацією `@Override`.
6. Написати метод `main`, що створює масив об'єктів різних підкласів через посилання на базовий тип та демонструє поліморфну поведінку.
7. Реалізувати принаймні один `record` для представлення незмінного стану (результат вимірювання, подія тощо).
8. Продемонструвати динамічне зв'язування: викликати перевизначений метод через посилання на суперклас.
9. Перевірити роботу `instanceof` та розпізнавання зразків (pattern matching): `if (device instanceof TemperatureSensor ts)`.
10. Написати принаймні 3 модульні тести (JUnit 5) для ключових методів розроблених класів.
11. Оформити звіт: включити UML-діаграму класів (навіть у текстовому вигляді), вихідний код та результати тестування.

---

### Індивідуальні завдання

Розробити класову ієрархію з 1 базовим класом або інтерфейсом і 2–3 підкласами. Обсяг: до 120 рядків коду (без тестів). Продемонструвати поліморфізм у методі `main`.

**Варіант 1.** Ієрархія «Транспортний засіб»: абстрактний клас `Vehicle` (поля: марка, рік, макс. швидкість); підкласи `Car` (тип кузова), `Truck` (вантажопідйомність), `Motorcycle` (об'єм двигуна). Метод `describe()` у кожному підкласі. Демонстрація у масиві.

**Варіант 2.** Ієрархія «Геометричні фігури»: інтерфейс `Shape` з методами `area()` та `perimeter()`; `record`-реалізації `Circle(double r)`, `Rectangle(double w, double h)`, `Triangle(double a, double b, double c)`. Вивести площі та периметри у відсортованому порядку.

**Варіант 3.** Ієрархія «Банківський рахунок»: абстрактний клас `BankAccount` (номер, баланс); підкласи `SavingsAccount` (відсоткова ставка, нарахування відсотків), `CheckingAccount` (ліміт овердрафту), `DepositAccount` (строк депозиту). Метод `getYearlyIncome()`.

**Варіант 4.** Ієрархія «Співробітник»: абстрактний клас `Employee` (ім'я, id); підкласи `FullTimeEmployee` (ставка), `PartTimeEmployee` (годинний тариф + кількість годин), `Contractor` (вартість контракту). Абстрактний метод `calculateSalary()`.

**Варіант 5.** Ієрархія «IoT-датчик» (на основі `java-iot`): інтерфейс `Sensor` з методами `read()` та `getUnit()`; `record`-реалізації `TemperatureSensor`, `HumiditySensor`, `PressureSensor`. Порівняти з інтерфейсом `CrowPiProject`.

**Варіант 6.** Ієрархія «Музичний інструмент»: абстрактний клас `Instrument` (назва, матеріал); підкласи `StringInstrument` (кількість струн), `WindInstrument` (тип мундштука), `PercussionInstrument` (тип корпусу). Метод `play()` з різним виводом.

**Варіант 7.** Ієрархія «Тварина»: абстрактний клас `Animal` (назва, вік); підкласи `Dog` (порода, дресирування), `Cat` (масть, самостійність), `Bird` (розмах крил). Метод `sound()`. Демонстрація поліморфізму через `List<Animal>`.

**Варіант 8.** Ієрархія «Електронний пристрій»: запечатаний інтерфейс `Device` (sealed) з дозволеними `Laptop`, `Smartphone`, `Tablet`. Кожен — `record` з полями (бренд, рік, ціна). Метод `describe()` через `switch` з розпізнаванням зразків.

**Варіант 9.** Ієрархія «Університет»: абстрактний клас `Person` (ім'я, email); підкласи `Student` (залікова книжка, середній бал), `Lecturer` (кафедра, вчений ступінь), `Administrator` (відділ). Метод `getRole()`.

**Варіант 10.** Ієрархія «Їжа»: інтерфейс `Edible` (методи `getCalories()`, `isVegetarian()`); `record`-реалізації `Fruit(String name, double calories)`, `Meat(String type, double calories)`, `Vegetable(String name, double calories)`. Сортування за калорійністю.

**Варіант 11.** Ієрархія «Фінансовий інструмент»: абстрактний клас `FinancialAsset` (назва, ціна); підкласи `Stock` (тікер, дивіденди), `Bond` (купон, термін), `Cryptocurrency` (символ, капіталізація). Метод `getExpectedReturn()`.

**Варіант 12.** Ієрархія «Повідомлення»: інтерфейс `Message` (методи `getSender()`, `getContent()`, `getTimestamp()`); `record`-реалізації `TextMessage`, `EmailMessage`, `PushNotification`. Вивести повідомлення у хронологічному порядку.

**Варіант 13.** Ієрархія «Ігровий персонаж»: абстрактний клас `Character` (ім'я, здоров'я, рівень); підкласи `Warrior` (атака ближнього бою), `Mage` (запас мани), `Archer` (точність). Абстрактний метод `attack()`.

**Варіант 14.** Ієрархія «Логістика»: абстрактний клас `Package` (трекінг-номер, вага, адреса); підкласи `StandardPackage`, `ExpressPackage` (термін доставки у годинах), `FragilePackage` (страхова вартість). Метод `calculateShippingCost()`.

**Варіант 15.** Ієрархія «Медіаконтент»: запечатаний інтерфейс `Media` з реалізаціями `Video(String title, int durationSeconds)`, `Audio(String title, int durationSeconds)`, `Podcast(String title, int episodeCount)`. Метод `describe()` через розпізнавання зразків у `switch`.

---

### Контрольні запитання

1. Що таке клас та об'єкт у Java? Яка різниця між ними з точки зору пам'яті?
2. Що означає інкапсуляція? Навіщо поля оголошуються `private`?
3. Яка різниця між `abstract class` та `interface` в Java? Коли слід обирати одне, а коли інше?
4. Що таке поліморфізм? Поясніть, що означає «динамічне зв'язування» (late binding).
5. Що робить ключове слово `super`? Наведіть два різних способи його використання.
6. Яке призначення анотації `@Override`? Що відбудеться, якщо перевизначити метод із помилкою в підписі без цієї анотації?
7. Що таке `record` у Java? Які методи генеруються компілятором автоматично?
8. Що таке `sealed` клас? Яку проблему він вирішує?
9. Що таке розпізнавання зразків (pattern matching) для `instanceof`? Як воно спрощує код?
10. Яка різниця між перевантаженням (overloading) та перевизначенням (overriding) методу?
11. Що таке конструктор? Чи може клас мати кілька конструкторів? Що таке конструктор за замовчуванням?
12. Що означає ключове слово `final` для класу, методу та поля?
13. Навіщо перевизначати методи `equals` та `hashCode`? Яке правило зв'язку між ними?
14. Що таке дефолтний метод (default method) в інтерфейсі? Коли він є доречним?
15. Поясніть концепцію «програмування на інтерфейс, а не на реалізацію». Яку перевагу це дає?

---

### Перелік посилань

1. Bloch, J. *Effective Java* (3rd ed.). Boston : Addison-Wesley, 2018. 412 p.
2. Horstmann, C. S. *Core Java, Volume I — Fundamentals* (12th ed.). Hoboken : Pearson, 2022. 928 p.
3. Gamma, E., Helm, R., Johnson, R., Vlissides, J. *Design Patterns: Elements of Reusable Object-Oriented Software*. Boston : Addison-Wesley, 1994. 395 p.
4. Oracle Corporation. *Classes and Objects — Java Tutorials* [Електронний ресурс]. URL: https://docs.oracle.com/javase/tutorial/java/javaOO/ (дата звернення: 2025-09-01).
5. Oracle Corporation. *Sealed Classes — JEP 409* [Електронний ресурс]. URL: https://openjdk.org/jeps/409 (дата звернення: 2025-09-01).
6. Oracle Corporation. *Record Classes — JEP 395* [Електронний ресурс]. URL: https://openjdk.org/jeps/395 (дата звернення: 2025-09-01).
7. Schildt, H. *Java: The Complete Reference* (12th ed.). New York : McGraw-Hill, 2021. 1248 p.

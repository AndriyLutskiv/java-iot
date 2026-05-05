# Лабораторна робота №6
## Застосування класів-колекцій і класів утиліт. Створення автоматизованих тестів

---

### Тема

Застосування класів-колекцій (`List`, `Set`, `Map`, `Queue`) та класів-утиліт (`Collections`, `Arrays`) Java; написання автоматизованих модульних тестів за допомогою фреймворку JUnit 5.

---

### Мета

Набути практичних навичок використання колекцій Java Collections Framework та написання автоматизованих модульних тестів із застосуванням JUnit 5 шляхом реалізації та тестування програм обробки даних.

---

### Теоретичні відомості

#### Java Collections Framework

**Основні інтерфейси та їх реалізації:**

| Інтерфейс | Реалізації | Особливості |
|-----------|-----------|-------------|
| `List<E>` | `ArrayList`, `LinkedList` | Впорядкована колекція, допускає дублікати |
| `Set<E>` | `HashSet`, `LinkedHashSet`, `TreeSet` | Множина без дублікатів |
| `Map<K,V>` | `HashMap`, `LinkedHashMap`, `TreeMap` | Пари «ключ — значення» |
| `Queue<E>` | `ArrayDeque`, `PriorityQueue` | Черга FIFO або пріоритетна |
| `Deque<E>` | `ArrayDeque` | Двобічна черга (стек + черга) |

```java
import java.util.*;

// List
List<String> names = new ArrayList<>(List.of("Іван", "Марія", "Олег"));
names.add("Ганна");
names.remove("Олег");
Collections.sort(names);

// Map
Map<String, Integer> scores = new HashMap<>();
scores.put("Іван", 95);
scores.put("Марія", 88);
int score = scores.getOrDefault("Петро", 0); // 0, якщо ключ відсутній

// Set
Set<String> unique = new HashSet<>(names);
unique.add("Іван"); // дублікат — проігнорується

// Незмінні колекції (Java 9+)
List<String> immutable = List.of("A", "B", "C");
Map<String, Integer> immutableMap = Map.of("a", 1, "b", 2);
```

#### Клас `Collections`

```java
List<Integer> nums = new ArrayList<>(List.of(5, 3, 8, 1, 9));

Collections.sort(nums);                         // [1, 3, 5, 8, 9]
Collections.reverse(nums);                      // [9, 8, 5, 3, 1]
Collections.shuffle(nums);                      // випадковий порядок
int max = Collections.max(nums);                // максимум
Collections.frequency(nums, 5);                 // кількість входжень
List<Integer> unmod = Collections.unmodifiableList(nums); // тільки для читання
```

#### Stream API (Java 8+)

```java
List<String> names = List.of("Анна", "Богдан", "Василь", "Аліна");

// Фільтрація + перетворення + збирання
List<String> filtered = names.stream()
    .filter(n -> n.startsWith("А"))
    .map(String::toUpperCase)
    .sorted()
    .collect(Collectors.toList());
// ["АЛІНА", "АННА"]

// Агрегація
double avg = names.stream()
    .mapToInt(String::length)
    .average()
    .orElse(0.0);

// Групування
Map<Integer, List<String>> byLength = names.stream()
    .collect(Collectors.groupingBy(String::length));
```

#### JUnit 5 — структура тестів

```java
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class CalculatorTest {

    @BeforeEach
    void setUp() {
        // ініціалізація перед кожним тестом
    }

    @Test
    @DisplayName("Сума двох позитивних чисел")
    void addPositiveNumbers() {
        assertEquals(5, Calculator.add(2, 3), "2 + 3 повинно дорівнювати 5");
    }

    @Test
    void divideByZeroThrows() {
        assertThrows(ArithmeticException.class, () -> Calculator.divide(1, 0));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 3, 5, 7, 9})
    void oddNumbers(int n) {
        assertTrue(n % 2 != 0, n + " повинно бути непарним");
    }
}
```

**Ключові анотації JUnit 5:** `@Test`, `@BeforeEach`, `@AfterEach`, `@BeforeAll`, `@AfterAll`, `@DisplayName`, `@ParameterizedTest`, `@ValueSource`, `@CsvSource`, `@Disabled`.

**Основні методи `Assertions`:** `assertEquals`, `assertNotEquals`, `assertTrue`, `assertFalse`, `assertNull`, `assertNotNull`, `assertThrows`, `assertAll`, `assertArrayEquals`.

#### Приклад з репозиторію java-iot

Клас [`MelodyLibrary.java`](https://github.com/AndriyLutskiv/java-iot/blob/main/projects/p03-music-box/src/main/java/ua/crowpi/projects/p03/MelodyLibrary.java) зберігає мелодії у `Map<String, Melody>`. Клас [`LeaderBoard.java`](https://github.com/AndriyLutskiv/java-iot/blob/main/projects/p11-lcd-game/src/main/java/ua/crowpi/projects/p11/LeaderBoard.java) реалізує рейтингову таблицю через `TreeMap`/`List`. Тест [`PropertiesLoaderTest.java`](https://github.com/AndriyLutskiv/java-iot/blob/main/core/src/test/java/ua/crowpi/core/util/PropertiesLoaderTest.java) демонструє написання JUnit 5 тестів у стилі репозиторію. Рекомендується проаналізувати всі три класи.

---

### Хід виконання роботи

1. Відкрити та проаналізувати `MelodyLibrary.java`, `LeaderBoard.java` та `PropertiesLoaderTest.java` (посилання вище). Визначити типи колекцій та методи, що використовуються.
2. Написати демонстраційний клас, що показує роботу `ArrayList`, `LinkedList`, `HashSet`, `TreeSet`, `HashMap`, `TreeMap`. Для кожної колекції показати: додавання, видалення, пошук, перебір.
3. Порівняти продуктивність `ArrayList` та `LinkedList` при вставці на початок 100 000 елементів. Використати `System.nanoTime()`. Пояснити результат.
4. Реалізувати завдання відповідно до свого варіанту.
5. Реалізувати щонайменше одну операцію з використанням Stream API: фільтрацію, сортування та групування (метод `groupingBy`).
6. Написати щонайменше 10 JUnit 5 тестів для реалізованих методів. Використати `@ParameterizedTest` з `@CsvSource` для принаймні 2 тестів.
7. Використати `@BeforeEach` для ініціалізації тестових даних та `assertThrows` для перевірки виключень.
8. Перевірити покриття коду тестами (Coverage) в IDE (IntelliJ: Run with Coverage). Прагнути до ≥80% покриття класу з бізнес-логікою.
9. Оформити звіт: вихідний код, результати JUnit (кількість тестів, кількість успішних), знімок звіту покриття.

---

### Індивідуальні завдання

Розробити програму (2–3 класи), що використовує колекції Java, та написати не менше 10 JUnit 5 тестів. Обсяг коду: до 150 рядків (без тестів).

**Варіант 1.** Реалізувати клас `StudentRegistry` для реєстру студентів: зберігати у `Map<String, Student>` (ключ — залікова книжка). Методи: `add`, `remove`, `findByName(String)` → `List<Student>`, `topStudents(int n)` — топ-N за середнім балом. Тести: коректність додавання, дублікат, пошук, топ-список.

**Варіант 2.** Реалізувати клас `LibraryCatalog` для каталогу книг: зберігати у `Map<String, List<Book>>` (ключ — автор). Методи: `addBook`, `findByTitle(String)`, `getByAuthor(String)`, `getMostProlificAuthor()`. Тести: порожній каталог, однаковий автор, пошук по рядку.

**Варіант 3.** Реалізувати клас `PlaylistManager` (натхненний [`MelodyLibrary.java`](https://github.com/AndriyLutskiv/java-iot/blob/main/projects/p03-music-box/src/main/java/ua/crowpi/projects/p03/MelodyLibrary.java)): `Map<String, List<String>>` (жанр → список пісень). Методи: `addSong`, `removeSong`, `shuffle(String genre)`, `getByGenre`. Тести: невідомий жанр, перетасовка.

**Варіант 4.** Реалізувати клас `ScoreBoard` (натхненний [`LeaderBoard.java`](https://github.com/AndriyLutskiv/java-iot/blob/main/projects/p11-lcd-game/src/main/java/ua/crowpi/projects/p11/LeaderBoard.java)): рейтинг гравців у `TreeMap<Integer, String>` (рахунок → ім'я). Методи: `addScore`, `getTop(int n)`, `getRank(String name)`. Тести: рівний рахунок двох гравців, негативний рахунок.

**Варіант 5.** Реалізувати клас `FrequencyCounter<T>` із generic-параметром: підраховує кількість кожного елемента у `Map<T, Integer>`. Методи: `add(T)`, `getCount(T)`, `getMostFrequent()`, `getAll()` — відсортовані за частотою. Тести: рядки, числа.

**Варіант 6.** Реалізувати клас `UniqueWordDictionary`: зберігає унікальні слова у `TreeSet<String>`. Методи: `addWord`, `remove`, `startingWith(String prefix)` → підмножина, `size`. Тести: нечутливість до регістру, порядок сортування.

**Варіант 7.** Реалізувати клас `TaskQueue` на основі `ArrayDeque<Task>` (клас `Task` із пріоритетом та описом). Методи: `enqueue`, `dequeue`, `peek`, `processAll`. Альтернативна реалізація з `PriorityQueue`. Тести: порожня черга, порядок видачі.

**Варіант 8.** Реалізувати клас `EventBus` (паттерн «Спостерігач»): `Map<String, List<Runnable>>` (тема → список обробників). Методи: `subscribe(String topic, Runnable handler)`, `publish(String topic)`, `unsubscribe`. Тести: публікація без підписників, кілька підписників.

**Варіант 9.** Реалізувати клас `InventoryManager` для складу: `Map<String, Integer>` (товар → кількість). Методи: `addStock`, `removeStock`, `getLowStock(int threshold)`, `getTotalValue(Map<String, Double> prices)`. Тести: від'ємний залишок, порожній склад.

**Варіант 10.** Реалізувати клас `PhoneBook`: `Map<String, Set<String>>` (ім'я → набір номерів телефонів). Методи: `addContact`, `addPhone`, `removeContact`, `findByPhone(String)` — знайти ім'я за номером. Тести: кілька номерів, видалення.

**Варіант 11.** Реалізувати клас `SensorDataBuffer<T>` на основі `ArrayDeque` з обмеженням розміру (circlar buffer). Методи: `push(T)` (при переповненні — видаляти найстаріший), `toList()`, `average()` (для числових типів через `Number`). Тести: переповнення.

**Варіант 12.** Реалізувати клас `WordCloud`: підраховує частоту слів у тексті (`Map<String, Long>` через `Collectors.counting`). Методи: `analyze(String text)`, `topN(int n)`, `wordsLongerThan(int len)`. Використати Stream API у всіх методах. Тести: порожній рядок, регістр.

**Варіант 13.** Реалізувати клас `ProductCatalog`: `List<Product>` де `Product record(String name, String category, double price)`. Методи через Stream API: `filterByCategory`, `sortByPrice`, `averagePriceByCategory()` → `Map<String, Double>`. Тести: порожній каталог, сортування.

**Варіант 14.** Реалізувати клас `GraphAdjacency` для орієнтованого графа: `Map<String, Set<String>>` (вузол → множина сусідів). Методи: `addEdge`, `removeEdge`, `getNeighbors`, `hasCycle()` (обхід DFS). Тести: самопетля, граф без циклу, граф з циклом.

**Варіант 15.** Реалізувати клас `MultiSetCounter<T>`: мультимножина, що дозволяє кілька екземплярів одного значення. Зберігати у `HashMap<T, Integer>`. Методи: `add(T, int count)`, `remove(T, int count)`, `entrySet()` відсортований. Тести: від'ємна кількість, нульова кількість.

---

### Контрольні запитання

1. Яка різниця між `ArrayList` та `LinkedList` з точки зору складності операцій? Коли слід обирати кожен?
2. Яка різниця між `HashSet`, `LinkedHashSet` та `TreeSet`? Який порядок обходу у кожному?
3. Чому `HashMap` не гарантує порядку ітерації? Яка реалізація зберігає порядок вставки?
4. Що таке `equals` та `hashCode` у контексті `HashMap`? Що станеться, якщо два об'єкти рівні за `equals`, але мають різні `hashCode`?
5. Що таке незмінні колекції (`List.of`, `Map.of`)? Яку виключення кидають при спробі модифікації?
6. Яка різниця між `Collections.unmodifiableList` та `List.of`?
7. Що таке `Comparator` та `Comparable`? Як реалізувати нестандартне сортування?
8. Що таке `Stream` у Java? Яка різниця між проміжними (intermediate) та термінальними (terminal) операціями?
9. Що таке `Collectors.groupingBy`? Наведіть приклад групування списку об'єктів за полем.
10. Які анотації JUnit 5 відповідають за: ініціалізацію перед кожним тестом, перевірку виключення, параметризований тест?
11. Що таке `assertAll` у JUnit 5? Чим він відрізняється від кількох окремих `assertEquals`?
12. Що таке покриття коду (code coverage)? Яка різниця між покриттям рядків і покриттям гілок?
13. Що таке `@TempDir` у JUnit 5? Для яких тестів він є доречним?
14. Чому не слід тестувати приватні методи безпосередньо? Як тестувати поведінку, що залежить від приватного методу?
15. Що таке `PriorityQueue`? Який природний порядок вона використовує за замовчуванням?

---

### Перелік посилань

1. Bloch, J. *Effective Java* (3rd ed.). Boston : Addison-Wesley, 2018. 412 p.
2. Horstmann, C. S. *Core Java, Volume I — Fundamentals* (12th ed.). Hoboken : Pearson, 2022. 928 p.
3. Oracle Corporation. *Collections — Java Tutorials* [Електронний ресурс]. URL: https://docs.oracle.com/javase/tutorial/collections/ (дата звернення: 2025-09-01).
4. Oracle Corporation. *java.util.stream — Stream API* [Електронний ресурс]. URL: https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/stream/package-summary.html (дата звернення: 2025-09-01).
5. JUnit Team. *JUnit 5 User Guide* [Електронний ресурс]. URL: https://junit.org/junit5/docs/current/user-guide/ (дата звернення: 2025-09-01).
6. Schildt, H. *Java: The Complete Reference* (12th ed.). New York : McGraw-Hill, 2021. 1248 p.
7. Warburton, R. *Java 8 Lambdas*. Sebastopol : O'Reilly Media, 2014. 182 p.

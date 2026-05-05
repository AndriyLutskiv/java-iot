# Лабораторна робота №10
## Робота з базами даних

---

### Тема

Робота з реляційними базами даних у Java засобами JDBC: підключення, виконання SQL-запитів, транзакції та патерн репозиторію.

---

### Мета

Набути практичних навичок взаємодії з реляційними базами даних у Java шляхом реалізації програм, що використовують JDBC API для виконання SQL-запитів `SELECT`, `INSERT`, `UPDATE`, `DELETE`, керування транзакціями та патерн репозиторію.

---

### Теоретичні відомості

#### Архітектура JDBC

**JDBC (Java Database Connectivity)** — стандартний API для взаємодії Java-застосунків з реляційними базами даних. Архітектура:

```
Застосунок Java
    ↓  (JDBC API)
JDBC Driver Manager
    ↓  (JDBC Driver)
База даних (SQLite / PostgreSQL / MySQL / H2 тощо)
```

Ключові класи в пакеті `java.sql`:

| Клас/Інтерфейс | Призначення |
|----------------|-------------|
| `DriverManager` | Керування драйверами, отримання з'єднань |
| `Connection` | З'єднання з БД; управління транзакціями |
| `Statement` | Виконання простих SQL-запитів |
| `PreparedStatement` | Параметризовані запити (захист від SQL-ін'єкції) |
| `CallableStatement` | Виклик збережених процедур |
| `ResultSet` | Набір рядків результату запиту |

#### SQLite як навчальна база даних

**SQLite** — легковагова вбудована СУБД, що зберігає базу у єдиному файлі. Не потребує окремого серверного процесу. Ідеально для навчального середовища. JDBC-драйвер: `org.xerial:sqlite-jdbc`.

```xml
<!-- Maven -->
<dependency>
    <groupId>org.xerial</groupId>
    <artifactId>sqlite-jdbc</artifactId>
    <version>3.45.3.0</version>
</dependency>
```

#### Підключення та базові операції

```java
import java.sql.*;

public class DatabaseExample {

    private static final String URL = "jdbc:sqlite:mydata.db";

    public static void main(String[] args) throws SQLException {
        // Підключення — автоматично закривається у try-with-resources
        try (Connection conn = DriverManager.getConnection(URL)) {

            // Створення таблиці
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS sensors (
                        id    INTEGER PRIMARY KEY AUTOINCREMENT,
                        name  TEXT    NOT NULL,
                        value REAL,
                        ts    INTEGER NOT NULL
                    )
                """);
            }

            // Вставка (НІКОЛИ не конкатенувати значення у SQL — SQL-ін'єкція!)
            String sql = "INSERT INTO sensors (name, value, ts) VALUES (?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, "DHT11");
                ps.setDouble(2, 23.5);
                ps.setLong(3, System.currentTimeMillis());
                ps.executeUpdate();
            }

            // Запит
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, name, value, ts FROM sensors WHERE name = ?")) {
                ps.setString(1, "DHT11");
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        System.out.printf("ID=%d  name=%s  value=%.1f%n",
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getDouble("value"));
                    }
                }
            }
        }
    }
}
```

#### Транзакції

За замовчуванням JDBC увімкнено режим `autoCommit=true` — кожен оператор фіксується одразу. Для атомарного виконання кількох операцій:

```java
conn.setAutoCommit(false);
try {
    debit(conn, fromAccount, amount);
    credit(conn, toAccount, amount);
    conn.commit();              // зафіксувати обидві операції разом
} catch (SQLException e) {
    conn.rollback();            // відкотити обидві при помилці
    throw e;
} finally {
    conn.setAutoCommit(true);
}
```

#### Патерн репозиторію (Repository Pattern)

Відокремлення логіки доступу до бази від бізнес-логіки:

```java
// Модель даних
public record SensorReading(long id, String name, double value, long timestamp) {}

// Інтерфейс репозиторію
public interface SensorReadingRepository {
    void save(SensorReading reading) throws SQLException;
    List<SensorReading> findAll() throws SQLException;
    List<SensorReading> findByName(String name) throws SQLException;
    void deleteById(long id) throws SQLException;
}

// Реалізація
public class SqliteSensorReadingRepository implements SensorReadingRepository {
    private final Connection conn;

    public SqliteSensorReadingRepository(Connection conn) { this.conn = conn; }

    @Override
    public void save(SensorReading r) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO sensors(name, value, ts) VALUES(?,?,?)")) {
            ps.setString(1, r.name());
            ps.setDouble(2, r.value());
            ps.setLong(3, r.timestamp());
            ps.executeUpdate();
        }
    }
    // ... інші методи
}
```

#### Запобігання SQL-ін'єкціям

**Ніколи** не будувати SQL-рядки конкатенацією з користувацьким введенням:

```java
// НЕБЕЗПЕЧНО!
String badSql = "SELECT * FROM users WHERE name = '" + userInput + "'";

// ПРАВИЛЬНО — PreparedStatement
PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE name = ?");
ps.setString(1, userInput);
```

#### Приклад з репозиторію java-iot

Клас [`DatabaseManager.java`](https://github.com/AndriyLutskiv/java-iot/blob/main/projects/p09-smart-home/src/main/java/ua/crowpi/projects/p09/DatabaseManager.java) ініціалізує SQLite-базу та виконує DDL (CREATE TABLE). Клас [`DeviceEventRepository.java`](https://github.com/AndriyLutskiv/java-iot/blob/main/projects/p09-smart-home/src/main/java/ua/crowpi/projects/p09/DeviceEventRepository.java) реалізує CRUD-операції для подій пристроїв. Клас [`ProfileRepository.java`](https://github.com/AndriyLutskiv/java-iot/blob/main/projects/p09-smart-home/src/main/java/ua/crowpi/projects/p09/ProfileRepository.java) зберігає профілі налаштувань. Рекомендується вивчити ці класи як зразок промислового використання JDBC.

---

### Хід виконання роботи

1. Відкрити та проаналізувати `DatabaseManager.java`, `DeviceEventRepository.java` та `ProfileRepository.java` (посилання вище). Визначити: як встановлюється з'єднання, які SQL-запити виконуються, як закриваються ресурси.
2. Підключити JDBC-драйвер SQLite до проєкту (Gradle або Maven-залежність). Переконатися у можливості підключення командою `DriverManager.getConnection("jdbc:sqlite:test.db")`.
3. Створити схему бази даних (таблиці) відповідно до варіанту. Використати `CREATE TABLE IF NOT EXISTS` у методі ініціалізації.
4. Реалізувати клас репозиторію з методами `save`, `findAll`, `findById`, `update`, `deleteById`. Усі методи — через `PreparedStatement`.
5. Реалізувати `main`-метод, що демонструє повний CRUD-цикл: вставка 5+ записів, читання всіх, оновлення одного, видалення одного, повторне читання.
6. Реалізувати транзакцію для операції, що вимагає атомарності (залежно від варіанту): вимкнути `autoCommit`, виконати кілька операцій, `commit`/`rollback`.
7. Реалізувати запит з `WHERE`, `ORDER BY` та `LIMIT`. Вивести результати у форматованій таблиці.
8. Реалізувати пошук за декількома параметрами з `AND`/`OR`.
9. Написати 5 JUnit 5 тестів з `@BeforeEach` (створення in-memory SQLite `jdbc:sqlite::memory:`) та `@AfterEach` (закриття з'єднання).
10. Оформити звіт: схема бази даних (у вигляді SQL CREATE TABLE або таблиць), вихідний код репозиторію, результати CRUD-демонстрації, результати тестів.

---

### Індивідуальні завдання

Розробити програму (3–4 класи: схема БД, репозиторій, модель, демонстрація). Обсяг: до 150 рядків коду. СУБД: SQLite (in-file або in-memory для тестів).

**Варіант 1.** База даних студентів: таблиці `students(id, name, group_name, year)` та `grades(id, student_id, subject, grade)`. Репозиторій: `addStudent`, `addGrade`, `getAverageGrade(int studentId)`, `getTopStudents(int n)`. Тести: порожня БД, топ-список.

**Варіант 2.** База даних бібліотеки: таблиці `books(id, title, author, year, available)` та `loans(id, book_id, reader_name, loan_date, return_date)`. Репозиторій: `borrowBook` (транзакція: позначити `available=false` + додати запис у loans), `returnBook`.

**Варіант 3.** База даних IoT-датчиків (натхненний [`DeviceEventRepository.java`](https://github.com/AndriyLutskiv/java-iot/blob/main/projects/p09-smart-home/src/main/java/ua/crowpi/projects/p09/DeviceEventRepository.java)): таблиця `events(id, device_id, event_type, timestamp, payload TEXT)`. Методи: `logEvent`, `getLastN(int n)`, `getByDevice(String id)`, `cleanup(long olderThanMs)`.

**Варіант 4.** Інтернет-магазин: таблиці `products(id, name, price, stock)` та `orders(id, product_id, quantity, total, created_at)`. Репозиторій: `placeOrder` (транзакція: перевірити наявність → зменшити `stock` → додати замовлення). Тест на недостатній залишок.

**Варіант 5.** Контакт-книга: таблиця `contacts(id, name, email, phone, notes)`. Репозиторій: `add`, `findByName(String)` (LIKE-пошук), `findByPhone`, `update`, `delete`. Всі методи — `PreparedStatement`. Тести: пошук з кирилицею.

**Варіант 6.** Менеджер задач: таблиця `tasks(id, title, description, status, priority, created_at, due_date)`. Статус: `TODO`, `IN_PROGRESS`, `DONE`. Методи: `create`, `updateStatus`, `getByStatus`, `getOverdue(long now)`. Тест транзакції оновлення.

**Варіант 7.** Профілі розумного будинку (натхненний [`ProfileRepository.java`](https://github.com/AndriyLutskiv/java-iot/blob/main/projects/p09-smart-home/src/main/java/ua/crowpi/projects/p09/ProfileRepository.java)): таблиця `profiles(id, name, settings_json)`. Зберігати налаштування як JSON-рядок. Методи: `saveProfile`, `loadProfile(String name)`, `listProfiles`, `deleteProfile`. Тест завантаження відсутнього профілю.

**Варіант 8.** Журнал вимірювань: таблиця `measurements(id, sensor_name, value, unit, timestamp)`. Методи: `record`, `getAverage(String sensor, long from, long to)`, `getMin`, `getMax`, `getCount`. Всі агрегати — через SQL-функції, не Java. Тести на правильність агрегатів.

**Варіант 9.** Банківська система: таблиці `accounts(id, owner, balance)` та `transfers(id, from_id, to_id, amount, ts)`. Метод `transfer(long fromId, long toId, double amount)` — транзакція. Тест: паралельні перекази без втрати балансу (через `SERIALIZABLE` ізоляцію).

**Варіант 10.** Каталог фільмів: таблиці `movies(id, title, year, genre, rating)` та `reviews(id, movie_id, reviewer, score, comment)`. Методи: `addMovie`, `addReview`, `getAverageRating(int movieId)`, `getTopByGenre(String genre, int n)`. Тести на порядок сортування.

**Варіант 11.** Реєстр транспортних засобів: таблиця `vehicles(id, plate, owner, brand, model, year, insured)`. Методи: `register`, `findByPlate`, `findByOwner`, `updateInsurance(String plate, boolean insured)`, `getExpiredInsurance()`. Тести: пошук за точним та частковим номером.

**Варіант 12.** Система бронювання: таблиці `rooms(id, number, type, price_per_night)` та `reservations(id, room_id, guest_name, check_in, check_out)`. Метод `reserve` — транзакція з перевіркою перетину дат. Тести: спроба подвійного бронювання.

**Варіант 13.** Словник: таблиця `words(id, original, translation, language_from, language_to, added_at)`. Методи: `addWord`, `findTranslation(String word, String langFrom, String langTo)`, `randomWord(String langFrom)`, `deleteWord`. Тест з кирилицею.

**Варіант 14.** Система оцінювання (LMS): таблиці `courses(id, name, description)`, `enrollments(id, student_name, course_id, enrolled_at)`, `submissions(id, enrollment_id, score, submitted_at)`. Метод `getFinalScore(String student, String course)` — середнє балів. Тест пустого курсу.

**Варіант 15.** Кімнатні рослини: таблиця `plants(id, name, species, last_watered, last_fertilized, location)`. Методи: `add`, `water(int id)`, `fertilize(int id)`, `getNeedingWater(long maxAgeMs)`, `getNeedingFertilizer`. Тест на правильне визначення потребуючих догляду.

---

### Контрольні запитання

1. Що таке JDBC? Яку роль відіграє JDBC-драйвер?
2. Яка різниця між `Statement` та `PreparedStatement`? Чому завжди слід надавати перевагу `PreparedStatement`?
3. Що таке SQL-ін'єкція? Наведіть приклад уразливого коду та його безпечний аналог.
4. Що таке транзакція? Які властивості описує акронім ACID?
5. Що означає `autoCommit=true` за замовчуванням? Коли потрібно вимикати цей режим?
6. Що таке `rollback`? Коли він викликається автоматично, а коли — вручну?
7. Що таке `ResultSet`? Як читати значення стовпців за ім'ям та за індексом?
8. Що відбудеться, якщо `Connection`, `Statement` або `ResultSet` не закрити? Яку конструкцію Java використовувати для гарантованого закриття?
9. Що таке SQL DDL та DML? Наведіть по одному прикладу операторів кожного типу.
10. Що таке JOIN в SQL? Поясніть різницю між `INNER JOIN`, `LEFT JOIN` та `RIGHT JOIN`.
11. Що таке `LIKE` в SQL? Як шукати рядки, що містять підрядок?
12. Що таке `ORDER BY` та `LIMIT`? Як вибрати перші 10 записів, відсортованих за датою?
13. Що таке індекс у базі даних? Коли його варто створювати?
14. Що таке in-memory база даних SQLite (`jdbc:sqlite::memory:`)? Чому вона зручна для тестування?
15. Що таке патерн «Репозиторій» (Repository)? Яку проблему він вирішує в архітектурі застосунку?

---

### Перелік посилань

1. Horstmann, C. S. *Core Java, Volume II — Advanced Features* (12th ed.). Hoboken : Pearson, 2022. 832 p.
2. Schildt, H. *Java: The Complete Reference* (12th ed.). New York : McGraw-Hill, 2021. 1248 p.
3. Oracle Corporation. *JDBC Database Access — Java Tutorials* [Електронний ресурс]. URL: https://docs.oracle.com/javase/tutorial/jdbc/ (дата звернення: 2025-09-01).
4. Oracle Corporation. *java.sql Package API* [Електронний ресурс]. URL: https://docs.oracle.com/en/java/javase/25/docs/api/java.sql/java/sql/package-summary.html (дата звернення: 2025-09-01).
5. SQLite Consortium. *SQLite Documentation* [Електронний ресурс]. URL: https://www.sqlite.org/docs.html (дата звернення: 2025-09-01).
6. Bloch, J. *Effective Java* (3rd ed.). Boston : Addison-Wesley, 2018. 412 p.
7. Fowler, M. *Patterns of Enterprise Application Architecture*. Boston : Addison-Wesley, 2002. 560 p.

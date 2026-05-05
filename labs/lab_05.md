# Лабораторна робота №5
## Файли. Потоки введення/виведення

---

### Тема

Робота з файловою системою та потоками введення/виведення у Java: пакети `java.io` та `java.nio.file`.

---

### Мета

Набути практичних навичок читання та запису файлів, серіалізації об'єктів та роботи з файловою системою засобами пакетів `java.io` та `java.nio.file` мови Java шляхом реалізації програм обробки файлових даних.

---

### Теоретичні відомості

#### Ієрархія потоків у `java.io`

Потоки поділяються на **байтові** та **символьні**:

| Тип | Вхідні | Вихідні |
|-----|--------|---------|
| Байтові | `InputStream` | `OutputStream` |
| Символьні | `Reader` | `Writer` |

Ключові реалізації:
- `FileInputStream` / `FileOutputStream` — байтовий файловий I/O
- `FileReader` / `FileWriter` — символьний файловий I/O
- `BufferedReader` / `BufferedWriter` — буферизовані обгортки для ефективного I/O
- `PrintWriter` — зручне форматоване виведення у файл

#### `java.nio.file` — сучасний API

Починаючи з Java 7, рекомендується використовувати `java.nio.file`:

```java
import java.nio.file.*;
import java.io.IOException;
import java.util.List;

Path path = Path.of("data", "readings.csv"); // Path.of замість new File()

// Запис усіх рядків одразу
List<String> lines = List.of("час,температура", "10:00,23.5", "11:00,24.1");
Files.write(path, lines, StandardCharsets.UTF_8);

// Читання усіх рядків
List<String> read = Files.readAllLines(path, StandardCharsets.UTF_8);

// Читання у рядок
String content = Files.readString(path); // Java 11+

// Запис рядка
Files.writeString(path, "Новий вміст", StandardOpenOption.APPEND);
```

#### `BufferedReader` з `try-with-resources`

```java
Path path = Path.of("log.txt");
try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
    String line;
    while ((line = reader.readLine()) != null) {
        System.out.println(line);
    }
} catch (IOException e) {
    System.err.println("Помилка читання: " + e.getMessage());
}
```

Конструкція `try-with-resources` гарантує закриття ресурсу навіть при виникненні виключення.

#### Файлова система: `Path` та `Files`

```java
Path dir = Path.of("output");
Files.createDirectories(dir);              // створити директорії рекурсивно
Files.exists(dir);                         // перевірка існування
Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
Files.move(src, dst);
Files.delete(path);
long size = Files.size(path);              // розмір у байтах

// Перебір файлів у директорії
try (Stream<Path> entries = Files.list(dir)) {
    entries.filter(p -> p.toString().endsWith(".csv"))
           .forEach(System.out::println);
}
```

#### Серіалізація об'єктів

```java
import java.io.*;

// Клас для серіалізації повинен реалізовувати Serializable
public record SensorReading(String id, double value) implements Serializable {}

// Серіалізація (запис у файл)
try (ObjectOutputStream oos = new ObjectOutputStream(
        new FileOutputStream("reading.ser"))) {
    oos.writeObject(new SensorReading("T-01", 36.6));
}

// Десеріалізація (читання з файлу)
try (ObjectInputStream ois = new ObjectInputStream(
        new FileInputStream("reading.ser"))) {
    SensorReading r = (SensorReading) ois.readObject();
    System.out.println(r);
}
```

#### Формат CSV

CSV (*Comma-Separated Values*) — текстовий формат для табличних даних. Кожен рядок — рядок таблиці; поля розділені комою. Перший рядок зазвичай — заголовок.

#### Приклад з репозиторію java-iot

Клас [`CsvDataLogger.java`](https://github.com/AndriyLutskiv/java-iot/blob/main/projects/p01-thermometer/src/main/java/ua/crowpi/projects/p01/CsvDataLogger.java) записує показання датчиків у CSV-файл за допомогою `PrintWriter` та `BufferedWriter`. Клас [`EventFileLogger.java`](https://github.com/AndriyLutskiv/java-iot/blob/main/projects/p02-counter/src/main/java/ua/crowpi/projects/p02/EventFileLogger.java) реалізує запис подій у текстовий журнал. Клас [`HtmlReportGenerator.java`](https://github.com/AndriyLutskiv/java-iot/blob/main/projects/p10-meteostation/src/main/java/ua/crowpi/projects/p10/HtmlReportGenerator.java) генерує HTML-файл з результатами вимірювань. Рекомендується проаналізувати ці класи перед виконанням завдання.

---

### Хід виконання роботи

1. Відкрити та проаналізувати `CsvDataLogger.java` та `EventFileLogger.java` (посилання вище). Визначити клас потоку, що використовується, спосіб закриття ресурсів, кодування символів.
2. Створити тестовий файл `test_data.txt` з 10–20 рядками тексту (власне ім'я, дата, числа) засобами `Files.writeString`.
3. Написати метод читання файлу рядок за рядком через `BufferedReader`. Підрахувати кількість рядків, слів та символів.
4. Реалізувати завдання відповідно до свого варіанту.
5. Виконати запис даних у CSV-файл (`data.csv`) та зворотнє читання. Переконатися, що кодування UTF-8 зберігається (перевірити кирилицю).
6. Продемонструвати роботу `Files.copy`, `Files.move`, `Files.delete`. Створити директорію `output/`, скопіювати туди файл, перейменувати.
7. Реалізувати серіалізацію та десеріалізацію об'єктів відповідно до варіанту.
8. Перехопити та обробити виключення `IOException` та `FileNotFoundException`. Вивести інформативне повідомлення замість стек-трейсу.
9. Написати 3 JUnit-тести з використанням `@TempDir` (анотація JUnit 5 для тимчасових директорій) для тестування файлових операцій.
10. Оформити звіт: вихідний код, вміст створених файлів (знімки екрану або вставлений текст), результати тестування.

---

### Індивідуальні завдання

Розробити програму (2–3 класи), що реалізує файловий I/O відповідно до варіанту. Обсяг: до 150 рядків коду. Обов'язково: `try-with-resources`, обробка `IOException`, UTF-8 кодування.

**Варіант 1.** Реалізувати клас `StudentGradeBook`, що зберігає оцінки студентів у CSV-файлі (стовпці: прізвище, ім'я, дисципліна, оцінка). Методи: `addRecord`, `readAll`, `findByStudent(String lastName)`, `averageBySubject(String subject)`.

**Варіант 2.** Реалізувати клас `TemperatureLogger` (натхненний [`CsvDataLogger.java`](https://github.com/AndriyLutskiv/java-iot/blob/main/projects/p01-thermometer/src/main/java/ua/crowpi/projects/p01/CsvDataLogger.java)): записує показання з міткою часу у CSV. Методи: `append(double temp)`, `readLast(int n)`, `getAverage()`. Перевірити роботу з кирилицею у полі коментаря.

**Варіант 3.** Реалізувати клас `ConfigManager`, що читає та записує конфігурацію з `.properties`-файлу. Методи: `get(String key)`, `set(String key, String value)`, `save()`, `load()`. Зберігати шлях до файлу у полі класу.

**Варіант 4.** Реалізувати клас `EventLogger` (натхненний [`EventFileLogger.java`](https://github.com/AndriyLutskiv/java-iot/blob/main/projects/p02-counter/src/main/java/ua/crowpi/projects/p02/EventFileLogger.java)): записує повідомлення у файл журналу з рівнями INFO, WARN, ERROR та мітками часу. Методи: `info`, `warn`, `error`. Записувати у режимі додавання (`APPEND`).

**Варіант 5.** Реалізувати клас `HtmlTableGenerator` (натхненний [`HtmlReportGenerator.java`](https://github.com/AndriyLutskiv/java-iot/blob/main/projects/p10-meteostation/src/main/java/ua/crowpi/projects/p10/HtmlReportGenerator.java)): приймає список рядків (масиви `String[]`), заголовок та генерує HTML-файл з таблицею. Файл зберегти у кодуванні UTF-8 з метатегом `charset=UTF-8`.

**Варіант 6.** Реалізувати клас `FileWordCount`, що читає текстовий файл (можна використовувати `Files.lines` зі `Stream`) та підраховує частоту кожного слова. Результат записати у інший файл у форматі `слово: кількість`, відсортований за зменшенням частоти.

**Варіант 7.** Реалізувати клас `DirectoryScanner`, що рекурсивно обходить директорію (`Files.walk`), підраховує файли за розширенням та виводить статистику. Результат записати у JSON-подібний текстовий файл.

**Варіант 8.** Реалізувати клас `SerializableSensorData` із серіалізацією: клас `SensorSnapshot implements Serializable` зберігає ідентифікатор, масив показань та мітку часу. Методи: `save(Path p)` та статичний `load(Path p)`. Демонстрація: зберегти → завантажити → порівняти дані.

**Варіант 9.** Реалізувати клас `MultiFileMerger`, що читає всі `.txt`-файли з вказаної директорії та об'єднує їх у один файл `merged.txt`. Між вмістом кожного файлу вставляти роздільник з ім'ям файлу-джерела.

**Варіант 10.** Реалізувати клас `CsvToHtmlConverter`: читає CSV-файл з рядком-заголовком та перетворює його у HTML-таблицю з підтримкою кирилиці. Клас `Row` для зберігання рядків CSV.

**Варіант 11.** Реалізувати клас `LineNumberedFile`: записує будь-який текстовий файл з доданими номерами рядків у форматі `001: текст рядка`. Читає вихідний файл рядок за рядком через `BufferedReader`, записує через `BufferedWriter`.

**Варіант 12.** Реалізувати клас `BinaryFileAnalyzer`, що читає файл байт за байтом (`FileInputStream`), підраховує частоту кожного значення байта (0–255) та виводить топ-10 найчастіших байтів у шістнадцятковому форматі.

**Варіант 13.** Реалізувати клас `FileDiff`, що порівнює два текстові файли рядок за рядком та виводить відмінності у форматі `уніфікованого diff` (`+ рядок тільки у файлі 2`, `- рядок тільки у файлі 1`).

**Варіант 14.** Реалізувати клас `DataArchiver` з методами: `compress(Path src, Path dst)` — перетворює текстові рядки у `GZIPOutputStream`; `decompress(Path src, Path dst)` — зворотнє. Порівняти розміри файлів до та після.

**Варіант 15.** Реалізувати клас `PropertyFileEditor` з підтримкою списків: значення може бути комою-роздільним списком (`key=val1,val2,val3`). Методи: `getList(String key)`, `setList(String key, List<String> values)`. Зберегти у `.properties`-файл.

---

### Контрольні запитання

1. Яка різниця між байтовими (`InputStream`/`OutputStream`) та символьними (`Reader`/`Writer`) потоками? Коли слід використовувати кожен тип?
2. Чому при роботі з текстовими файлами важливо явно вказувати кодування (`StandardCharsets.UTF_8`)? Що відбувається, якщо кодування не вказано?
3. Що таке `try-with-resources`? Який інтерфейс повинен реалізовувати ресурс для використання у цій конструкції?
4. Навіщо використовується `BufferedReader`/`BufferedWriter`? Як буферизація впливає на продуктивність?
5. Яка різниця між `Path.of` та `new File`? Чому `Path` є кращим вибором у сучасному Java-коді?
6. Що таке серіалізація? Яку роль відіграє `serialVersionUID`?
7. Що таке `transient` поле? Чому деякі поля не варто серіалізувати?
8. Чим відрізняється `Files.write` від `Files.writeString`? Які `StandardOpenOption` підтримуються?
9. Що робить `Files.walk` і яку структуру даних вона повертає? Чим відрізняється від `Files.list`?
10. Як налаштувати `PrintWriter`, щоб він автоматично скидав буфер після кожного `println`?
11. Що таке `RandomAccessFile`? У яких сценаріях він є необхідним?
12. Що таке `FileChannel` та `MappedByteBuffer`? Яку перевагу вони дають при роботі з великими файлами?
13. Як перехопити одразу кілька різних типів виключень в одному блоці `catch`?
14. Що робить `Files.lines(Path)` і яку перевагу має перед `readAllLines` при роботі з великими файлами?
15. Як програмно перевірити, чи є шлях файлом, директорією або символьним посиланням?

---

### Перелік посилань

1. Horstmann, C. S. *Core Java, Volume II — Advanced Features* (12th ed.). Hoboken : Pearson, 2022. 832 p.
2. Schildt, H. *Java: The Complete Reference* (12th ed.). New York : McGraw-Hill, 2021. 1248 p.
3. Oracle Corporation. *Basic I/O — Java Tutorials* [Електронний ресурс]. URL: https://docs.oracle.com/javase/tutorial/essential/io/ (дата звернення: 2025-09-01).
4. Oracle Corporation. *java.nio.file.Files API* [Електронний ресурс]. URL: https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/nio/file/Files.html (дата звернення: 2025-09-01).
5. Oracle Corporation. *File I/O (Featuring NIO.2)* [Електронний ресурс]. URL: https://docs.oracle.com/javase/tutorial/essential/io/fileio.html (дата звернення: 2025-09-01).
6. Bloch, J. *Effective Java* (3rd ed.). Boston : Addison-Wesley, 2018. 412 p.
7. Deitel, P., Deitel, H. *Java: How to Program* (11th ed.). Hoboken : Pearson, 2020. 1040 p.

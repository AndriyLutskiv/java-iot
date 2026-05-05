# Лабораторна робота №4
## Операції зі стрічками

---

### Тема

Операції зі стрічками (рядками): клас `String`, клас `StringBuilder`, регулярні вирази та форматування тексту в Java.

---

### Мета

Набути практичних навичок опрацювання рядкових даних засобами стандартної бібліотеки Java шляхом реалізації програм, що використовують методи класу `String`, `StringBuilder`, `StringJoiner`, текстові блоки та регулярні вирази.

---

### Теоретичні відомості

#### Клас `String`

Рядок у Java є об'єктом класу `java.lang.String`. Рядки є **незмінними** (*immutable*): будь-яка операція, що «змінює» рядок, насправді створює новий об'єкт.

```java
String s = "Привіт, світе!";

// Основні методи
int len    = s.length();           // 14
char c     = s.charAt(0);          // 'П'
String sub = s.substring(8, 13);   // "світ"
String up  = s.toUpperCase();      // "ПРИВІТ, СВІТЕ!"
String tr  = "  Java  ".strip();   // "Java" (аналог trim(), Unicode-безпечний)
boolean sw = s.startsWith("Прив"); // true
int idx    = s.indexOf("світ");    // 8
String rep = s.replace("світе", "Java"); // "Привіт, Java!"
String[] parts = "a,b,c".split(","); // ["a", "b", "c"]
boolean eq = "hello".equalsIgnoreCase("HELLO"); // true
```

**Важливо:** для порівняння рядків завжди використовувати `equals()` або `equalsIgnoreCase()`, а не `==` (оператор `==` порівнює посилання, а не вміст).

#### Пул рядків (String Pool)

Рядкові літерали зберігаються у пулі рядків (`String Pool`) у купі. Два однакові літерали вказують на один об'єкт. Метод `intern()` дозволяє помістити рядок у пул вручну.

#### Текстові блоки (Text Blocks, Java 15+)

```java
String json = """
    {
        "sensor": "DHT11",
        "temperature": 23.5
    }
    """;
```

Відступи обрізаються автоматично. Текстові блоки зручні для JSON, SQL, HTML у коді.

#### `StringBuilder` та `StringBuffer`

`StringBuilder` — змінний (mutable) аналог `String`; ефективний для накопичення рядків у циклі:

```java
StringBuilder sb = new StringBuilder();
for (int i = 1; i <= 5; i++) {
    sb.append("Рядок ").append(i).append('\n');
}
String result = sb.toString();
```

`StringBuffer` — потокобезпечний (thread-safe) варіант `StringBuilder`; повільніший через синхронізацію.

#### Форматування: `String.format` та `formatted`

```java
String msg = String.format("Температура: %.1f°C, Вологість: %d%%", 23.5, 65);
// Або через instance-метод (Java 15+):
String msg2 = "Значення: %d".formatted(42);
```

#### `StringJoiner` та `String.join`

```java
// Об'єднання з роздільником
String joined = String.join(", ", "Java", "Python", "C++"); // "Java, Python, C++"

// З префіксом та суфіксом
StringJoiner sj = new StringJoiner(", ", "[", "]");
sj.add("Alpha").add("Beta").add("Gamma");
System.out.println(sj); // [Alpha, Beta, Gamma]
```

#### Регулярні вирази

```java
import java.util.regex.*;

String text = "Код: A-123, Серійний: B-456";

// Перевірка відповідності
boolean match = "abc123".matches("[a-z]+\\d+"); // true

// Пошук усіх збігів
Pattern p = Pattern.compile("[A-Z]-\\d+");
Matcher m = p.matcher(text);
while (m.find()) {
    System.out.println(m.group()); // A-123, потім B-456
}

// Заміна
String clean = text.replaceAll("\\d+", "***"); // маскування чисел
```

#### Приклад з репозиторію java-iot

Клас [`IrCodeDecoder.java`](https://github.com/AndriyLutskiv/java-iot/blob/main/projects/p03-music-box/src/main/java/ua/crowpi/projects/p03/IrCodeDecoder.java) виконує розбір бітових послідовностей ІЧ-сигналу у рядкові коди команд. Клас [`MelodyLibrary.java`](https://github.com/AndriyLutskiv/java-iot/blob/main/projects/p03-music-box/src/main/java/ua/crowpi/projects/p03/MelodyLibrary.java) зберігає мелодії у текстовому форматі та розбирає їх у ноти. Рекомендується проаналізувати, як у цих класах використовуються операції зі стрічками.

---

### Хід виконання роботи

1. Ознайомитися з класами `IrCodeDecoder.java` та `MelodyLibrary.java` (посилання вище). Визначити, які методи `String`/`StringBuilder` у них застосовано.
2. Написати метод, що демонструє роботу щонайменше 10 методів класу `String`. Виводити вхідний рядок, назву методу та результат.
3. Порівняти продуктивність конкатенації через `+` та через `StringBuilder` у циклі 100 000 ітерацій. Використати `System.nanoTime()` для вимірювання. Зробити висновки.
4. Реалізувати завдання відповідно до свого варіанту.
5. Реалізувати валідацію вхідних даних за допомогою регулярних виразів (наприклад, email, номер телефону, IP-адреса або дата — залежно від варіанту).
6. Продемонструвати текстові блоки: оголосити рядок у форматі JSON або SQL без екранування, вивести на екран.
7. Реалізувати метод форматування з `String.format` та `StringJoiner`.
8. Реалізувати власний метод `reverse(String s)` без використання `StringBuilder.reverse()`. Порівняти результати з вбудованим методом.
9. Написати 3 JUnit-тести для реалізованих методів рядкової обробки.
10. Оформити звіт: вихідний код, порівняльна таблиця продуктивності, результати тестування.

---

### Індивідуальні завдання

Розробити програму (2–3 класи або один складний клас), що виконує обробку рядкових даних. Обсяг: до 150 рядків коду.

**Варіант 1.** Реалізувати клас `TextStatistics`, що приймає текст і обчислює: кількість слів, речень, символів (без пробілів), кількість кожної голосної літери. Слова розбивати методом `split` за пробілами та розділовими знаками.

**Варіант 2.** Реалізувати клас `PasswordValidator` з методами: `isStrong(String pwd)` — пароль ≥8 символів, має великі, малі, цифри та спецсимвол; `generateMask(String pwd)` — маскує всі символи крім першого та останнього. Використати регулярні вирази.

**Варіант 3.** Реалізувати клас `CsvParser`, що розбирає рядок у форматі CSV (з урахуванням полів у лапках, що можуть містити кому) та повертає `List<String>`. Написати зворотній метод `toCsvLine(List<String>)`.

**Варіант 4.** Реалізувати клас `TemplateFiller`, що приймає шаблон рядка з плейсхолдерами у форматі `{{key}}` та `Map<String, String>` значень, і повертає заповнений рядок. Використати `Pattern` та `Matcher` для пошуку плейсхолдерів.

**Варіант 5.** Реалізувати клас `MorseCodeConverter` з методами: `toMorse(String text)` — перетворює латинський текст у код Морзе; `fromMorse(String morse)` — зворотнє перетворення. Зберігати таблицю у `Map<Character, String>`.

**Варіант 6.** Реалізувати клас `LogParser`, що розбирає рядки лог-файлу формату `[РІВЕНЬ] ЧАС — Повідомлення`. Методи: `getLevel()`, `getTime()`, `getMessage()`. Фільтрувати за рівнем за допомогою `enum LogLevel`.

**Варіант 7.** Реалізувати клас `IrMessageDecoder` (натхненний [`IrCodeDecoder.java`](https://github.com/AndriyLutskiv/java-iot/blob/main/projects/p03-music-box/src/main/java/ua/crowpi/projects/p03/IrCodeDecoder.java)): приймає рядок із нулів і одиниць довжиною 32 символи, розбиває на 4 байти, виводить кожен у десятковому та шістнадцятковому форматі. Перевірити рядок регулярним виразом `[01]{32}`.

**Варіант 8.** Реалізувати клас `MarkdownStripper`, що видаляє Markdown-розмітку з рядка: `**текст**` → `текст`, `_текст_` → `текст`, `[текст](url)` → `текст`, заголовки `# ` → без `#`. Використати `replaceAll` з регулярними виразами.

**Варіант 9.** Реалізувати клас `VersionComparator`, що порівнює версії програм у форматі `major.minor.patch` (наприклад, `"1.10.3"` та `"1.9.99"`). Методи: `compare(String v1, String v2)` → -1, 0 або 1; `isCompatible(String v1, String v2)` — однакові `major`.

**Варіант 10.** Реалізувати клас `SqlQueryBuilder` із методами `select(String... cols)`, `from(String table)`, `where(String condition)`, `build()`. Клас накопичує частини запиту в `StringBuilder` та повертає готовий SQL-рядок. Забороняється конкатенація через `+` в циклі.

**Варіант 11.** Реалізувати клас `PhoneNumberFormatter`, що нормалізує рядки телефонних номерів різних форматів `(050) 123-45-67`, `+380501234567`, `050-123-45-67` до єдиного формату `+38 (XXX) XXX-XX-XX`. Використати регулярні вирази та `replaceAll`.

**Варіант 12.** Реалізувати клас `RomanNumeralConverter` з методами: `toRoman(int n)` — перетворює ціле (1–3999) у рядок римських цифр; `toArabic(String roman)` — зворотнє перетворення. Валідувати вхідний рядок регулярним виразом.

**Варіант 13.** Реалізувати клас `HtmlEscaper` з методами: `escape(String text)` — замінює `<`, `>`, `&`, `"`, `'` на HTML-entities; `unescape(String html)` — зворотнє. Написати тести з рядками, що містять усі спецсимволи.

**Варіант 14.** Реалізувати клас `NoteParser` (натхненний [`MelodyLibrary.java`](https://github.com/AndriyLutskiv/java-iot/blob/main/projects/p03-music-box/src/main/java/ua/crowpi/projects/p03/MelodyLibrary.java)): приймає рядок нот формату `"C4:500,D4:250,E4:250"` (нота:тривалість), повертає список записів `NoteEntry(String name, int durationMs)`. Валідувати формат регулярним виразом.

**Варіант 15.** Реалізувати клас `WordFrequencyCounter`, що підраховує частоту кожного слова у тексті (без урахування регістру та розділових знаків). Виводити топ-10 найчастіших слів у відсортованому порядку. Порівняти реалізацію на `HashMap` з реалізацією на потоках (`Stream`).

---

### Контрольні запитання

1. Чому рядки у Java є незмінними (immutable)? Які переваги це дає?
2. Чим відрізняється `==` від `equals()` при порівнянні рядків? Наведіть приклад, де `==` дає неочікуваний результат.
3. Що таке пул рядків (String Pool)? Як метод `intern()` пов'язаний з пулом?
4. Чому конкатенація рядків через `+` у циклі є неефективною? Яку альтернативу слід використовувати?
5. Чим відрізняється `StringBuilder` від `StringBuffer`? Коли слід використовувати кожен?
6. Що таке текстові блоки (text blocks)? Як обробляється відступ у текстовому блоці?
7. Що робить метод `strip()` і чим він відрізняється від `trim()`?
8. Як у Java реалізована підтримка Unicode у рядках? Що таке кодова точка (code point) та одиниця коду (code unit)?
9. Що таке регулярний вираз? Поясніть синтаксис: `[a-z]+`, `\\d{3}`, `^[A-Z].*\\.$`.
10. Що робить метод `split(String regex, int limit)` з ненульовим параметром `limit`?
11. Яка різниця між `String.format` та `formatted()`?
12. Що таке `StringJoiner`? Яку перевагу він має порівняно з `String.join`?
13. Як порівнювати рядки без урахування регістру? Чи враховує `equalsIgnoreCase` кирилицю?
14. Що таке `Matcher.group(int)` та як отримати всі групи захоплення?
15. Коли краще використовувати `contains()`, а коли `indexOf()` — наведіть відмінності.

---

### Перелік посилань

1. Horstmann, C. S. *Core Java, Volume I — Fundamentals* (12th ed.). Hoboken : Pearson, 2022. 928 p.
2. Bloch, J. *Effective Java* (3rd ed.). Boston : Addison-Wesley, 2018. 412 p.
3. Oracle Corporation. *java.lang.String API* [Електронний ресурс]. URL: https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/String.html (дата звернення: 2025-09-01).
4. Oracle Corporation. *java.lang.StringBuilder API* [Електронний ресурс]. URL: https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/StringBuilder.html (дата звернення: 2025-09-01).
5. Oracle Corporation. *Regular Expressions — Java Tutorials* [Електронний ресурс]. URL: https://docs.oracle.com/javase/tutorial/essential/regex/ (дата звернення: 2025-09-01).
6. Oracle Corporation. *Text Blocks — JEP 378* [Електронний ресурс]. URL: https://openjdk.org/jeps/378 (дата звернення: 2025-09-01).
7. Schildt, H. *Java: The Complete Reference* (12th ed.). New York : McGraw-Hill, 2021. 1248 p.
8. Friedl, J. E. F. *Mastering Regular Expressions* (3rd ed.). Sebastopol : O'Reilly Media, 2006. 542 p.

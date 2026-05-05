# Лабораторна робота №8
## Використання Java для створення мережевих програм

---

### Тема

Використання Java для створення мережевих програм: TCP/IP-сокети, HTTP-клієнти та основи мережевого програмування.

---

### Мета

Набути практичних навичок розробки мережевих застосунків мовою Java шляхом реалізації TCP-клієнта та серверу, а також HTTP-клієнта з використанням стандартного API `java.net.http`.

---

### Теоретичні відомості

#### Основи мережевої моделі

Мережева комунікація Java базується на стеку протоколів **TCP/IP**:
- **IP** (Internet Protocol) — адресація хостів (IPv4: 192.168.1.1; IPv6: ::1).
- **TCP** (Transmission Control Protocol) — надійна, орієнтована на з'єднання передача; гарантує доставку та порядок пакетів.
- **UDP** (User Datagram Protocol) — ненадійна, без з'єднання; швидша, але без гарантій.
- **Порт** — 16-бітний ідентифікатор (0–65 535) для розрізнення сервісів на одному хості. Зарезервовані: 0–1023; загальновідомі: HTTP 80, HTTPS 443, FTP 21.

#### TCP-сервер (`ServerSocket`)

```java
import java.net.*;
import java.io.*;

public class EchoServer {
    public static void main(String[] args) throws IOException {
        int port = 9000;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Сервер слухає порт " + port);
            while (true) {
                // Очікування підключення клієнта (блокуюча операція)
                Socket client = serverSocket.accept();
                System.out.println("Підключено: " + client.getInetAddress());

                // Обробка у потоці, щоб не блокувати прийняття нових з'єднань
                new Thread(() -> handleClient(client)).start();
            }
        }
    }

    private static void handleClient(Socket socket) {
        try (socket;
             BufferedReader in = new BufferedReader(
                 new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(
                 new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {

            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("Отримано: " + line);
                out.println("Ехо: " + line); // відправити назад
            }
        } catch (IOException e) {
            System.err.println("Помилка клієнта: " + e.getMessage());
        }
    }
}
```

#### TCP-клієнт (`Socket`)

```java
public class EchoClient {
    public static void main(String[] args) throws IOException {
        try (Socket socket = new Socket("localhost", 9000);
             PrintWriter out = new PrintWriter(
                 new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
             BufferedReader in = new BufferedReader(
                 new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedReader console = new BufferedReader(new InputStreamReader(System.in))) {

            String input;
            System.out.println("Підключено до сервера. Введіть повідомлення (exit — вихід):");
            while (!(input = console.readLine()).equals("exit")) {
                out.println(input);
                System.out.println("Сервер відповів: " + in.readLine());
            }
        }
    }
}
```

#### HTTP-клієнт (`java.net.http`, Java 11+)

```java
import java.net.http.*;
import java.net.URI;
import java.time.Duration;

HttpClient client = HttpClient.newBuilder()
    .connectTimeout(Duration.ofSeconds(10))
    .build();

// GET-запит
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("https://httpbin.org/get"))
    .header("Accept", "application/json")
    .GET()
    .build();

HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
System.out.println("Статус: " + response.statusCode());
System.out.println("Тіло: " + response.body());

// POST-запит
String json = "{\"sensor\": \"T-01\", \"value\": 23.5}";
HttpRequest postRequest = HttpRequest.newBuilder()
    .uri(URI.create("https://httpbin.org/post"))
    .header("Content-Type", "application/json")
    .POST(HttpRequest.BodyPublishers.ofString(json))
    .build();

HttpResponse<String> postResponse = client.send(postRequest, HttpResponse.BodyHandlers.ofString());
```

#### Асинхронний HTTP (sendAsync)

```java
client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
    .thenApply(HttpResponse::body)
    .thenAccept(System.out::println)
    .join(); // дочекатися завершення
```

#### UDP (датаграми)

```java
// Сервер UDP
DatagramSocket serverSocket = new DatagramSocket(9001);
byte[] buffer = new byte[1024];
DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
serverSocket.receive(packet); // блокування до отримання
String msg = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
```

#### Приклад з репозиторію java-iot

Клас [`NbuApiClient.java`](https://github.com/AndriyLutskiv/java-iot/blob/main/projects/p12-currency-clock/src/main/java/ua/crowpi/projects/p12/NbuApiClient.java) визначає інтерфейс для HTTP-клієнта НБУ. Клас [`RealNbuApiClient.java`](https://github.com/AndriyLutskiv/java-iot/blob/main/projects/p12-currency-clock/src/main/java/ua/crowpi/projects/p12/RealNbuApiClient.java) реалізує запит до реального REST API. Клас [`MockNbuApiClient.java`](https://github.com/AndriyLutskiv/java-iot/blob/main/projects/p12-currency-clock/src/main/java/ua/crowpi/projects/p12/MockNbuApiClient.java) — тестова заглушка. Рекомендується вивчити патерн розділення інтерфейсу та реалізації для тестованого HTTP-клієнта.

---

### Хід виконання роботи

1. Відкрити та проаналізувати `NbuApiClient.java`, `RealNbuApiClient.java` та `MockNbuApiClient.java` (посилання вище). Визначити: який HTTP-метод використовується, як обробляється JSON-відповідь, як тестується код без реальної мережі.
2. Реалізувати простий TCP-луна-сервер (`EchoServer`): приймає підключення, читає рядки та повертає їх назад із префіксом.
3. Реалізувати TCP-клієнт (`EchoClient`): підключається до серверу, відправляє введені з консолі рядки та виводить відповідь.
4. Запустити сервер та клієнт одночасно (два термінальних вікна або `Thread`). Переконатися у двосторонньому обміні.
5. Реалізувати завдання відповідно до свого варіанту.
6. Реалізувати HTTP GET-запит за допомогою `java.net.http.HttpClient`. Якщо варіант передбачає публічне API — перевірити реальну відповідь; якщо ні — використати `https://httpbin.org` або `https://jsonplaceholder.typicode.com`.
7. Обробити відповідь: розібрати JSON-рядок власноруч (методами `String.indexOf`, `substring`) або використати бібліотеку Gson/Jackson (якщо дозволяє конфігурація проєкту).
8. Реалізувати обробку мережевих виключень: `ConnectException`, `SocketTimeoutException`, `HttpTimeoutException`. Вивести інформативне повідомлення.
9. Написати тест із `MockNbuApiClient`-підходом: mock-клас, що повертає заготовлену JSON-відповідь, дозволяє тестувати бізнес-логіку без мережі.
10. Виміряти час відповіді 5 HTTP-запитів (послідовних та асинхронних через `sendAsync`). Порівняти результати.
11. Оформити звіт: вихідний код сервера та клієнта, знімки виводу при роботі, обговорення мережевих виключень.

---

### Індивідуальні завдання

Розробити мережевий застосунок (3–4 класи). Обсяг: до 250 рядків коду. Обов'язково: обробка виключень, підтримка кирилиці (UTF-8).

**Варіант 1.** Реалізувати «Чат» у CLI: TCP-сервер, що підтримує до 5 одночасних клієнтів. Кожне повідомлення від клієнта транслюється всім іншим із зазначенням імені відправника. Ім'я клієнт надсилає першим рядком при підключенні.

**Варіант 2.** Реалізувати HTTP-клієнт для API НБУ (за зразком [`RealNbuApiClient.java`](https://github.com/AndriyLutskiv/java-iot/blob/main/projects/p12-currency-clock/src/main/java/ua/crowpi/projects/p12/RealNbuApiClient.java)): GET-запит до `https://bank.gov.ua/NBUStatService/v1/statdatacommon/nbuviewing?fmt=json`, розбір JSON-відповіді (рядками), вивід курсів USD, EUR, GBP.

**Варіант 3.** Реалізувати HTTP-клієнт для відкритого API `https://jsonplaceholder.typicode.com/posts`: отримати список постів (GET), вивести перші 5. Реалізувати POST-запит для створення нового поста. Вивести статус відповіді та тіло.

**Варіант 4.** Реалізувати TCP-сервер «Вікторина»: при підключенні клієнт отримує питання, надсилає відповідь, сервер перевіряє та відповідає «Правильно!» або «Неправильно. Правильна відповідь: ...». Після 5 питань — підсумковий рахунок.

**Варіант 5.** Реалізувати клієнт-серверний «Словник»: TCP-сервер зберігає `Map<String, String>` (слово → переклад). Клієнт надсилає слово, сервер повертає переклад або «Не знайдено». Підтримати команди `ADD слово переклад` та `DEL слово`.

**Варіант 6.** Реалізувати HTTP-клієнт для відкритого прогнозу погоди: API `https://wttr.in/<місто>?format=j1` повертає JSON. Розібрати рядками та вивести температуру, опис погоди, вологість для Тернополя або введеного міста.

**Варіант 7.** Реалізувати TCP «Файловий сервер»: клієнт надсилає ім'я файлу → сервер читає файл і надсилає вміст байт за байтом через `InputStream`. Клієнт зберігає отримані байти у файл. Передбачити обробку відсутнього файлу.

**Варіант 8.** Реалізувати HTTP-клієнт для API GitHub: GET-запит до `https://api.github.com/repos/<owner>/<repo>` (публічний репозиторій). Вивести: назву, опис, кількість зірок, мову. Параметри репо — аргументи `main`.

**Варіант 9.** Реалізувати UDP-застосунок «Датчик — Реєстратор»: клієнт надсилає UDP-датаграму з показанням (рядок `"T:23.5 H:65"`), сервер отримує, зберігає у `List` та виводить на консоль. Після 10 датаграм — вивести статистику.

**Варіант 10.** Реалізувати TCP-сервер «Калькулятор»: клієнт надсилає вираз у рядку (`"3 + 4"`, `"10 / 2"`), сервер обчислює та повертає результат. Підтримати +, -, *, /. Обробити ділення на нуль.

**Варіант 11.** Реалізувати HTTP-клієнт для перевірки IP-геолокації: GET-запит до `https://ipinfo.io/{ip}/json` (публічне API). Розібрати відповідь та вивести: місто, регіон, країну, провайдера. Підтримати введення IP як аргумент `main`.

**Варіант 12.** Реалізувати «Менеджер задач» на TCP: сервер зберігає список `Task(id, title, done)`. Клієнт підтримує команди: `LIST`, `ADD назва`, `DONE id`, `DELETE id`. Відповіді сервера — рядки у форматі CSV.

**Варіант 13.** Реалізувати HTTP-клієнт для `https://jsonplaceholder.typicode.com/users`: завантажити список користувачів (GET), вивести таблицю (id, ім'я, email, місто). Виконати PUT-запит для оновлення першого користувача та перевірити відповідь.

**Варіант 14.** Реалізувати TCP «Нотатки» з трьома паралельними клієнтами: `Map<String, String>` (ключ → нотатка). Команди: `PUT key value`, `GET key`, `LIST`. Захистити `Map` від конкурентного доступу через `ConcurrentHashMap`.

**Варіант 15.** Реалізувати HTTP-клієнт для REST API `https://restcountries.com/v3.1/name/{country}`: запит за назвою країни (аргумент `main`). Вивести: офіційна назва, столиця, населення, регіон. Обробити 404 (країна не знайдена).

---

### Контрольні запитання

1. Яка різниця між TCP та UDP? Наведіть приклад задачі, де доцільний кожен протокол.
2. Що таке сокет? Яку пару параметрів однозначно ідентифікує TCP-з'єднання?
3. Що таке `ServerSocket.accept()`? Чому цей виклик є блокуючим?
4. Навіщо обробляти кожне з'єднання у окремому потоці? Які ризики пов'язані з цим підходом?
5. Що таке `SocketTimeoutException`? Як встановити тайм-аут очікування підключення та читання?
6. Яке призначення `HttpClient` у `java.net.http`? Чим він кращий за `HttpURLConnection`?
7. Яка різниця між синхронним `send` та асинхронним `sendAsync` у `HttpClient`?
8. Що таке `BodyPublisher` та `BodyHandler`? Для чого вони потрібні?
9. Що таке REST API? Яку роль відіграють HTTP-методи GET, POST, PUT, DELETE?
10. Що таке JSON? Наведіть приклад структури JSON-об'єкта та JSON-масиву.
11. Що таке `ConnectException` та коли вона виникає?
12. Яка різниця між `InetAddress` та `URI`? Для яких задач вони використовуються?
13. Що таке `DatagramSocket` та `DatagramPacket`? Як надіслати UDP-повідомлення?
14. Що таке `localhost` та `loopback`-адреса? Як пов'язані `127.0.0.1` та `::1`?
15. Що таке HTTP-заголовки (headers)? Яку роль відіграють `Content-Type` та `Accept`?

---

### Перелік посилань

1. Horstmann, C. S. *Core Java, Volume II — Advanced Features* (12th ed.). Hoboken : Pearson, 2022. 832 p.
2. Schildt, H. *Java: The Complete Reference* (12th ed.). New York : McGraw-Hill, 2021. 1248 p.
3. Oracle Corporation. *Custom Networking — Java Tutorials* [Електронний ресурс]. URL: https://docs.oracle.com/javase/tutorial/networking/ (дата звернення: 2025-09-01).
4. Oracle Corporation. *java.net.http — HTTP Client API* [Електронний ресурс]. URL: https://docs.oracle.com/en/java/javase/25/docs/api/java.net.http/java/net/http/package-summary.html (дата звернення: 2025-09-01).
5. Kurose, J. F., Ross, K. W. *Computer Networking: A Top-Down Approach* (8th ed.). Hoboken : Pearson, 2021. 856 p.
6. Richardson, L., Ruby, S. *RESTful Web Services*. Sebastopol : O'Reilly Media, 2007. 448 p.
7. Tanenbaum, A. S., Wetherall, D. J. *Computer Networks* (5th ed.). Upper Saddle River : Pearson, 2011. 960 p.

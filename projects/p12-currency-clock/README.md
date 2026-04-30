# p12 — Currency & Clock Display

Alternates a 16×2 I²C LCD between two pages, spending 5 seconds on each:

| Page     | Row 0                | Row 1                |
|----------|----------------------|----------------------|
| Clock    | `Time: 14:35:22  `   | `Date: 22.03.2026`   |
| Currency | `USD: 41.23 UAH  `   | `EUR: 44.56 UAH  `   |

Exchange rates are fetched live from the **National Bank of Ukraine (NBU) REST API**
each time the currency page becomes active.

## Hardware wiring

| Component      | Pin / Bus       | Notes                          |
|----------------|-----------------|--------------------------------|
| LCD (I²C)      | SDA/SCL (I²C-1) | Address 0x27 (PCF8574 backpack)|

Only the LCD is required — no GPIO pins are used.

## NBU API

```
GET https://bank.gov.ua/NBU_Exchange/exchange?json
```

Returns a JSON array of all official exchange rates for today.  Example entry:

```json
{"r030":840,"txt":"Долар США","rate":41.2345,"cc":"USD","exchangedate":"22.03.2026"}
```

The project filters for `cc == "USD"` and `cc == "EUR"` and displays each rate
formatted to 2 decimal places.

If the API call fails (network unavailable, timeout), the last successfully fetched
rates are shown.  If no fetch has ever succeeded, `N/A` is displayed.

## Display cycle

```
┌─ 5 s ──────────┐    ┌─ 5 s ──────────┐
│  CLOCK          │    │  CURRENCY       │
│  Time: 14:35:22 │◄──►│  USD: 41.23 UAH│
│  Date: 22.03.26 │    │  EUR: 44.56 UAH│
└─────────────────┘    └─────────────────┘
         ↑ repeats indefinitely ↑
```

The clock page refreshes every second.  The currency page is static for its 5-second
window; rates are re-fetched on the next currency-page entry.

## Running

```bash
# Hardware mode (Raspberry Pi with I²C LCD)
java -jar crowpi-suite-1.0.0.jar --project p12

# Mock mode (any machine — no hardware or network required)
java -jar crowpi-suite-1.0.0.jar --project p12 --mock
```

In mock mode the LCD output is printed to the console via `MockI2cFacade` and
static rates (USD = 41.23, EUR = 44.56) are returned by `MockNbuApiClient`.

## Key classes

| Class                 | Responsibility                                              |
|-----------------------|-------------------------------------------------------------|
| `CurrencyRate`        | Jackson POJO for one NBU API array entry                   |
| `NbuApiClient`        | Interface: `fetchRates() → List<CurrencyRate>`             |
| `RealNbuApiClient`    | `java.net.http.HttpClient` + Jackson; 10 s timeout         |
| `MockNbuApiClient`    | Returns static USD/EUR rates without network access        |
| `DisplayController`   | 1-second tick driver; manages mode switching and LCD writes|
| `ClockPage`           | Static helpers: `formatRow0(LocalTime)`, `formatRow1(LocalDate)` |
| `CurrencyPage`        | Static helpers: `formatRow0/1(List<CurrencyRate>)`         |
| `DisplayMode`         | Enum: `CLOCK`, `CURRENCY`                                  |
| `CurrencyClockProject`| `CrowPiProject` entry point; wires DI and starts scheduler |

## Unit tests (9 tests)

| Test                                       | Covers                                     |
|--------------------------------------------|--------------------------------------------|
| `testCurrencyRate_jacksonDeserialization`  | Jackson parses NBU JSON correctly          |
| `testClockPage_row0_is16Chars`             | Time row is exactly 16 characters          |
| `testClockPage_row1_is16Chars`             | Date row is exactly 16 characters          |
| `testCurrencyPage_formatLine_is16Chars`    | USD and EUR lines are exactly 16 characters|
| `testCurrencyPage_missingCurrency_showsNA` | Missing currency shows `N/A`               |
| `testMockNbuApiClient_returnsTwoRates`     | Mock returns exactly USD and EUR           |
| `testDisplayController_switchesAfterDisplaySeconds` | Mode switches after 5 ticks      |
| `testDisplayController_clockMode_writesLcd`| LCD `writeByte` called on clock tick       |
| `testDisplayController_currencyMode_fetchesOnce` | `fetchRates()` called once per window|

## Java 11 HTTP client

This project uses `java.net.http.HttpClient` introduced in Java 11 (JEP 321), which
provides a modern, non-blocking HTTP API without third-party dependencies:

```java
HttpClient client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("https://bank.gov.ua/NBU_Exchange/exchange?json"))
        .timeout(Duration.ofSeconds(10))
        .GET()
        .build();

HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
```

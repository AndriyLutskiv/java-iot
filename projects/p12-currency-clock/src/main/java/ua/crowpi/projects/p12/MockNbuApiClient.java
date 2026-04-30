package ua.crowpi.projects.p12;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Mock implementation of {@link NbuApiClient} for use in {@code --mock} mode and unit tests.
 *
 * <p>Returns a fixed list of two {@link CurrencyRate} entries (USD and EUR) without
 * making any network requests.  The exchange date is set to today's date at the time
 * the client is instantiated.</p>
 */
public class MockNbuApiClient implements NbuApiClient {

    /** Simulated USD/UAH rate used in mock mode. */
    public static final double MOCK_USD_RATE = 41.23;

    /** Simulated EUR/UAH rate used in mock mode. */
    public static final double MOCK_EUR_RATE = 44.56;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");

    // Дата встановлюється при створенні клієнта — відображає "сьогоднішній" курс
    private final String today = LocalDate.now().format(DATE_FMT);

    /**
     * Creates a new {@code MockNbuApiClient} with pre-defined rates.
     */
    public MockNbuApiClient() {
        // Використовуємо константи класу — не потрібно налаштування
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns two static entries: USD at {@value #MOCK_USD_RATE} UAH and
     * EUR at {@value #MOCK_EUR_RATE} UAH.  No network call is made.</p>
     *
     * @return immutable list with USD and EUR mock rates
     */
    @Override
    public List<CurrencyRate> fetchRates() throws IOException {
        // Симулюємо успішну відповідь NBU API — два записи без мережі
        return Collections.unmodifiableList(Arrays.asList(
                new CurrencyRate("USD", "Долар США",   MOCK_USD_RATE, today),
                new CurrencyRate("EUR", "Євро",        MOCK_EUR_RATE, today)
        ));
    }
}

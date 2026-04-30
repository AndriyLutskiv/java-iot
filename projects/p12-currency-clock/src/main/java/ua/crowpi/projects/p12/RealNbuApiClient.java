package ua.crowpi.projects.p12;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * Production implementation of {@link NbuApiClient} that fetches exchange rates
 * from the National Bank of Ukraine REST API using the built-in {@link HttpClient}
 * introduced in Java 11.
 *
 * <p>The API endpoint returns a JSON array of all official exchange rates for today.
 * The response is deserialized with Jackson into a {@code List<CurrencyRate>}.</p>
 *
 * <p>Both connection and read timeouts are set to {@value #TIMEOUT_SECONDS} seconds
 * to prevent blocking indefinitely on a slow network.</p>
 */
public class RealNbuApiClient implements NbuApiClient {

    /** NBU official exchange-rate JSON endpoint. */
    public static final String NBU_API_URL =
            "https://bank.gov.ua/NBU_Exchange/exchange?json";

    /** HTTP and read timeout in seconds. */
    private static final int TIMEOUT_SECONDS = 10;

    // Jackson ObjectMapper є потокобезпечним після конфігурації — безпечно ділити між тіками
    private final ObjectMapper mapper = new ObjectMapper();

    // HttpClient теж потокобезпечний та рекомендований до перевикористання між запитами
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
            .build();

    /**
     * Creates a new {@code RealNbuApiClient} with default HTTP client settings.
     */
    public RealNbuApiClient() {
        // Нічого додаткового — httpClient і mapper ініціалізовані вище як поля екземпляру
    }

    /**
     * {@inheritDoc}
     *
     * <p>Issues a synchronous HTTP GET request to the NBU API and deserializes
     * the JSON array body into a list of {@link CurrencyRate} objects.
     * If the response status is not 2xx an {@link IOException} is thrown.</p>
     *
     * @return list of all today's exchange rates provided by the NBU
     * @throws IOException if the network request fails, times out, or the
     *                     response body cannot be parsed as valid JSON
     */
    @Override
    public List<CurrencyRate> fetchRates() throws IOException {
        // Будуємо запит із таймаутом на відповідь
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(NBU_API_URL))
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response;
        try {
            // send() — синхронний виклик, блокує потік до отримання відповіді або таймауту
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            // Відновлюємо прапор переривання і перетворюємо на IOException
            Thread.currentThread().interrupt();
            throw new IOException("HTTP request was interrupted", e);
        }

        // Перевіряємо HTTP-статус — очікуємо 2xx
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException(
                    "NBU API returned HTTP " + response.statusCode() + " for " + NBU_API_URL);
        }

        // Десеріалізуємо JSON масив у List<CurrencyRate> за допомогою Jackson TypeReference
        List<CurrencyRate> rates = mapper.readValue(
                response.body(),
                new TypeReference<List<CurrencyRate>>() { }
        );

        return Collections.unmodifiableList(rates);
    }
}

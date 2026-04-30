package ua.crowpi.projects.p12;

import java.io.IOException;
import java.util.List;

/**
 * Abstraction over the National Bank of Ukraine (NBU) currency exchange-rate API.
 *
 * <p>The production implementation ({@link RealNbuApiClient}) issues an HTTP GET
 * request to {@code https://bank.gov.ua/NBU_Exchange/exchange?json} and parses the
 * returned JSON array via Jackson.  The mock implementation
 * ({@link MockNbuApiClient}) returns a static list of pre-defined rates so that
 * the project can be run and tested without network access.</p>
 */
public interface NbuApiClient {

    /**
     * Fetches the full list of today's exchange rates from the NBU.
     *
     * <p>The returned list contains one {@link CurrencyRate} entry per currency.
     * Callers that only need USD and EUR should filter the result themselves.</p>
     *
     * @return immutable list of currency rates; never {@code null}
     * @throws IOException if the request fails or the response cannot be parsed
     */
    List<CurrencyRate> fetchRates() throws IOException;
}

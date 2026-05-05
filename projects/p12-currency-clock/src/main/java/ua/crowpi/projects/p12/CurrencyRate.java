package ua.crowpi.projects.p12;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Value object that maps one entry from the NBU (National Bank of Ukraine) exchange-rate
 * JSON array to a Java object.
 *
 * <p>Example JSON entry returned by the API
 * ({@code https://bank.gov.ua/NBU_Exchange/exchange?json}):</p>
 * <pre>
 * {
 *   "r030":         840,
 *   "txt":          "Долар США",
 *   "rate":         41.2345,
 *   "cc":           "USD",
 *   "exchangedate": "22.03.2026"
 * }
 * </pre>
 *
 * <p>{@code @JsonIgnoreProperties(ignoreUnknown = true)} дозволяє ігнорувати будь-які
 * додаткові поля (наприклад {@code r030}), що можуть з'явитися в API у майбутньому.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CurrencyRate {

    /** ISO 4217 currency code, e.g. {@code "USD"} or {@code "EUR"}. */
    @JsonProperty("cc")
    private String currencyCode;

    /** Ukrainian name of the currency, e.g. {@code "Долар США"}. */
    @JsonProperty("txt")
    private String currencyName;

    /** Exchange rate of one unit of this currency against UAH (Ukrainian Hryvnia). */
    @JsonProperty("rate")
    private double rate;

    /** Date of this exchange rate in {@code "dd.MM.yyyy"} format. */
    @JsonProperty("exchangedate")
    private String exchangeDate;

    /**
     * No-argument constructor required by Jackson for deserialization.
     */
    public CurrencyRate() {
        // Порожній конструктор — Jackson заповнює поля через сеттери
    }

    /**
     * Convenience constructor for tests and mock implementations.
     *
     * @param currencyCode ISO 4217 currency code
     * @param currencyName human-readable currency name
     * @param rate         exchange rate against UAH
     * @param exchangeDate date string in {@code "dd.MM.yyyy"} format
     */
    public CurrencyRate(String currencyCode, String currencyName, double rate, String exchangeDate) {
        this.currencyCode = currencyCode;
        this.currencyName = currencyName;
        this.rate         = rate;
        this.exchangeDate = exchangeDate;
    }

    /**
     * Returns the ISO 4217 currency code.
     *
     * @return currency code, e.g. {@code "USD"}
     */
    public String getCurrencyCode() { return currencyCode; }

    /**
     * Returns the Ukrainian name of the currency.
     *
     * @return currency name in Ukrainian, e.g. {@code "Долар США"}
     */
    public String getCurrencyName() { return currencyName; }

    /**
     * Returns the exchange rate of one unit of this currency against UAH.
     *
     * @return rate in UAH
     */
    public double getRate() { return rate; }

    /**
     * Returns the date for which this rate is valid.
     *
     * @return date string in {@code "dd.MM.yyyy"} format
     */
    public String getExchangeDate() { return exchangeDate; }

    /** @param currencyCode ISO 4217 currency code */
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }

    /** @param currencyName human-readable currency name */
    public void setCurrencyName(String currencyName) { this.currencyName = currencyName; }

    /** @param rate exchange rate against UAH */
    public void setRate(double rate) { this.rate = rate; }

    /** @param exchangeDate date string in {@code "dd.MM.yyyy"} format */
    public void setExchangeDate(String exchangeDate) { this.exchangeDate = exchangeDate; }

    @Override
    public String toString() {
        return currencyCode + "=" + rate + " UAH (" + exchangeDate + ")";
    }
}

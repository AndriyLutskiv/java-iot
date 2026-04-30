package ua.crowpi.projects.p12;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ua.crowpi.core.hardware.I2cFacade;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for p12 Currency &amp; Clock Display.
 *
 * <p>Tests cover JSON deserialization, LCD line formatting, mode switching logic,
 * and LCD write verification via Mockito.</p>
 */
@ExtendWith(MockitoExtension.class)
class CurrencyClockTest {

    @Mock
    private I2cFacade mockLcd;

    @Mock
    private NbuApiClient mockApiClient;

    // =========================================================================
    // Test 1: CurrencyRate Jackson deserialization
    // =========================================================================

    @Test
    void testCurrencyRate_jacksonDeserialization() throws Exception {
        // Мінімальний JSON рядок, що імітує одну відповідь NBU API
        String json = "[{\"cc\":\"USD\",\"txt\":\"Долар США\",\"rate\":41.2345,"
                + "\"exchangedate\":\"22.03.2026\"}]";

        ObjectMapper mapper = new ObjectMapper();
        List<CurrencyRate> rates = mapper.readValue(json,
                new TypeReference<List<CurrencyRate>>() { });

        assertEquals(1, rates.size());
        assertEquals("USD",          rates.get(0).getCurrencyCode());
        assertEquals("Долар США",    rates.get(0).getCurrencyName());
        assertEquals(41.2345,        rates.get(0).getRate(), 0.0001);
        assertEquals("22.03.2026",   rates.get(0).getExchangeDate());
    }

    // =========================================================================
    // Test 2: ClockPage row 0 is exactly 16 characters
    // =========================================================================

    @Test
    void testClockPage_row0_is16Chars() {
        String row0 = ClockPage.formatRow0(LocalTime.of(14, 35, 22));
        assertEquals(16, row0.length(), "Row 0 must be exactly 16 characters");
        assertTrue(row0.startsWith("Time: 14:35:22"),
                "Row 0 must contain formatted time");
    }

    // =========================================================================
    // Test 3: ClockPage row 1 is exactly 16 characters
    // =========================================================================

    @Test
    void testClockPage_row1_is16Chars() {
        String row1 = ClockPage.formatRow1(LocalDate.of(2026, 3, 22));
        assertEquals(16, row1.length(), "Row 1 must be exactly 16 characters");
        assertTrue(row1.contains("22.03.2026"),
                "Row 1 must contain formatted date");
    }

    // =========================================================================
    // Test 4: CurrencyPage formatLine is exactly 16 characters
    // =========================================================================

    @Test
    void testCurrencyPage_formatLine_is16Chars() {
        List<CurrencyRate> rates = Arrays.asList(
                new CurrencyRate("USD", "Долар США", 41.23, "22.03.2026"),
                new CurrencyRate("EUR", "Євро",      44.56, "22.03.2026")
        );

        String usdLine = CurrencyPage.formatRow0(rates);
        String eurLine = CurrencyPage.formatRow1(rates);

        assertEquals(16, usdLine.length(), "USD line must be exactly 16 characters");
        assertEquals(16, eurLine.length(), "EUR line must be exactly 16 characters");
        assertTrue(usdLine.startsWith("USD: 41.23 UAH"), "USD line format mismatch");
        assertTrue(eurLine.startsWith("EUR: 44.56 UAH"), "EUR line format mismatch");
    }

    // =========================================================================
    // Test 5: CurrencyPage shows N/A when currency not in list
    // =========================================================================

    @Test
    void testCurrencyPage_missingCurrency_showsNA() {
        List<CurrencyRate> emptyList = Collections.emptyList();

        String usdLine = CurrencyPage.formatRow0(emptyList);

        assertEquals(16, usdLine.length(), "N/A line must still be 16 characters");
        assertTrue(usdLine.contains("N/A"), "Missing currency must show N/A");
    }

    // =========================================================================
    // Test 6: MockNbuApiClient returns USD and EUR
    // =========================================================================

    @Test
    void testMockNbuApiClient_returnsTwoRates() throws IOException {
        MockNbuApiClient client = new MockNbuApiClient();
        List<CurrencyRate> rates = client.fetchRates();

        assertEquals(2, rates.size(), "Mock client must return exactly 2 rates");

        boolean hasUsd = rates.stream()
                .anyMatch(r -> "USD".equals(r.getCurrencyCode()));
        boolean hasEur = rates.stream()
                .anyMatch(r -> "EUR".equals(r.getCurrencyCode()));

        assertTrue(hasUsd, "Mock rates must include USD");
        assertTrue(hasEur, "Mock rates must include EUR");
    }

    // =========================================================================
    // Test 7: DisplayController switches mode after DISPLAY_SECONDS ticks
    // =========================================================================

    @Test
    void testDisplayController_switchesAfterDisplaySeconds() {
        // Лише CLOCK-тіки — fetchRates() не потрібен у цьому тесті
        DisplayController ctrl = new DisplayController(mockLcd, mockApiClient);
        assertEquals(DisplayMode.CLOCK, ctrl.getCurrentMode(), "Must start in CLOCK mode");

        // Виконуємо DISPLAY_SECONDS тіків — після цього режим має перемкнутися
        for (int i = 0; i < DisplayController.DISPLAY_SECONDS; i++) {
            ctrl.tick();
        }

        assertEquals(DisplayMode.CURRENCY, ctrl.getCurrentMode(),
                "Mode must switch to CURRENCY after " + DisplayController.DISPLAY_SECONDS + " ticks");
    }

    // =========================================================================
    // Test 8: DisplayController writes to LCD on every clock tick
    // =========================================================================

    @Test
    void testDisplayController_clockMode_writesLcd() {
        DisplayController ctrl = new DisplayController(mockLcd, mockApiClient);

        // Один тік у режимі CLOCK
        ctrl.tick();

        // LCD повинен отримати записи — курсор + символи рядка 0 і рядка 1
        verify(mockLcd, atLeastOnce()).writeByte(
                eq(DisplayController.LCD_ADDR), anyInt(), any(byte.class));
    }

    // =========================================================================
    // Test 9: DisplayController fetches rates only on first currency tick
    // =========================================================================

    @Test
    void testDisplayController_currencyMode_fetchesOnce() throws IOException {
        when(mockApiClient.fetchRates()).thenReturn(Collections.emptyList());

        DisplayController ctrl = new DisplayController(mockLcd, mockApiClient);

        // Переходимо у режим CURRENCY — DISPLAY_SECONDS тіків CLOCK
        for (int i = 0; i < DisplayController.DISPLAY_SECONDS; i++) {
            ctrl.tick();
        }
        // Тепер в режимі CURRENCY — перший тік має завантажити курси
        ctrl.tick(); // тік 0 в CURRENCY → fetch
        ctrl.tick(); // тік 1 в CURRENCY → не fetch
        ctrl.tick(); // тік 2 в CURRENCY → не fetch

        // fetchRates() повинен бути викликаний рівно один раз протягом цього вікна
        verify(mockApiClient, times(1)).fetchRates();
    }
}

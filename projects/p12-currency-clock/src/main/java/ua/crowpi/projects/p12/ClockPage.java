package ua.crowpi.projects.p12;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Formats the current local time and date into two 16-character strings
 * suitable for display on a 16×2 character LCD.
 *
 * <p>Output layout:</p>
 * <pre>
 * Row 0: "Time: HH:MM:SS  "   (label 6 chars + time 8 chars + padding)
 * Row 1: "Date: DD.MM.YYYY"   (label 6 chars + date 10 chars = exactly 16)
 * </pre>
 *
 * <p>All methods are {@code static} — this class contains no mutable state.</p>
 */
public final class ClockPage {

    /** Width of the 16×2 LCD in characters. */
    public static final int LCD_WIDTH = 16;

    // Форматери оголошені static final — потокобезпечні і не перестворюються кожен раз
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private ClockPage() {
        // Утилітний клас — конструктор закритий
    }

    /**
     * Formats the top LCD row for the current time.
     *
     * <p>Example: {@code "Time: 14:35:22  "} (always exactly 16 characters).</p>
     *
     * @param time the local time to format
     * @return exactly {@value #LCD_WIDTH}-character string for LCD row 0
     */
    public static String formatRow0(LocalTime time) {
        // "Time: " = 6 символів, HH:mm:ss = 8 символів → разом 14, доповнюємо до 16
        return pad("Time: " + time.format(TIME_FMT));
    }

    /**
     * Formats the bottom LCD row for the current date.
     *
     * <p>Example: {@code "Date: 22.03.2026"} (always exactly 16 characters).</p>
     *
     * @param date the local date to format
     * @return exactly {@value #LCD_WIDTH}-character string for LCD row 1
     */
    public static String formatRow1(LocalDate date) {
        // "Date: " = 6 символів, DD.MM.YYYY = 10 символів → рівно 16, доповнення не потрібне
        return pad("Date: " + date.format(DATE_FMT));
    }

    /**
     * Pads or truncates a string to exactly {@value #LCD_WIDTH} characters.
     *
     * @param s input string
     * @return string of exactly {@value #LCD_WIDTH} characters
     */
    static String pad(String s) {
        // String.format із лівим вирівнюванням — зручний спосіб доповнити пробілами праворуч
        return String.format("%-" + LCD_WIDTH + "s", s).substring(0, LCD_WIDTH);
    }
}

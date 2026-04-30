package ua.crowpi.projects.p11;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Manages a persistent top-10 leaderboard stored as JSON in {@value #FILE}.
 *
 * <p>The leaderboard is loaded from disk on construction (tolerating a missing file).
 * {@link #addScore(String, int)} inserts a new entry, re-sorts descending by score,
 * and discards the 11th entry if present.  Call {@link #save()} to flush changes.</p>
 */
public class LeaderBoard {

    /** File path for the leaderboard JSON. */
    public static final String FILE = "scores.json";

    private static final int MAX_ENTRIES = 10;

    private final List<ScoreRecord> scores = new ArrayList<>();
    private final ObjectMapper      mapper;

    /**
     * Creates a LeaderBoard and loads existing scores from {@value #FILE} if it exists.
     *
     * <p>If the file is absent, the leaderboard starts empty — no exception is thrown.</p>
     */
    public LeaderBoard() {
        mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        load();
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Adds a new score entry, sorts descending, and trims to the top 10.
     *
     * @param name  player name for the entry
     * @param score achieved score (coins collected)
     */
    public void addScore(String name, int score) {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        scores.add(new ScoreRecord(name, score, ts));

        // Сортуємо за спаданням очок — найвищий результат на першому місці
        scores.sort(Comparator.comparingInt(ScoreRecord::getScore).reversed());

        // Видаляємо зайвий запис якщо більше MAX_ENTRIES після вставки
        while (scores.size() > MAX_ENTRIES) {
            scores.remove(scores.size() - 1);
        }
    }

    /**
     * Persists the leaderboard to {@value #FILE} as pretty-printed JSON.
     *
     * @throws IOException if the file cannot be written
     */
    public void save() throws IOException {
        // Створюємо батьківські директорії якщо потрібно
        File file = new File(FILE);
        if (file.getParentFile() != null) {
            Files.createDirectories(Paths.get(file.getParent()));
        }
        mapper.writeValue(file, scores);
    }

    /**
     * Returns an unmodifiable view of the current top scores.
     *
     * @return top scores in descending order, at most 10 entries
     */
    public List<ScoreRecord> getTopScores() {
        return Collections.unmodifiableList(scores);
    }

    /**
     * Returns {@code true} if the given score qualifies for the leaderboard.
     *
     * @param score the score to test
     * @return {@code true} if there are fewer than 10 entries, or the score beats the lowest
     */
    public boolean isTopScore(int score) {
        if (scores.size() < MAX_ENTRIES) return true;
        return score > scores.get(scores.size() - 1).getScore();
    }

    /** Returns the number of entries currently in the leaderboard. @return entry count */
    public int size() { return scores.size(); }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Loads scores from {@value #FILE}; silently skips if the file does not exist.
     */
    private void load() {
        File file = new File(FILE);
        if (!file.exists()) {
            // Файл ще не існує — починаємо з порожнього рейтингу
            return;
        }
        try {
            List<ScoreRecord> loaded = mapper.readValue(file,
                    new TypeReference<List<ScoreRecord>>() { });
            if (loaded != null) {
                scores.addAll(loaded);
            }
        } catch (IOException e) {
            // Пошкоджений файл або неправильний формат — ігноруємо, починаємо заново
            System.err.println("[LeaderBoard] Could not load scores.json: " + e.getMessage());
        }
    }
}

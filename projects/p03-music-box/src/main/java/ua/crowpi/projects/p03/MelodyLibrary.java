package ua.crowpi.projects.p03;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Static factory that provides the nine built-in melodies for the music box.
 *
 * <p>All melodies are created once at class-loading time and stored in an
 * unmodifiable list. Callers retrieve the complete list via {@link #getMelodies()}
 * and select individual melodies by 0-based index.</p>
 *
 * <p>Standard note frequencies used throughout (equal temperament, A4 = 440 Hz):</p>
 * <pre>
 *   C4=262  D4=294  E4=330  F4=349  G4=392
 *   A4=440  B4=494  C5=523  D5=587  E5=659
 *   F5=698  G5=784  A5=880
 *   Eb4=311 Bb4=466
 * </pre>
 *
 * <p>A rest note is represented by frequency {@code 0} — see {@link Note#isRest()}.</p>
 */
public final class MelodyLibrary {

    // -------------------------------------------------------------------------
    // Note frequency constants — стандартний рівноправний стрій (ET), A4=440 Гц
    // -------------------------------------------------------------------------

    /** С4 (до першої октави) — 262 Гц */
    private static final int C4 = 262;
    /** D4 (ре першої октави) — 294 Гц */
    private static final int D4 = 294;
    /** E4 (мі першої октави) — 330 Гц */
    private static final int E4 = 330;
    /** F4 (фа першої октави) — 349 Гц */
    private static final int F4 = 349;
    /** G4 (соль першої октави) — 392 Гц */
    private static final int G4 = 392;
    /** A4 (ля першої октави) — 440 Гц */
    private static final int A4 = 440;
    /** B4 (сі першої октави) — 494 Гц */
    private static final int B4 = 494;
    /** C5 (до другої октави) — 523 Гц */
    private static final int C5 = 523;
    /** D5 (ре другої октави) — 587 Гц */
    private static final int D5 = 587;
    /** E5 (мі другої октави) — 659 Гц */
    private static final int E5 = 659;
    /** F5 (фа другої октави) — 698 Гц */
    @SuppressWarnings("unused")
    private static final int F5 = 698;
    /** G5 (соль другої октави) — 784 Гц */
    private static final int G5 = 784;
    /** A5 (ля другої октави) — 880 Гц */
    @SuppressWarnings("unused")
    private static final int A5 = 880;
    /** Eb4 (мі-бемоль першої октави) — 311 Гц */
    private static final int EB4 = 311;
    /** Bb4 (сі-бемоль першої октави) — 466 Гц */
    private static final int BB4 = 466;
    /** Частота для сигналів Морзе — умовно 800 Гц */
    private static final int MORSE_FREQ = 800;
    /** REST — пауза (нульова частота позначає тишу) */
    private static final int REST = 0;

    // -------------------------------------------------------------------------
    // Prebuilt melody list
    // -------------------------------------------------------------------------

    /** Незмінний список усіх 9 мелодій, проіндексований 0–8. */
    private static final List<Melody> MELODIES;

    static {
        // Збираємо всі мелодії у порядку 1–9 (індекси 0–8)
        List<Melody> list = new ArrayList<>(9);
        list.add(buildMorseSos());           // 1
        list.add(buildCMajorScale());        // 2
        list.add(buildJingleBells());        // 3
        list.add(buildHappyBirthday());      // 4
        list.add(buildOdeToJoy());           // 5
        list.add(buildTwinkleTwinkle());     // 6
        list.add(buildImperialMarch());      // 7
        list.add(buildMarioTheme());         // 8
        list.add(buildPatrolSignal());       // 9
        MELODIES = Collections.unmodifiableList(list);
    }

    // -------------------------------------------------------------------------
    // Private constructor — тільки статичне використання
    // -------------------------------------------------------------------------

    /** Utility class — instantiation is prohibited. */
    private MelodyLibrary() {
        throw new UnsupportedOperationException("MelodyLibrary is a static utility class");
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns the complete list of nine built-in melodies.
     *
     * <p>The list is indexed 0–8, corresponding to IR button presses 1–9.
     * The list itself and each {@link Melody}'s note list are unmodifiable.</p>
     *
     * @return unmodifiable list of nine {@link Melody} objects
     */
    public static List<Melody> getMelodies() {
        return MELODIES;
    }

    // -------------------------------------------------------------------------
    // Individual melody builders
    // -------------------------------------------------------------------------

    /**
     * Builds melody 1: MORSE SOS.
     *
     * <p>S = three short (100 ms) beeps at 800 Hz, each followed by a 100 ms rest.
     * O = three long (300 ms) beeps at 800 Hz, each followed by a 100 ms rest.
     * Pattern: S S S O O O S S S.</p>
     *
     * @return the MORSE SOS melody
     */
    private static Melody buildMorseSos() {
        List<Note> n = new ArrayList<>();

        // S — три коротких сигнали (крапки)
        for (int i = 0; i < 3; i++) {
            n.add(new Note(MORSE_FREQ, 100)); // коротка крапка
            n.add(new Note(REST, 100));        // пауза між сигналами
        }
        // O — три довгих сигнали (тире)
        for (int i = 0; i < 3; i++) {
            n.add(new Note(MORSE_FREQ, 300)); // довге тире
            n.add(new Note(REST, 100));        // пауза між сигналами
        }
        // S — знову три коротких сигнали
        for (int i = 0; i < 3; i++) {
            n.add(new Note(MORSE_FREQ, 100));
            n.add(new Note(REST, 100));
        }

        return new Melody("MORSE SOS", n);
    }

    /**
     * Builds melody 2: C MAJOR SCALE.
     *
     * <p>Eight ascending notes C4–C5, each 300 ms.</p>
     *
     * @return the C Major Scale melody
     */
    private static Melody buildCMajorScale() {
        List<Note> n = new ArrayList<>();
        // Гама до-мажор від С4 до С5 — класичний навчальний приклад гами
        int[] freqs = {C4, D4, E4, F4, G4, A4, B4, C5};
        for (int freq : freqs) {
            n.add(new Note(freq, 300));
        }
        return new Melody("C MAJOR SCALE", n);
    }

    /**
     * Builds melody 3: JINGLE BELLS (first eight notes).
     *
     * <p>The classic opening phrase of Jingle Bells with authentic note timings.</p>
     *
     * @return the Jingle Bells melody
     */
    private static Melody buildJingleBells() {
        List<Note> n = new ArrayList<>();
        // Перші вісім нот «Jingle Bells» — впізнавана тема свята
        // E E E  E E E  E G C D E
        n.add(new Note(E4, 200)); // Jin-
        n.add(new Note(E4, 200)); // gle
        n.add(new Note(E4, 400)); // Bells
        n.add(new Note(E4, 200)); // Jin-
        n.add(new Note(E4, 200)); // gle
        n.add(new Note(E4, 400)); // Bells
        n.add(new Note(E4, 200)); // Jin-
        n.add(new Note(G4, 200)); // gle
        n.add(new Note(C4, 300)); // all the
        n.add(new Note(D4, 150)); // way
        n.add(new Note(E4, 600)); // (тримаємо)
        return new Melody("JINGLE BELLS", n);
    }

    /**
     * Builds melody 4: HAPPY BIRTHDAY (first six notes).
     *
     * <p>The opening phrase of Happy Birthday To You.</p>
     *
     * @return the Happy Birthday melody
     */
    private static Melody buildHappyBirthday() {
        List<Note> n = new ArrayList<>();
        // «Happy Birthday» — перші шість нот з автентичними тривалостями
        n.add(new Note(C4, 200)); // Hap-
        n.add(new Note(C4, 200)); // py
        n.add(new Note(D4, 400)); // Birth-
        n.add(new Note(C4, 400)); // day
        n.add(new Note(F4, 400)); // to
        n.add(new Note(E4, 800)); // you
        return new Melody("HAPPY BIRTHDAY", n);
    }

    /**
     * Builds melody 5: ODE TO JOY.
     *
     * <p>The first eight notes of Beethoven's Ode to Joy from Symphony No. 9.</p>
     *
     * @return the Ode to Joy melody
     */
    private static Melody buildOdeToJoy() {
        List<Note> n = new ArrayList<>();
        // Бетховен — «Ода до радості», перша фраза теми
        n.add(new Note(E4, 300));
        n.add(new Note(E4, 300));
        n.add(new Note(F4, 300));
        n.add(new Note(G4, 300));
        n.add(new Note(G4, 300));
        n.add(new Note(F4, 300));
        n.add(new Note(E4, 300));
        n.add(new Note(D4, 300));
        return new Melody("ODE TO JOY", n);
    }

    /**
     * Builds melody 6: TWINKLE TWINKLE.
     *
     * <p>The classic opening phrase of Twinkle Twinkle Little Star.</p>
     *
     * @return the Twinkle Twinkle melody
     */
    private static Melody buildTwinkleTwinkle() {
        List<Note> n = new ArrayList<>();
        // «Мигтить, мигтить, зірочко» — першочергова навчальна мелодія
        n.add(new Note(C4, 300)); // Twin-
        n.add(new Note(C4, 300)); // kle
        n.add(new Note(G4, 300)); // twin-
        n.add(new Note(G4, 300)); // kle
        n.add(new Note(A4, 300)); // lit-
        n.add(new Note(A4, 300)); // tle
        n.add(new Note(G4, 600)); // star (подвоєна тривалість)
        return new Melody("TWINKLE TWINKLE", n);
    }

    /**
     * Builds melody 7: IMPERIAL MARCH.
     *
     * <p>The iconic theme from Star Wars (Darth Vader's Theme) — first phrase.</p>
     *
     * @return the Imperial March melody
     */
    private static Melody buildImperialMarch() {
        List<Note> n = new ArrayList<>();
        // «Марш Імперської гвардії» — Star Wars, Дарт Вейдер
        // Eb4=311 Гц, Bb4=466 Гц
        n.add(new Note(G4,  500));
        n.add(new Note(G4,  500));
        n.add(new Note(G4,  500));
        n.add(new Note(EB4, 350)); // Eb4 — мі-бемоль (понижена терція)
        n.add(new Note(BB4, 150)); // Bb4 — сі-бемоль (висока квінта)
        n.add(new Note(G4,  500));
        n.add(new Note(EB4, 350));
        n.add(new Note(BB4, 150));
        n.add(new Note(G4, 1000)); // довга фінальна нота
        return new Melody("IMPERIAL MARCH", n);
    }

    /**
     * Builds melody 8: MARIO THEME.
     *
     * <p>The opening notes of the Super Mario Bros. main theme.</p>
     *
     * @return the Mario Theme melody
     */
    private static Melody buildMarioTheme() {
        List<Note> n = new ArrayList<>();
        // «Маріо» — культова гра Nintendo, впізнаваний початок теми
        n.add(new Note(E5,   150));
        n.add(new Note(E5,   150));
        n.add(new Note(REST, 150)); // пауза між подвоєним E5
        n.add(new Note(E5,   150));
        n.add(new Note(REST, 150)); // пауза перед переходом на C5
        n.add(new Note(C5,   150));
        n.add(new Note(E5,   150));
        n.add(new Note(G5,   300)); // стрибок на G5 — верхня нота теми
        return new Melody("MARIO THEME", n);
    }

    /**
     * Builds melody 9: PATROL SIGNAL.
     *
     * <p>A rising arpeggio (C5–E5–G5) played twice followed by a held C5 —
     * evokes a patrol alert or beacon sound.</p>
     *
     * @return the Patrol Signal melody
     */
    private static Melody buildPatrolSignal() {
        List<Note> n = new ArrayList<>();
        // Сигнал патруля — висхідне арпеджіо двічі, потім довгий сигнал
        n.add(new Note(C5, 100));
        n.add(new Note(E5, 100));
        n.add(new Note(G5, 100));
        n.add(new Note(C5, 100));
        n.add(new Note(E5, 100));
        n.add(new Note(G5, 100));
        n.add(new Note(C5, 200)); // фінальний підтверджувальний сигнал
        return new Melody("PATROL SIGNAL", n);
    }
}

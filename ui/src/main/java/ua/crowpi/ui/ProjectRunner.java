package ua.crowpi.ui;

// ============================================================================
// ProjectRunner — менеджер фонового запуску одного CrowPi-проекту
// ============================================================================
//
// НАВЧАЛЬНИЙ КОНТЕКСТ: Threading у Swing
// ──────────────────────────────────────
// Swing є однопоточним фреймворком: усі операції з компонентами (кнопки, мітки)
// мають виконуватись виключно в Event Dispatch Thread (EDT).
//
// CrowPi-проекти (project.run()) блокують виклик до Ctrl+C або shutdown().
// Якщо запустити проект прямо в EDT — вікно "зависне" і не реагуватиме на кліки.
//
// Рішення: запускаємо project.run() у daemon-потоці (фоновий Thread).
// Після завершення — повідомляємо UI через SwingUtilities.invokeLater(),
// що гарантує виконання колбеку в EDT.
//
// Схема потоків:
//
//   EDT (Swing)                    Daemon Thread ("project-runner-*")
//   ───────────                    ──────────────────────────────────
//   start()  ──────────────────►  project.run(mockMode)   (блокує)
//   stop()   ──────────────────►  project.shutdown()
//                                 interrupt()
//            ◄──────────────────  SwingUtilities.invokeLater(onFinished)
//   onFinished() (у EDT)
// ============================================================================

import ua.crowpi.core.CrowPiProject;
import ua.crowpi.core.exception.HardwareException;

import javax.swing.SwingUtilities;
import java.util.function.BiConsumer;

/**
 * Manages running a {@link CrowPiProject} in a background thread so that
 * the Swing Event Dispatch Thread (EDT) remains responsive during project execution.
 *
 * <p>Usage pattern:
 * <pre>
 *   ProjectRunner runner = new ProjectRunner((name, err) -&gt; updateUI(name, err));
 *   runner.start(project, false);   // runs project.run() in background
 *   // later...
 *   runner.stop();                  // calls project.shutdown() + interrupts thread
 * </pre>
 *
 * <p>This class is <strong>not</strong> thread-safe for concurrent {@code start()} calls;
 * all calls should originate from the EDT (which is single-threaded by design).
 */
public class ProjectRunner {

    // ── Стан ─────────────────────────────────────────────────────────────────

    /**
     * Колбек, що викликається після завершення проекту (нормального або через stop()).
     * Сигнатура: {@code (projectName, errorMessage)} — errorMessage == null при успіху.
     * ЗАВЖДИ викликається в EDT через {@link SwingUtilities#invokeLater}.
     */
    private final BiConsumer<String, String> onFinished;

    /**
     * Поточний запущений проект або {@code null}, якщо жоден не запущений.
     * Оголошено {@code volatile}, бо читається/пишеться з різних потоків
     * (EDT для перевірки, daemon-потік для скидання після завершення).
     */
    private volatile CrowPiProject currentProject;

    /**
     * Фоновий daemon-потік, у якому виконується {@code project.run()}.
     * {@code null} коли жоден проект не запущений.
     */
    private Thread runnerThread;

    // ── Конструктор ──────────────────────────────────────────────────────────

    /**
     * Creates a new {@code ProjectRunner}.
     *
     * @param onFinished callback invoked on the EDT when the project finishes;
     *                   receives {@code (projectName, errorOrNull)} where
     *                   {@code errorOrNull} is {@code null} on clean exit
     */
    public ProjectRunner(BiConsumer<String, String> onFinished) {
        this.onFinished = onFinished;
    }

    // ── Публічний API ─────────────────────────────────────────────────────────

    /**
     * Starts the given project in a background daemon thread.
     *
     * <p>Precondition: any previously running project must already be stopped
     * via {@link #stop()} before calling this method.
     *
     * @param project  the project to launch
     * @param mockMode {@code true} to use mock GPIO facades (no real hardware);
     *                 {@code false} to use Pi4J real GPIO on Raspberry Pi
     */
    public void start(CrowPiProject project, boolean mockMode) {
        // Зберігаємо посилання на поточний проект для можливості зупинки через stop()
        currentProject = project;

        // Daemon-потік — JVM може завершитись, навіть якщо він іще виконується.
        // Це важливо: якщо користувач закриє вікно, JVM не "застрягне" в project.run().
        runnerThread = new Thread(
                () -> executeProject(project, mockMode),
                "project-runner-" + project.getProjectId()
        );
        runnerThread.setDaemon(true);
        runnerThread.start();
    }

    /**
     * Stops the currently running project (if any).
     *
     * <p>Stopping sequence:
     * <ol>
     *   <li>Calls {@code project.shutdown()} — releases GPIO pins, stops executor services, etc.
     *   <li>Interrupts the runner thread — unblocks {@code Thread.sleep()} or {@code wait()} calls
     * </ol>
     *
     * <p>This method is idempotent — safe to call multiple times or when no project is running.
     */
    public void stop() {
        CrowPiProject proj = currentProject;
        if (proj == null) {
            return; // Нічого не запущено — нічого робити
        }

        // shutdown() — коректне завершення: звільняє GPIO піни, зупиняє потоки
        // всередині проекту, закриває файли тощо. Метод ідемпотентний за специфікацією.
        try {
            proj.shutdown();
        } catch (Exception e) {
            // Не пробрасуємо — навіть при помилці shutdown() намагаємось перервати потік
            System.err.println("[ProjectRunner] Помилка під час shutdown(): " + e.getMessage());
        }

        // Переривємо потік для проектів, що блокуються у Thread.sleep() або Object.wait().
        // shutdown() може не розблокувати такий потік — interrupt() гарантує завершення.
        Thread t = runnerThread;
        if (t != null && t.isAlive()) {
            t.interrupt();
        }

        // Примітка: currentProject і runnerThread скинуться в executeProject() → finally
    }

    /**
     * Returns {@code true} if a project is currently running.
     *
     * @return {@code true} if the runner thread is alive
     */
    public boolean isRunning() {
        Thread t = runnerThread;
        return t != null && t.isAlive();
    }

    // ── Внутрішня логіка ─────────────────────────────────────────────────────

    /**
     * Body of the runner thread. Calls {@code project.run()}, handles exceptions,
     * and notifies the UI via the callback when done.
     *
     * @param project  project to run
     * @param mockMode mock flag forwarded to {@code project.run()}
     */
    private void executeProject(CrowPiProject project, boolean mockMode) {
        String error = null;

        try {
            // Основний блокуючий виклик — виконується до shutdown() або Ctrl+C.
            // Проект сам керує своїм циклом (while(running.get()) або подібне).
            project.run(mockMode);

        } catch (HardwareException e) {
            // Типова помилка: GPIO пін недоступний, I2C-пристрій не відповідає тощо
            error = "HardwareException: " + e.getMessage();
            System.err.println("[ProjectRunner] " + error);

        } catch (Exception e) {
            // CrowPiProject.run() оголошує лише throws HardwareException,
            // тому InterruptedException не може вирватись назовні напряму —
            // проект перехоплює переривання всередині і завершує run() штатно.
            // Сюди потрапляють лише несподівані RuntimeException.
            error = e.getClass().getSimpleName() + ": " + e.getMessage();
            System.err.println("[ProjectRunner] Непередбачена помилка: " + error);

        } finally {
            // Скидаємо стан незалежно від причини завершення (нормальне, помилка, переривання).
            // finally гарантує виконання навіть при RuntimeException.
            currentProject = null;
            runnerThread   = null;

            // Повідомляємо UI через EDT (ОБОВ'ЯЗКОВО: ми зараз у daemon-потоці, не в EDT).
            // SwingUtilities.invokeLater ставить колбек у чергу EDT — Swing-компоненти
            // оновляться безпечно, без race condition.
            final String finalError = error;
            SwingUtilities.invokeLater(() -> onFinished.accept(project.getName(), finalError));
        }
    }
}

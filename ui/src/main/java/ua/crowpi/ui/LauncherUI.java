package ua.crowpi.ui;

// ============================================================================
// LauncherUI — графічний Swing-лаунчер для CrowPi Project Suite
// ============================================================================
//
// НАВЧАЛЬНИЙ КОНТЕКСТ: Java Swing — основи GUI-програмування
// ──────────────────────────────────────────────────────────
// Swing — стандартна Java-бібліотека для створення графічних інтерфейсів.
// Вона входить до JDK (пакет javax.swing) і не потребує зовнішніх залежностей.
//
// Ключові концепції Swing, що використовуються тут:
//
// 1. Event Dispatch Thread (EDT) — єдиний потік, в якому Swing малює компоненти
//    і обробляє події. НІКОЛИ не блокуйте EDT (не викликайте project.run() в ньому!)
//    Використовуйте SwingUtilities.invokeLater() для переходу в EDT з інших потоків.
//
// 2. JFrame — головне вікно програми. Містить «content pane» — контейнер
//    для всіх інших компонентів.
//
// 3. LayoutManager — Swing використовує менеджери компоновки замість фіксованих
//    координат. Тут:
//      • BorderLayout — ділить контейнер на 5 зон (N/S/W/E/CENTER)
//      • BoxLayout(Y_AXIS) — складає компоненти вертикально в один стовпець
//      • GridLayout — рівна сітка рядків та колонок
//
// 4. BoxLayout(Y_AXIS) + JScrollPane — класичний патерн для вертикальних списків,
//    що прокручуються. Кожна кнопка займає повну ширину панелі і фіксовану висоту.
//    Якщо кнопок більше, ніж вміщується у вікно — з'являється смуга прокрутки.
//
// 5. JSplitPane — ділить площу між двома компонентами з можливістю перетягування
//    роздільника. Тут: ліворуч — список кнопок, праворуч — опис проекту.
//
// 6. JTextArea — багаторядкове текстове поле. setEditable(false) + setLineWrap(true)
//    перетворює його на зручний компонент для відображення довгого тексту,
//    що автоматично переносить слова.
//
// 7. ToolTipText — підказка при наведенні курсору (додаткова, на випадок
//    якщо опис справа не видно).
//
// 8. ServiceLoader — не Swing, а Java-механізм виявлення плагінів.
//    Тут використовується для динамічного отримання списку CrowPiProject.
//
// Структура вікна (вертикальний список, оптимізований для малого екрана):
//
// ┌──────────────────────────────────────────────────┐
// │         CrowPi Project Launcher                  │  ← заголовок (NORTH)
// ├────────────────────┬─────────────────────────────┤
// │ P01 Thermometer    │ ┌ Опис проекту ───────────┐ │
// │ P02 Counter        │ │ <назва>                 │ │
// │ P03 Music Box      │ │                         │ │  ← CENTER
// │ P04 Alarm          │ │ <опис одним реченням>   │ │    (JSplitPane)
// │ P05 Radar          │ │                         │ │
// │ ...  ↕ scroll      │ └─────────────────────────┘ │
// ├────────────────────┴─────────────────────────────┤
// │ [x] Mock mode   [■ Зупинити]    Статус: готово  │  ← SOUTH
// └──────────────────────────────────────────────────┘
// ============================================================================

import ua.crowpi.core.CrowPiProject;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Main Swing window for the CrowPi Project Launcher UI.
 *
 * <p>Discovers all {@link CrowPiProject} implementations at runtime using
 * {@link ServiceLoader} (the same plugin mechanism used by the console
 * {@code ua.crowpi.core.launcher.Launcher}). Renders one button per project
 * in a <strong>vertical scrollable list</strong> on the left; the right panel
 * shows the full description of the currently hovered or active project.
 * Clicking a button starts the project in a background thread via {@link ProjectRunner}.
 *
 * <p>Optimised for the CrowPi 7-inch display (~1024×600 px) where horizontal
 * button rows are hard to read — a vertical list with large text is much clearer.
 *
 * <p>This class is the entry point of the {@code crowpi-ui-1.0.0.jar} fat JAR.
 * It does <strong>not</strong> modify any existing class in the {@code core} or
 * {@code projects} modules.
 */
public class LauncherUI extends JFrame {

    // ── Візуальні константи ──────────────────────────────────────────────────

    /**
     * Початкові розміри вікна.
     * 720×520 добре вміщується на 7" дисплеї CrowPi (1024×600 або 800×480).
     */
    private static final int WINDOW_WIDTH  = 720;
    private static final int WINDOW_HEIGHT = 520;

    /**
     * Висота одного рядка (кнопки) у вертикальному списку.
     * 64px = гарантовано вміщує два рядки тексту (ID + назва) на будь-яких
     * системних шрифтах, включаючи GTK+ на Raspbian де розміри рендера більші.
     */
    private static final int BTN_HEIGHT = 76;

    /**
     * Початкова ширина лівої панелі зі списком кнопок.
     * JSplitPane дозволяє перетягнути роздільник, але за замовчуванням — 280px.
     */
    private static final int LIST_PANEL_WIDTH = 280;

    /** Кольорова схема — темна, зручна для дрібного 7-дюймового дисплея. */
    private static final Color COLOR_BG          = new Color(34, 34, 38);    // фон вікна
    private static final Color COLOR_LIST_BG     = new Color(44, 44, 50);    // фон списку кнопок
    private static final Color COLOR_DESC_BG     = new Color(40, 40, 46);    // фон панелі опису
    private static final Color COLOR_BTN_IDLE    = new Color(44, 44, 50);    // кнопка: спокій
    private static final Color COLOR_BTN_HOVER   = new Color(55, 85, 140);   // кнопка: наведення
    private static final Color COLOR_BTN_ACTIVE  = new Color(28, 120, 55);   // кнопка: запущено
    private static final Color COLOR_BTN_BORDER  = new Color(62, 62, 70);    // межа між кнопками
    private static final Color COLOR_TEXT_MAIN   = new Color(230, 230, 240); // основний текст
    private static final Color COLOR_TEXT_DIM    = new Color(160, 160, 175); // другорядний текст
    private static final Color COLOR_TEXT_ID     = new Color(120, 170, 255); // ID проекту (синій)
    private static final Color COLOR_STATUS      = new Color(180, 180, 195); // рядок статусу
    private static final Color COLOR_DIVIDER     = new Color(60, 60, 68);    // роздільник панелей

    // ── Стан ─────────────────────────────────────────────────────────────────

    /**
     * Список знайдених проектів, відсортований за projectId.
     * Заповнюється один раз у конструкторі; надалі лише читається.
     */
    private final List<CrowPiProject> projects = new ArrayList<>();

    /**
     * Менеджер фонового запуску проектів.
     * Один екземпляр на весь час роботи вікна.
     *
     * @see ProjectRunner
     */
    private final ProjectRunner runner = new ProjectRunner(this::handleProjectFinished);

    /**
     * Кнопка, що відповідає поточному активному проекту ({@code null} = не запущено).
     * Підсвічується зеленим (COLOR_BTN_ACTIVE) під час виконання.
     */
    private JButton activeButton = null;

    // ── Swing-компоненти, що потрібні в кількох методах ─────────────────────

    /** Чекбокс "Mock mode" — дозволяє тестувати без реального GPIO. */
    private JCheckBox mockCheckbox;

    /** Кнопка зупинки активного проекту. Неактивна, поки жоден проект не запущений. */
    private JButton stopButton;

    /** Рядок статусу в нижній частині вікна. */
    private JLabel statusLabel;

    /**
     * Заголовок панелі опису (права частина): великий текст із назвою проекту.
     * Оновлюється при наведенні на кнопку.
     */
    private JLabel descNameLabel;

    /**
     * Ідентифікатор проекту на панелі опису (P01, P02 …).
     * Відображається над назвою, дрібнішим шрифтом іншого кольору.
     */
    private JLabel descIdLabel;

    /**
     * Текстова область із описом проекту (права частина).
     * {@link JTextArea} з увімкненим перенесенням слів — підходить для
     * довільного за довжиною тексту без горизонтальної прокрутки.
     */
    private JTextArea descTextArea;

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Constructor: loads projects via ServiceLoader and builds the Swing window.
     * Must be called on the EDT (via {@link SwingUtilities#invokeLater}).
     */
    public LauncherUI() {
        super("CrowPi Project Launcher");

        // 1) Завантажуємо проекти (не Swing — порядок кроків неважливий)
        loadProjects();

        // 2) Будуємо всі Swing-компоненти і розставляємо їх у вікні
        buildWindow();

        // 3) Реєструємо слухача подій вікна для коректного завершення
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // Зупиняємо поточний проект (GPIO звільняється), потім виходимо
                runner.stop();
                dispose();
                System.exit(0);
            }
        });
    }

    // =========================================================================
    // ЗАВАНТАЖЕННЯ ПРОЕКТІВ
    // =========================================================================

    /**
     * Loads all {@link CrowPiProject} implementations from the fat JAR using
     * {@link ServiceLoader}. Skips individual projects that fail to instantiate
     * without aborting the rest (same fault-tolerant strategy as
     * {@code ProjectRegistry} in the core module).
     *
     * <p>After loading, sorts by {@code projectId} (p01, p02 … p12) so list
     * order is always deterministic regardless of JAR packaging order.
     */
    private void loadProjects() {
        // ServiceLoader читає META-INF/services/ua.crowpi.core.CrowPiProject у fat JAR
        // і інстанціює кожен клас, перерахований у цьому файлі.
        // mergeServiceFiles() у ui/build.gradle об'єднав усі 12 service-файлів в один.
        ServiceLoader<CrowPiProject> loader = ServiceLoader.load(CrowPiProject.class);

        // Явний Iterator (а не for-each) — перехоплюємо помилку одного проекту
        // і продовжуємо завантаження решти (той самий підхід, що в ProjectRegistry).
        Iterator<CrowPiProject> it = loader.iterator();
        while (it.hasNext()) {
            try {
                projects.add(it.next());
            } catch (Exception | Error e) {
                System.err.println("[UI] Не вдалося завантажити проект: "
                        + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }

        // Сортуємо за projectId — "p01" < "p02" < … < "p12" (лексикографічно)
        projects.sort((a, b) -> a.getProjectId().compareTo(b.getProjectId()));

        System.out.println("[UI] Знайдено проектів: " + projects.size());
    }

    // =========================================================================
    // ПОБУДОВА ВІКНА
    // =========================================================================

    /**
     * Builds and arranges all Swing components inside the {@link JFrame}.
     *
     * <p>Overall structure:
     * <pre>
     *   JFrame
     *   └─ root JPanel  (BorderLayout)
     *      ├─ NORTH:   buildHeaderPanel()   — заголовок
     *      ├─ CENTER:  buildSplitPanel()    — список кнопок + опис
     *      └─ SOUTH:   buildControlPanel()  — Mock mode, Stop, статус
     * </pre>
     */
    private void buildWindow() {
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setMinimumSize(new Dimension(520, 380));
        setLocationRelativeTo(null); // по центру екрана
        setResizable(true);

        // Кореневий контейнер з темним фоном
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(COLOR_BG);
        setContentPane(root);

        root.add(buildHeaderPanel(), BorderLayout.NORTH);
        root.add(buildSplitPanel(),  BorderLayout.CENTER);
        root.add(buildControlPanel(), BorderLayout.SOUTH);
    }

    // ─── Заголовок ───────────────────────────────────────────────────────────

    /**
     * Builds the compact header bar with the application title.
     *
     * @return configured {@link JPanel} for the NORTH zone
     */
    private JPanel buildHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(28, 28, 32));
        // Нижня межа — тонка лінія-роздільник між заголовком і тілом
        panel.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, COLOR_DIVIDER),
                new EmptyBorder(10, 14, 10, 14)));

        JLabel title = new JLabel("CrowPi Project Launcher");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(Color.WHITE);

        // Підказка праворуч у заголовку — виглядає як "badge" або caption
        JLabel hint = new JLabel("Оберіть і запустіть проект");
        hint.setFont(new Font("SansSerif", Font.PLAIN, 12));
        hint.setForeground(COLOR_TEXT_DIM);

        panel.add(title, BorderLayout.WEST);
        panel.add(hint,  BorderLayout.EAST);
        return panel;
    }

    // ─── Головна область (JSplitPane) ────────────────────────────────────────

    /**
     * Builds the main area as a {@link JSplitPane}:
     * <ul>
     *   <li>Left component — {@link #buildButtonListPanel()} — scrollable vertical
     *       list of project buttons
     *   <li>Right component — {@link #buildDescriptionPanel()} — project description
     *       that updates on hover / click
     * </ul>
     *
     * <p>{@code JSplitPane} lets the user drag the divider to resize panels,
     * which is useful when project descriptions are long.
     *
     * @return configured {@link JSplitPane}
     */
    private JSplitPane buildSplitPanel() {
        // HORIZONTAL_SPLIT — ліва і права панелі розташовані поруч
        JSplitPane split = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                buildButtonListPanel(),
                buildDescriptionPanel());

        // Початкова ширина лівої панелі (у пікселях)
        split.setDividerLocation(LIST_PANEL_WIDTH);

        // Розмір роздільника в пікселях — 4px достатньо для перетягування
        split.setDividerSize(4);
        split.setBackground(COLOR_DIVIDER);
        split.setBorder(null);

        // Не змінювати розподіл при зміні розміру вікна: ліва панель залишається
        // фіксованою (0.0 = вся "зайва" ширина іде у правий компонент)
        split.setResizeWeight(0.0);

        return split;
    }

    // ─── Ліва панель: вертикальний список кнопок ─────────────────────────────

    /**
     * Builds the scrollable left panel containing one button per project,
     * stacked vertically using {@link BoxLayout}.
     *
     * <p>Why BoxLayout(Y_AXIS) instead of FlowLayout?
     * <ul>
     *   <li>Buttons are stacked in a single column — easy to scan vertically.
     *   <li>Each button stretches to the full panel width (set via
     *       {@code setMaximumSize(Short.MAX_VALUE, BTN_HEIGHT)}).
     *   <li>JScrollPane adds a vertical scrollbar when buttons overflow the
     *       visible area — essential for small CrowPi screen.
     * </ul>
     *
     * @return {@link JScrollPane} containing the button list
     */
    private JScrollPane buildButtonListPanel() {
        // BoxLayout(Y_AXIS) — розміщує дочірні компоненти вертикально, один під одним
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(COLOR_LIST_BG);

        if (projects.isEmpty()) {
            // Якщо ServiceLoader нічого не знайшов — показуємо помилку
            JLabel err = new JLabel("<html><center>Проектів не знайдено.<br>"
                    + "Перевірте збірку:<br>./gradlew :ui:shadowJar</center></html>");
            err.setForeground(new Color(255, 90, 90));
            err.setFont(new Font("SansSerif", Font.PLAIN, 13));
            err.setAlignmentX(Component.CENTER_ALIGNMENT);
            err.setBorder(new EmptyBorder(20, 10, 20, 10));
            listPanel.add(err);
        } else {
            // Для кожного проекту — повноширокий рядок-кнопка
            for (CrowPiProject project : projects) {
                listPanel.add(createProjectButton(project));
            }
        }

        // JScrollPane: вертикальна прокрутка є завжди (щоб список не "стрибав"),
        // горизонтальна — ніколи (кнопки розтягуються на повну ширину)
        JScrollPane scroll = new JScrollPane(listPanel,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(COLOR_LIST_BG);

        // Крок прокрутки колесом = одна кнопка (BTN_HEIGHT пікселів)
        scroll.getVerticalScrollBar().setUnitIncrement(BTN_HEIGHT);

        // Гарантуємо, що список починається з першого елемента після відображення.
        // invokeLater відкладає скрол до завершення першого layout-pass Swing,
        // бо до цього scrollBar.setValue(0) ігнорується (viewport ще не розраховано).
        SwingUtilities.invokeLater(() ->
                scroll.getVerticalScrollBar().setValue(0));

        return scroll;
    }

    /**
     * Creates one full-width row button for the given project.
     *
     * <p>Design decisions for CrowPi's small screen:
     * <ul>
     *   <li>Full panel width via {@code setMaximumSize(Short.MAX_VALUE, BTN_HEIGHT)} —
     *       BoxLayout Y_AXIS stretches to fill horizontal space.
     *   <li>LEFT alignment — easier to scan a vertical list left-to-right than centered.
     *   <li>Two visual elements: coloured ID badge on the left, project name larger font.
     *   <li>Bottom border line — visually separates rows like a table.
     *   <li>Hover effect — highlights the full row for clear feedback.
     *   <li>Tooltip — shows description for cases when right panel is too narrow.
     * </ul>
     *
     * @param project the project this button represents
     * @return styled full-width {@link JButton}
     */
    private JButton createProjectButton(CrowPiProject project) {
        // HTML у тексті кнопки — два "рядки" в одній кнопці:
        // • верхній: ID маленьким синім текстом
        // • нижній: назва великим білим текстом
        // font-size у Swing HTML задається у CSS pt, але сприймається як px (різниця невелика)
        String label = "<html>"
                + "<span style='color:#789ad0; font-size:10px;'>"
                + project.getProjectId().toUpperCase()
                + "</span><br>"
                + "<span style='color:#e6e6f0; font-size:14px;'>"
                + project.getName()
                + "</span>"
                + "</html>";

        JButton btn = new JButton(label);

        // Висота фіксована, ширина = максимально доступна (BoxLayout розтягне)
        btn.setPreferredSize(new Dimension(LIST_PANEL_WIDTH, BTN_HEIGHT));
        btn.setMinimumSize(new Dimension(80, BTN_HEIGHT));
        // Short.MAX_VALUE як "нескінченна" ширина — BoxLayout Y_AXIS розтягне кнопку
        // до реальної ширини контейнера (не більше)
        btn.setMaximumSize(new Dimension(Short.MAX_VALUE, BTN_HEIGHT));

        // Вирівнювання по лівому краю — BoxLayout Y_AXIS враховує AlignmentX
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Стиль: плоский, без рамки Swing (замінюємо на власну нижню межу)
        btn.setBackground(COLOR_BTN_IDLE);
        btn.setForeground(COLOR_TEXT_MAIN);
        btn.setOpaque(true);              // необхідно для setBackground на всіх L&F
        btn.setFocusPainted(false);       // без пунктирного прямокутника фокуса
        btn.setBorderPainted(true);
        btn.setHorizontalAlignment(SwingConstants.LEFT); // текст — зліва
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Відступ тексту від лівого краю + тонка нижня межа між рядками
        btn.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, COLOR_BTN_BORDER), // нижня лінія-роздільник
                new EmptyBorder(6, 14, 6, 10)));                 // внутрішній відступ

        // Tooltip — стисла підказка при наведенні (корисна якщо права панель прихована)
        btn.setToolTipText("<html><body style='width:240px; padding:3px'>"
                + "<b>" + project.getName() + "</b><br>"
                + project.getDescription()
                + "</body></html>");

        // MouseListener: hover-ефект + оновлення панелі опису праворуч
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                // Підсвічуємо рядок при наведенні (якщо він не є активним)
                if (btn != activeButton) {
                    btn.setBackground(COLOR_BTN_HOVER);
                }
                // Оновлюємо праву панель — показуємо опис проекту
                updateDescription(project);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (btn != activeButton) {
                    btn.setBackground(COLOR_BTN_IDLE);
                }
                // При виході курсору — залишаємо опис активного проекту
                // (або порожній стан, якщо жоден не запущений)
                if (activeButton == null) {
                    clearDescription();
                }
            }
        });

        // ActionListener — запуск проекту при кліку
        btn.addActionListener(e -> handleProjectButtonClick(project, btn));

        return btn;
    }

    // ─── Права панель: опис проекту ──────────────────────────────────────────

    /**
     * Builds the right description panel that shows the name and description
     * of the currently hovered or running project.
     *
     * <p>Uses a {@link JTextArea} (not {@link JLabel}) for the description text
     * because {@code JTextArea} supports automatic word-wrap ({@code setLineWrap(true)}),
     * which is essential for long descriptions on a narrow panel.
     *
     * @return configured description {@link JPanel}
     */
    private JPanel buildDescriptionPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(COLOR_DESC_BG);
        panel.setBorder(new EmptyBorder(18, 18, 18, 18));

        // ── Верхня частина: ID + назва ───────────────────────────────────────
        JPanel top = new JPanel(new GridLayout(2, 1, 0, 4));
        top.setBackground(COLOR_DESC_BG);

        // Ідентифікатор проекту (P01, P02 …) — дрібний, синюватий
        descIdLabel = new JLabel(" ");
        descIdLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        descIdLabel.setForeground(COLOR_TEXT_ID);

        // Назва проекту — великий жирний текст
        descNameLabel = new JLabel(" ");
        descNameLabel.setFont(new Font("SansSerif", Font.BOLD, 17));
        descNameLabel.setForeground(COLOR_TEXT_MAIN);

        top.add(descIdLabel);
        top.add(descNameLabel);

        // ── Роздільник між назвою і текстом опису ────────────────────────────
        // JSeparator — горизонтальна лінія; вставляємо її у JPanel щоб задати відступи
        JSeparator sep = new JSeparator();
        sep.setForeground(COLOR_DIVIDER);
        sep.setBackground(COLOR_DESC_BG);

        // ── Текст опису ───────────────────────────────────────────────────────
        // JTextArea замість JLabel: підтримує перенесення слів і прокрутку
        descTextArea = new JTextArea(" ");
        descTextArea.setEditable(false);       // лише для читання — не поле вводу
        descTextArea.setFocusable(false);      // не захоплює фокус клавіатури
        descTextArea.setLineWrap(true);        // переносить рядки, якщо не вміщуються
        descTextArea.setWrapStyleWord(true);   // переносить по межі слів (не посередині)
        descTextArea.setBackground(COLOR_DESC_BG);
        descTextArea.setForeground(COLOR_TEXT_DIM);
        descTextArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
        descTextArea.setBorder(null);

        // JScrollPane навколо JTextArea — якщо текст не вміщується по висоті
        JScrollPane descScroll = new JScrollPane(descTextArea,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        descScroll.setBorder(null);
        descScroll.getViewport().setBackground(COLOR_DESC_BG);

        // Розміщення: заголовок зверху, роздільник, текст займає решту місця
        panel.add(top,       BorderLayout.NORTH);
        panel.add(sep,       BorderLayout.CENTER); // тимчасово CENTER, замінюється нижче

        // Перероблюємо: вертикальне BoxLayout для top+sep+scroll
        panel.removeAll();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        top.setAlignmentX(Component.LEFT_ALIGNMENT);
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        descScroll.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(top);
        panel.add(Box.createVerticalStrut(8));   // відступ між назвою і роздільником
        panel.add(sep);
        panel.add(Box.createVerticalStrut(10));  // відступ між роздільником і текстом
        panel.add(descScroll);

        return panel;
    }

    /**
     * Updates the description panel to show info for the given project.
     * Safe to call from any thread (delegates to EDT via {@link SwingUtilities#invokeLater}).
     *
     * @param project project whose info should be displayed
     */
    private void updateDescription(CrowPiProject project) {
        SwingUtilities.invokeLater(() -> {
            descIdLabel.setText(project.getProjectId().toUpperCase());
            descNameLabel.setText(project.getName());
            descTextArea.setText(project.getDescription());
            // Прокручуємо текст на початок — щоб завжди показувати з першого рядка
            descTextArea.setCaretPosition(0);
        });
    }

    /**
     * Clears the description panel (shown when no project is hovered or active).
     */
    private void clearDescription() {
        SwingUtilities.invokeLater(() -> {
            descIdLabel.setText(" ");
            descNameLabel.setText(" ");
            descTextArea.setText("Наведіть курсор на проект зліва, щоб побачити його опис.");
        });
    }

    // ─── Нижня панель керування ───────────────────────────────────────────────

    /**
     * Builds the bottom control bar with:
     * <ul>
     *   <li>Mock mode checkbox
     *   <li>Stop button
     *   <li>Status label (right-aligned)
     * </ul>
     *
     * @return configured {@link JPanel} for the SOUTH zone
     */
    private JPanel buildControlPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setBackground(new Color(28, 28, 32));
        panel.setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, COLOR_DIVIDER), // верхня межа-роздільник
                new EmptyBorder(8, 14, 8, 14)));

        // ── Ліворуч: Mock mode + Stop ─────────────────────────────────────────
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setBackground(new Color(28, 28, 32));

        // Чекбокс Mock mode — підходить для тестування без Raspberry Pi
        mockCheckbox = new JCheckBox("Mock mode  (без GPIO)");
        mockCheckbox.setBackground(new Color(28, 28, 32));
        mockCheckbox.setForeground(COLOR_STATUS);
        mockCheckbox.setFont(new Font("SansSerif", Font.PLAIN, 13));
        mockCheckbox.setFocusPainted(false);
        mockCheckbox.setToolTipText("<html>"
                + "Усі GPIO-операції симулюються в консолі.<br>"
                + "Дозволяє запускати проекти на звичайному ПК без Raspberry Pi."
                + "</html>");

        // Кнопка зупинки — неактивна до моменту запуску першого проекту
        stopButton = new JButton("\u25a0  Зупинити"); // ■ Зупинити
        stopButton.setEnabled(false);
        stopButton.setBackground(new Color(140, 35, 35));
        stopButton.setForeground(Color.WHITE);
        stopButton.setFocusPainted(false);
        stopButton.setOpaque(true);
        stopButton.setFont(new Font("SansSerif", Font.BOLD, 13));
        stopButton.setToolTipText("Зупинити поточний проект (викликає project.shutdown())");
        stopButton.addActionListener(e -> handleStopClick());

        left.add(mockCheckbox);
        left.add(stopButton);

        // ── Праворуч: рядок статусу ───────────────────────────────────────────
        statusLabel = new JLabel("Готово. Оберіть проект.");
        statusLabel.setForeground(COLOR_STATUS);
        statusLabel.setFont(new Font("SansSerif", Font.ITALIC, 13));

        panel.add(left,        BorderLayout.WEST);
        panel.add(statusLabel, BorderLayout.EAST);
        return panel;
    }

    // =========================================================================
    // ОБРОБНИКИ ПОДІЙ
    // =========================================================================

    /**
     * Called when a project row button is clicked.
     * Stops any currently running project, then starts the selected one.
     *
     * @param project project bound to the clicked button
     * @param btn     the clicked button (for colour highlighting)
     */
    private void handleProjectButtonClick(CrowPiProject project, JButton btn) {
        // Зупиняємо попередній проект — shutdown() + interrupt()
        runner.stop();

        // Скидаємо підсвічування попередньої активної кнопки
        if (activeButton != null) {
            activeButton.setBackground(COLOR_BTN_IDLE);
        }

        // Запускаємо обраний проект у фоновому daemon-потоці
        boolean mockMode = mockCheckbox.isSelected();
        runner.start(project, mockMode);

        // Підсвічуємо нову активну кнопку зеленим, вмикаємо Stop
        activeButton = btn;
        btn.setBackground(COLOR_BTN_ACTIVE);
        stopButton.setEnabled(true);

        // Праву панель залишаємо з описом поточного проекту + додаємо статус у рядок
        updateDescription(project);
        setStatus("\u25ba " + project.getName()
                + (mockMode ? "  [mock]" : "  [real GPIO]"));
    }

    /**
     * Called when the Stop button is clicked.
     * The actual UI state reset happens in {@link #handleProjectFinished} after
     * the runner thread terminates.
     */
    private void handleStopClick() {
        // Викликаємо shutdown() + interrupt() у ProjectRunner
        runner.stop();
        // Статус і кольори кнопок оновляться у колбеку handleProjectFinished()
    }

    /**
     * Callback invoked by {@link ProjectRunner} on the EDT when a project finishes
     * (normally, via stop(), or due to an error).
     *
     * @param projectName display name of the finished project
     * @param error       error message, or {@code null} if finished cleanly
     */
    private void handleProjectFinished(String projectName, String error) {
        // Захисна перевірка: гарантуємо виконання в EDT
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> handleProjectFinished(projectName, error));
            return;
        }

        // Скидаємо підсвічування активної кнопки
        if (activeButton != null) {
            activeButton.setBackground(COLOR_BTN_IDLE);
            activeButton = null;
        }

        stopButton.setEnabled(false);
        clearDescription();

        if (error != null) {
            setStatus("\u2717 Помилка (" + projectName + "): " + error,
                    new Color(255, 100, 100));
        } else {
            setStatus("\u2713 " + projectName + " завершено.");
        }
    }

    // =========================================================================
    // УТИЛІТИ UI
    // =========================================================================

    /**
     * Updates the status label with the default colour.
     * Thread-safe — can be called from any thread.
     *
     * @param text text to show
     */
    void setStatus(String text) {
        setStatus(text, COLOR_STATUS);
    }

    /**
     * Updates the status label text and colour.
     * Schedules the update on the EDT if called from a non-EDT thread.
     *
     * @param text  text to show
     * @param color foreground colour
     */
    void setStatus(String text, Color color) {
        // Swing-компоненти — лише в EDT; invokeLater безпечний навіть якщо вже в EDT
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(text);
            statusLabel.setForeground(color);
        });
    }

    // =========================================================================
    // ТОЧКА ВХОДУ
    // =========================================================================

    /**
     * Entry point for the {@code crowpi-ui-1.0.0.jar} fat JAR.
     *
     * <p>Sets the system Look &amp; Feel, configures tooltip timing, then
     * opens the {@link LauncherUI} window on the EDT.
     *
     * @param args command-line arguments (ignored)
     */
    public static void main(String[] args) {
        // Встановлюємо системний Look & Feel (GTK+ на Raspbian замість Java Metal)
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("[UI] Системний L&F недоступний, використовуємо Metal: "
                    + e.getMessage());
        }

        // Підказки: 300 мс до появи, 10 с відображення
        ToolTipManager.sharedInstance().setInitialDelay(300);
        ToolTipManager.sharedInstance().setDismissDelay(10_000);

        // Запускаємо UI в EDT — обов'язкова вимога Swing thread-safety
        SwingUtilities.invokeLater(() -> {
            LauncherUI ui = new LauncherUI();
            // Показуємо підказку в правій панелі після відображення вікна
            ui.clearDescription();
            ui.setVisible(true);
        });
    }
}

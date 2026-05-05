# Лабораторна робота №7
## Створення програм з графічним інтерфейсом користувача

---

### Тема

Створення програм з графічним інтерфейсом користувача (GUI) засобами бібліотеки Swing мови Java.

---

### Мета

Набути практичних навичок розробки застосунків з графічним інтерфейсом користувача з використанням бібліотеки Swing шляхом реалізації віконних компонентів, менеджерів розміщення та обробників подій.

---

### Теоретичні відомості

#### Огляд GUI-бібліотек Java

У Java існує кілька підходів до побудови GUI:
- **Swing** (`javax.swing`) — класична бібліотека, вбудована у JDK. Платформо-незалежна завдяки власній відрисовці компонентів.
- **JavaFX** — сучасніший фреймворк із підтримкою CSS, FXML та анімацій. З JDK 11 поставляється окремо (OpenJFX).
- **SWT** (Standard Widget Toolkit) — Eclipse-бібліотека з нативними компонентами ОС.

У цій лабораторній роботі використовується **Swing** як найдоступніший варіант без додаткових залежностей.

#### Основні компоненти Swing

| Клас | Призначення |
|------|-------------|
| `JFrame` | Головне вікно застосунку |
| `JPanel` | Контейнер для групування компонентів |
| `JLabel` | Текстова мітка |
| `JButton` | Кнопка |
| `JTextField` | Однорядкове текстове поле |
| `JTextArea` | Багаторядкове текстове поле |
| `JCheckBox` | Прапорець |
| `JRadioButton` | Перемикач |
| `JComboBox` | Випадний список |
| `JList` | Список елементів |
| `JTable` | Таблиця |
| `JMenuBar` / `JMenu` / `JMenuItem` | Рядок меню |
| `JFileChooser` | Діалог вибору файлу |
| `JOptionPane` | Стандартні діалоги (info, error, input) |

#### Структура Swing-застосунку

```java
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class MainWindow extends JFrame {

    private JTextField inputField;
    private JLabel resultLabel;

    public MainWindow() {
        super("Мій застосунок");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 300);
        setLocationRelativeTo(null); // центр екрану

        // Ініціалізація компонентів
        inputField = new JTextField(20);
        JButton calcButton = new JButton("Обчислити");
        resultLabel = new JLabel("Результат з'явиться тут");

        // Обробник події кліку
        calcButton.addActionListener(e -> onCalculate());

        // Розміщення компонентів
        JPanel panel = new JPanel(new FlowLayout());
        panel.add(new JLabel("Введіть значення:"));
        panel.add(inputField);
        panel.add(calcButton);
        panel.add(resultLabel);

        add(panel);
        setVisible(true);
    }

    private void onCalculate() {
        String text = inputField.getText().strip();
        if (text.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Введіть значення!", "Помилка", JOptionPane.ERROR_MESSAGE);
            return;
        }
        resultLabel.setText("Результат: " + text.toUpperCase());
    }

    public static void main(String[] args) {
        // Swing-компоненти слід створювати у EDT (Event Dispatch Thread)
        SwingUtilities.invokeLater(MainWindow::new);
    }
}
```

#### Менеджери розміщення (Layout Managers)

| Менеджер | Опис |
|----------|------|
| `FlowLayout` | Розміщення зліва направо, перенос на новий рядок |
| `BorderLayout` | 5 зон: North, South, East, West, Center |
| `GridLayout` | Рівна сітка N×M |
| `GridBagLayout` | Гнучка сітка з обмеженнями |
| `BoxLayout` | Вертикальне або горизонтальне розміщення |

```java
JPanel panel = new JPanel(new BorderLayout());
panel.add(new JLabel("Заголовок"), BorderLayout.NORTH);
panel.add(new JScrollPane(textArea),  BorderLayout.CENTER);
panel.add(okButton,                   BorderLayout.SOUTH);
```

#### Подієва модель

Swing використовує патерн «Спостерігач» (Observer): компонент генерує подію → слухачі (listeners) обробляють її. З Java 8 зручно використовувати лямбди замість анонімних класів:

```java
button.addActionListener(e -> System.out.println("Натиснуто!"));
textField.addKeyListener(new KeyAdapter() {
    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) onCalculate();
    }
});
```

#### Look and Feel

```java
// Системний L&F (схожий на нативні вікна ОС)
UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
```

#### Приклад з репозиторію java-iot

Клас [`LauncherUI.java`](https://github.com/AndriyLutskiv/java-iot/blob/main/ui/src/main/java/ua/crowpi/ui/LauncherUI.java) реалізує графічний лаунчер для вибору та запуску IoT-проєктів. Клас [`ProjectRunner.java`](https://github.com/AndriyLutskiv/java-iot/blob/main/ui/src/main/java/ua/crowpi/ui/ProjectRunner.java) виконує вибраний проєкт у фоновому потоці. Рекомендується проаналізувати, як у них організовано компоновку вікна, обробку подій та взаємодію з фоновим потоком.

---

### Хід виконання роботи

1. Відкрити та проаналізувати `LauncherUI.java` та `ProjectRunner.java` (посилання вище). Визначити: які компоненти Swing використано, як організовано EDT, як фоновий потік оновлює UI.
2. Створити головне вікно (`JFrame`) з заголовком, мінімальним розміром 600×400 px та розміщенням у центрі екрану.
3. Реалізувати компоненти форми відповідно до варіанту. Використати принаймні 3 різних типи компонентів та 2 різні менеджери розміщення.
4. Реалізувати обробники подій для всіх інтерактивних елементів (кнопки, поля введення, список). Для кнопок — через лямбду або `ActionListener`.
5. Реалізувати валідацію введених даних з виведенням повідомлень через `JOptionPane`.
6. Додати рядок меню (`JMenuBar`) з розділами «Файл» (Відкрити, Зберегти, Вихід) та «Допомога» (Про програму). Реалізувати пункти «Вихід» та «Про програму».
7. Реалізувати діалог «Про програму» з назвою застосунку, версією та автором (через `JOptionPane` або `JDialog`).
8. Переконатися, що всі компоненти створюються та оновлюються в EDT (`SwingUtilities.invokeLater`). Продемонструвати проблему при виклику UI поза EDT (якщо актуально для варіанту).
9. Застосувати Look and Feel системного стилю.
10. Зробити знімки екрану головного вікна, діалогів та результатів роботи програми.
11. Оформити звіт: вихідний код, знімки екрану з поясненнями, опис подієвої моделі застосунку.

---

### Індивідуальні завдання

Розробити Swing-застосунок (2–3 класи). Обсяг: до 150 рядків коду. Обов'язково: щонайменше 3 типи компонентів, рядок меню, валідація, EDT.

**Варіант 1.** Калькулятор: 4 поля (`JTextField`) для двох операндів, знаку операції (`JComboBox`: +, -, *, /) та результату. Кнопки «Обчислити» та «Очистити». Обробка ділення на нуль через `JOptionPane`.

**Варіант 2.** Записник: `JTextArea` зі `JScrollPane`, кнопки «Відкрити файл», «Зберегти файл» через `JFileChooser`. Відображення шляху до поточного файлу у заголовку вікна. Питання про збереження перед виходом.

**Варіант 3.** Конвертор одиниць: `JComboBox` для вибору типу (відстань, температура, маса), два `JTextField` (вхід/вихід), кнопка «Конвертувати». При зміні значення у першому полі — автоматичний перерахунок через `DocumentListener`.

**Варіант 4.** Список завдань (To-Do): `JList` зі списком завдань, `JTextField` для введення нового завдання, кнопки «Додати», «Видалити», «Позначити виконаним» (зміна кольору рядка через `ListCellRenderer`).

**Варіант 5.** Тест-вікторина: питання відображається у `JLabel`, 4 варіанти відповіді — `JRadioButton` у `ButtonGroup`. Кнопки «Далі» та «Завершити». Після останнього питання — діалог з результатом (кількість правильних відповідей).

**Варіант 6.** Телефонна книга: `JTable` зі стовпцями «Ім'я», «Телефон», «Email»; кнопки «Додати», «Видалити», «Знайти». Пошук фільтрує рядки таблиці за введеним текстом через `TableRowSorter`.

**Варіант 7.** Реєстраційна форма: поля `JTextField` для імені, прізвища, email; `JComboBox` для курсу (1–6); `JCheckBox` для «Погоджуюсь з умовами»; кнопка «Зареєструватися» (активна лише при позначеному прапорці). Підтвердження реєстрації — `JOptionPane`.

**Варіант 8.** Генератор паролів: `JSlider` для довжини (8–32), `JCheckBox` для включення: великі, малі, цифри, спецсимволи. Кнопка «Згенерувати» → відображення у `JTextField` (тільки для читання). Кнопка «Копіювати» → буфер обміну (`Toolkit.getDefaultToolkit().getSystemClipboard()`).

**Варіант 9.** Прості нотатки: вікно з вкладками (`JTabbedPane`), кожна вкладка — `JTextArea`. Кнопки «Додати вкладку», «Закрити поточну». Зберігати вміст вкладок у `Map<String, String>`.

**Варіант 10.** Натхненний [`LauncherUI.java`](https://github.com/AndriyLutskiv/java-iot/blob/main/ui/src/main/java/ua/crowpi/ui/LauncherUI.java): лаунчер програм. `JList` зі списком операцій (назви власних навчальних класів). При виборі — опис у `JTextArea`. Кнопка «Запустити» → виконання у `SwingWorker`.

**Варіант 11.** Прогрес-бар задачі: кнопка «Почати завантаження», `JProgressBar` від 0 до 100%. Симуляція через `javax.swing.Timer` (збільшення на 5% кожні 200 мс). Після завершення — `JOptionPane` з повідомленням. Кнопка «Скасувати».

**Варіант 12.** Форма звіту: `JTextField` для назви, `JTextArea` для опису, `JSpinner` для дати (модель `SpinnerDateModel`). Кнопка «Попередній перегляд» — виводить форматований звіт у новий `JDialog` з `JTextArea`.

**Варіант 13.** Малювалка: `JPanel` з перевизначеним `paintComponent`, `JColorChooser` для вибору кольору пера, `JSlider` для товщини. Малювання через `MouseListener`/`MouseMotionListener`. Кнопка «Очистити».

**Варіант 14.** Таблиця оцінок: `JTable` з `DefaultTableModel` та стовпцями «Студент», «Предмет 1»…«Предмет 5». Кнопки «Додати рядок», «Видалити рядок». У рядку стану (нижня `JLabel`) — середній бал виділеного студента.

**Варіант 15.** Менеджер контактів: вікно з трьома панелями (`JSplitPane`): ліво — `JList` контактів, центр — деталі у `JPanel` з `GridLayout`, право — `JTextArea` нотаток. Кнопки «Додати», «Видалити», «Зберегти зміни».

---

### Контрольні запитання

1. Що таке EDT (Event Dispatch Thread) у Swing? Чому небезпечно оновлювати GUI поза EDT?
2. Що таке `SwingUtilities.invokeLater` та `SwingWorker`? Коли використовувати кожен?
3. Що таке менеджер розміщення (Layout Manager)? Чому не рекомендується використовувати `AbsoluteLayout` (`null` layout)?
4. Яка різниця між `JFrame.EXIT_ON_CLOSE` та `WindowAdapter.windowClosing`?
5. Що таке Look and Feel (L&F)? Які стандартні L&F постачаються з JDK?
6. Що таке `JScrollPane` і навіщо його застосовувати з `JTextArea` або `JList`?
7. Яка різниця між `ActionListener` та `MouseListener`? Наведіть приклад, де кожен є доречним.
8. Що таке `DefaultTableModel`? Як динамічно додавати рядки до `JTable`?
9. Що таке `DocumentListener`? Для якої задачі він використовується?
10. Що таке `ButtonGroup`? Навіщо вона потрібна для `JRadioButton`?
11. Як відкрити файл за допомогою `JFileChooser`? Як встановити фільтр для типів файлів?
12. Що таке `JOptionPane.showInputDialog`? Як обробити натискання кнопки «Скасувати»?
13. Що таке `ListCellRenderer`? Як змінити відображення окремих елементів у `JList`?
14. Як отримати текст з `JTextField` і встановити його програмно?
15. Що таке `javax.swing.Timer`? Чим він відрізняється від `java.util.Timer` для задач GUI?

---

### Перелік посилань

1. Horstmann, C. S. *Core Java, Volume II — Advanced Features* (12th ed.). Hoboken : Pearson, 2022. 832 p.
2. Schildt, H. *Java: The Complete Reference* (12th ed.). New York : McGraw-Hill, 2021. 1248 p.
3. Oracle Corporation. *Creating a GUI With Swing — Java Tutorials* [Електронний ресурс]. URL: https://docs.oracle.com/javase/tutorial/uiswing/ (дата звернення: 2025-09-01).
4. Oracle Corporation. *javax.swing Package API* [Електронний ресурс]. URL: https://docs.oracle.com/en/java/javase/25/docs/api/java.desktop/javax/swing/package-summary.html (дата звернення: 2025-09-01).
5. Oracle Corporation. *Concurrency in Swing* [Електронний ресурс]. URL: https://docs.oracle.com/javase/tutorial/uiswing/concurrency/ (дата звернення: 2025-09-01).
6. Loy, M., Eckstein, R., Wood, D., Elliott, J., Cole, B. *Java Swing* (2nd ed.). Sebastopol : O'Reilly Media, 2002. 1278 p.
7. Bloch, J. *Effective Java* (3rd ed.). Boston : Addison-Wesley, 2018. 412 p.

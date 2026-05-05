#!/bin/bash
# =============================================================================
# run-ui.sh — запуск CrowPi Suite через графічний Swing-інтерфейс
# =============================================================================
#
# Запускає crowpi-ui-1.0.0.jar від імені root через 'sudo su -'.
# Root-права необхідні для доступу до GPIO, I2C, SPI, PWM на Raspberry Pi.
#
# Вимоги до середовища:
#   - Підключений дисплей (7" CrowPi або HDMI) або активний X11-сервер
#   - Запущений X11-сервер (LXDE / startx на Raspberry Pi OS)
#   - Java 11: /usr/lib/jvm/java-11-openjdk-amd64
#
# Збірка UI JAR (якщо ще не зібраний):
#   JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64 ./gradlew :ui:shadowJar
#
# Примітка щодо X11 і root:
#   При переході в root через 'sudo su -' root-процес може не мати доступу
#   до X11-дисплея поточного користувача. Цей скрипт вирішує проблему
#   передачею DISPLAY і XAUTHORITY у root-оболонку.
# =============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Шлях до UI fat JAR (результат gradle :ui:shadowJar)
UI_JAR="${SCRIPT_DIR}/ui/build/libs/crowpi-ui-1.0.0.jar"

# ── Перевірка наявності UI JAR ───────────────────────────────────────────────
if [ ! -f "$UI_JAR" ]; then
    echo "================================================================="
    echo " ПОМИЛКА: UI JAR-файл не знайдено:"
    echo "   $UI_JAR"
    echo ""
    echo " Виконайте збірку командою:"
    echo "   JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64 ./gradlew :ui:shadowJar"
    echo "================================================================="
    exit 1
fi

# ── Налаштування X11-змінних ─────────────────────────────────────────────────
#
# DISPLAY — вказує X-серверу, на якому дисплеї відображати вікно.
# :0 — перший локальний X11-дисплей, типовий для RPi з LXDE або startx.
# Якщо DISPLAY вже встановлено у поточному оточенні — використовуємо його значення.
DISPLAY="${DISPLAY:-:0}"

# XAUTHORITY — файл з ключами авторизації для підключення до X11-сервера.
# Root за замовчуванням не має доступу до X-сесії звичайного користувача.
# Визначаємо шлях до .Xauthority оригінального користувача (той, хто запустив sudo).
# $SUDO_USER — змінна, яку sudo встановлює автоматично (ім'я користувача до підвищення прав).
if [ -z "$XAUTHORITY" ]; then
    if [ -n "$SUDO_USER" ]; then
        # Якщо скрипт запущений через sudo — беремо .Xauthority оригінального юзера
        XAUTHORITY="/home/${SUDO_USER}/.Xauthority"
    else
        # Якщо запущено не через sudo (напряму як root) — типовий шлях
        XAUTHORITY="${HOME}/.Xauthority"
    fi
fi

# Дозволяємо root-процесу підключатись до X11-сервера від імені поточного користувача.
# xhost +local:root додає root до списку дозволених клієнтів X-сервера.
# Помилку ігноруємо (2>/dev/null) — команда може бути недоступна в деяких конфігураціях.
xhost +local:root 2>/dev/null || true

echo "================================================================="
echo " CrowPi Suite — графічний Swing-запуск"
echo " JAR:          $UI_JAR"
echo " DISPLAY:      $DISPLAY"
echo " XAUTHORITY:   $XAUTHORITY"
echo " Запуск від root через 'sudo su -' ..."
echo "================================================================="

# ── Запуск від root через 'sudo su -' ────────────────────────────────────────
#
# DISPLAY і XAUTHORITY явно передаємо у root-оболонку через -c "VAR=val ...".
# Без цього root отримає порожній DISPLAY і Swing не зможе відкрити вікно
# (викине HeadlessException або "Cannot connect to X server").
#
# exec замінює поточний bash-процес на sudo — заощаджує один fork і
# коректно передає сигнали (Ctrl+C) до java-процесу.
exec sudo su - -c "DISPLAY=\"${DISPLAY}\" XAUTHORITY=\"${XAUTHORITY}\" java -jar \"${UI_JAR}\""

#!/bin/bash
# =============================================================================
# build.sh — конвертація методичних вказівок md → odt + pdf
# Вимоги: pandoc >= 3.x, texlive-xetex (для PDF через XeLaTeX)
# Використання: ./build.sh [odt|pdf|all]
#              Якщо аргумент не передано — виконується 'all'
# =============================================================================

set -e

# --- Налаштування ---
OUTPUT_DIR="."
OUTPUT_NAME="metod_java_labs"
REFERENCE_ODT="reference.odt"
LANG="uk"

# Порядок вхідних файлів
MD_FILES=(
    "00_title.md"
    "00_abbreviations.md"
    "00_intro.md"
    "00_toc.md"
    "lab_01.md"
    "lab_02.md"
    "lab_03.md"
    "lab_04.md"
    "lab_05.md"
    "lab_06.md"
    "lab_07.md"
    "lab_08.md"
    "lab_09.md"
    "lab_10.md"
)

# --- Функції ---
log_info()  { echo "[INFO]  $*"; }
log_ok()    { echo "[OK]    $*"; }
log_error() { echo "[ERROR] $*" >&2; }

check_pandoc() {
    if ! command -v pandoc &>/dev/null; then
        log_error "pandoc не знайдено. Встановіть: sudo apt install pandoc"
        exit 1
    fi
    PANDOC_VERSION=$(pandoc --version | head -1 | awk '{print $2}')
    log_info "Знайдено pandoc версії $PANDOC_VERSION"
}

check_files() {
    local missing=0
    for f in "${MD_FILES[@]}"; do
        if [[ ! -f "$f" ]]; then
            log_error "Файл не знайдено: $f"
            missing=1
        fi
    done
    if [[ $missing -eq 1 ]]; then
        log_error "Деякі вхідні файли відсутні. Генерацію перервано."
        exit 1
    fi
}

build_odt() {
    log_info "Генерація ODT: ${OUTPUT_NAME}.odt ..."

    local ref_args=()
    if [[ -f "$REFERENCE_ODT" ]]; then
        log_info "Використовується шаблон стилів: $REFERENCE_ODT"
        ref_args=(--reference-doc="$REFERENCE_ODT")
    else
        log_info "Шаблон $REFERENCE_ODT не знайдено — використовується стиль pandoc за замовчуванням"
    fi

    pandoc \
        "${MD_FILES[@]}" \
        --from=markdown \
        --to=odt \
        --output="${OUTPUT_DIR}/${OUTPUT_NAME}.odt" \
        --toc \
        --toc-depth=3 \
        --metadata=lang:"$LANG" \
        --metadata=title:"Методичні вказівки до виконання лабораторних робіт з дисципліни «Програмування мовою Java»" \
        --metadata=author:"Луцків А. М., Осухівська Г.М." \
        --metadata=date:"2025" \
        "${ref_args[@]}" \
        2>&1

    log_ok "ODT збережено: ${OUTPUT_DIR}/${OUTPUT_NAME}.odt"
}

build_pdf() {
    log_info "Генерація PDF: ${OUTPUT_NAME}.pdf ..."

    # Перевірка наявності XeLaTeX
    if ! command -v xelatex &>/dev/null; then
        log_error "xelatex не знайдено. Встановіть: sudo apt install texlive-xetex texlive-lang-cyrillic"
        log_error "Альтернатива: sudo apt install wkhtmltopdf  та змінити --pdf-engine=wkhtmltopdf"
        exit 1
    fi

    pandoc \
        "${MD_FILES[@]}" \
        --from=markdown \
        --to=pdf \
        --output="${OUTPUT_DIR}/${OUTPUT_NAME}.pdf" \
        --pdf-engine=xelatex \
        --toc \
        --toc-depth=3 \
        --metadata=lang:"$LANG" \
        --metadata=title:"Методичні вказівки до виконання лабораторних робіт з дисципліни «Програмування мовою Java»" \
        --metadata=author:"Луцків А. М., Осухівська Г.М." \
        --metadata=date:"2025" \
        -V mainfont="DejaVu Serif" \
        -V monofont="DejaVu Sans Mono" \
        -V fontsize=12pt \
        -V papersize=a4 \
        -V geometry="margin=2.5cm" \
        -V colorlinks=true \
        2>&1

    log_ok "PDF збережено: ${OUTPUT_DIR}/${OUTPUT_NAME}.pdf"
}

# --- Точка входу ---
check_pandoc

# Перейти до директорії скрипта
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

check_files

MODE="${1:-all}"

case "$MODE" in
    odt)
        build_odt
        ;;
    pdf)
        build_pdf
        ;;
    all)
        build_odt
        build_pdf
        ;;
    *)
        log_error "Невідомий режим: $MODE"
        echo "Використання: $0 [odt|pdf|all]"
        exit 1
        ;;
esac

log_ok "Готово!"

# p03 — IR Remote Music Box

Play nine built-in melodies using an IR remote. Buttons 1-9 select a melody,
button 0 stops playback. A 16×2 LCD shows the melody name and a progress bar
made of block characters.

## Hardware wiring

| Component      | Pin / Bus         | Notes                         |
|----------------|-------------------|-------------------------------|
| IR receiver    | GPIO 20 (BCM), physical pin 38 | Output to GPIO pin   |
| Passive buzzer | GPIO 12 (BCM/PWM) | Hardware-PWM channel 0        |
| LCD (I²C)      | SDA/SCL (I²C-1)   | Address 0x21 (PCF8574 backpack)|

## Melody library

| Button | Melody                  |
|--------|-------------------------|
| 1      | Happy Birthday          |
| 2      | Twinkle Twinkle         |
| 3      | Jingle Bells            |
| 4      | Ode to Joy              |
| 5      | Imperial March          |
| 6      | Super Mario Theme       |
| 7      | Tetris Theme            |
| 8      | Fur Elise               |
| 9      | Zelda Theme             |
| 0      | Stop playback           |

## Running

```bash
# Hardware mode (on Raspberry Pi)
java -jar crowpi-suite-1.0.0.jar --project p03

# Mock mode (any machine)
java -jar crowpi-suite-1.0.0.jar --project p03 --mock
```

## LCD display

Row 0: melody name (up to 16 chars)
Row 1: progress bar — `████████░░░░░░░░` (filled/empty blocks)

## Key classes

| Class               | Responsibility                                   |
|---------------------|--------------------------------------------------|
| `MelodyLibrary`     | Static catalogue of 9 melodies with Hz values    |
| `Note`              | Frequency + duration pair                        |
| `MelodyPlayer`      | PWM-based note playback with stop/skip support   |
| `IrCodeDecoder`     | Maps raw IR codes to button numbers 0-9          |
| `LcdProgressRenderer` | Renders melody name + block progress bar       |
| `MusicBoxProject`   | Main loop: IR listener → player dispatch         |

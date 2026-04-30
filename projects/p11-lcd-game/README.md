# p11 — LCD Platform Game

A playable 2-row platform game rendered on a 16×2 character LCD. The player
jumps between platforms, collects coins, and avoids enemies. Scores are
persisted to a JSON leaderboard (top 10, sorted descending).

## Hardware wiring

| Component          | Pin / Bus         | Notes                           |
|--------------------|-------------------|---------------------------------|
| LCD (I²C)          | SDA/SCL (I²C-1)   | Address 0x27; PCF8574 backpack  |
| Left button        | GPIO 26 (BCM)     | Move player left                |
| Right button       | GPIO 19 (BCM)     | Move player right               |
| Jump button        | GPIO 13 (BCM)     | Jump / start game / restart     |
| Pause button       | GPIO 6  (BCM)     | Toggle pause                    |
| Buzzer             | GPIO 12 (BCM/PWM) | Sound effects                   |

## Game rules

- The game world is 16 columns wide × 2 rows tall.
- Row 1 is the ground; row 0 is the upper row.
- Three platforms occupy the upper row at fixed positions.
- Two enemies patrol the ground, bouncing between walls.
- Four coins are scattered across both rows.
- Collect all 4 coins to **WIN**. Lose all 3 health points to get **GAME OVER**.
- Press **JUMP** after game over or win to restart.

## Physics

| Mechanic    | Behaviour                                        |
|-------------|--------------------------------------------------|
| Gravity     | Every tick: if not on platform/ground → fall     |
| Jump        | Sets y=0, sets `jumpTicksRemaining=3`            |
| Platform    | Any cell covered by a `Platform` object holds y=0|

## Sound effects

| Event           | Frequency | Duration |
|-----------------|-----------|----------|
| Jump            | 900 Hz    | 80 ms    |
| Coin collect    | 1200 Hz   | 100 ms   |
| Enemy hit       | 400 Hz    | 300 ms   |
| Win             | 784 Hz    | 500 ms   |
| Game Over       | 262 Hz    | 800 ms   |

## Leaderboard

Top 10 scores are saved to `scores.json` in the working directory:

```json
[{"name":"PLAYER","score":4},{"name":"PLAYER","score":3}]
```

## Running

```bash
# Hardware mode
java -jar crowpi-suite-1.0.0.jar --project p11

# Mock mode (simulated button events via MockGpioFacade)
java -jar crowpi-suite-1.0.0.jar --project p11 --mock
```

## Key classes

| Class           | Responsibility                                              |
|-----------------|-------------------------------------------------------------|
| `GameWorld`     | Holds player, platforms, enemies, coins; `reset()`         |
| `GameEngine`    | 100 ms tick scheduler; button handlers; state machine      |
| `Physics`       | Static: gravity, jump, platform detection, coin collection |
| `LcdRenderer`   | Double-buffered `char[2][16]`; writes only changed cells   |
| `Enemy`         | Thread-safe `synchronized move()` between bounds           |
| `LeaderBoard`   | Jackson-based top-10 JSON persistence                      |
| `SoundEffects`  | Enum; `play(GpioFacade)` via PWM                           |
| `LcdCharset`    | Custom character bitmaps (player, enemy, coin, platform)   |

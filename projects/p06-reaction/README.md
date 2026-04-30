# p06 — Reaction Speed Trainer

A two-player competitive reaction game. Both players watch the same LED; when
it flashes, each must press their button as quickly as possible. The first to
press wins the round. Pressing before the LED lights up is a false start and
costs −1 point.

## Hardware wiring

| Component          | Pin / Bus     | Notes                             |
|--------------------|---------------|-----------------------------------|
| RGB LED – Red      | GPIO 17 (BCM) | "Ready" colour during countdown   |
| RGB LED – Green    | GPIO 27 (BCM) | "Go!" flash                       |
| Player 1 button    | GPIO 26 (BCM) | Pull-down input                   |
| Player 2 button    | GPIO 19 (BCM) | Pull-down input                   |
| Buzzer             | GPIO 12 (BCM) | Confirmation beep on round end    |

## Scoring

| Event                   | Points |
|-------------------------|--------|
| Fastest correct press   | +1     |
| False start (too early) | −1     |
| Slow but correct press  | 0      |

First player to reach **5 points** wins the match (configurable in source).

## Running

```bash
# Hardware mode
java -jar crowpi-suite-1.0.0.jar --project p06

# Mock mode (auto-generates button presses after random delay)
java -jar crowpi-suite-1.0.0.jar --project p06 --mock
```

## Key classes

| Class             | Responsibility                                              |
|-------------------|-------------------------------------------------------------|
| `GameRound`       | Holds timestamps; `applyScores()` determines winner        |
| `ReactionEngine`  | Thread-safe game loop; `runOneRound()` for unit testing    |
| `PlayerScore`     | Mutable score for each player                              |
| `RoundResult`     | Outcome enum: `P1_WINS`, `P2_WINS`, `P1_FALSE`, `P2_FALSE`|
| `ReactionProject` | GPIO wiring + match loop                                   |

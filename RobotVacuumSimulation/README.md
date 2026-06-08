# Robot Vacuum Cleaning Simulation

A JavaFX-based desktop simulation of a robot vacuum cleaner that autonomously navigates a room, avoids obstacles, detects and cleans different types of dirt, manages battery life, and returns to its charging station when needed.

---

## Course Information

| | |
|---|---|
| **Course** | BZ 214 Visual Programming |
| **Group** | Group 4 |

## Group Members

| Student No | Name |
|------------|------|
| 1030511095 | Çağrı Kaan Yanık |
| 1030510843 | Salih Bayraktar |
| 1030521203 | Fatma Gürler |

---

## Requirements

- Java 21+
- JavaFX 21.0.6 (included via Maven)
- Apache Maven 3.8+
- IntelliJ IDEA (recommended) or any Maven-compatible IDE

---

## How to Run

### Option 1 — IntelliJ IDEA (Recommended)

1. Open IntelliJ IDEA
2. Click **File → Open** and select the `RobotVacuumSimulation` folder
3. Wait for Maven to download dependencies automatically
4. Run the `Launcher.java` class or click the green play button

### Option 2 — Maven Command Line

```bash
cd RobotVacuumSimulation
mvn clean javafx:run
```

---

## Project Structure

```
src/main/java/com/example/robotvacuumsimulation/
│
├── Main.java                  # JavaFX Application entry point
├── Launcher.java              # JavaFX module-compatible launcher
│
├── model/
│   ├── Robot.java             # Robot state: position, battery, direction
│   ├── Room.java              # 20x14 grid management
│   ├── Cell.java              # Single grid cell: obstacle, dirt, charging station
│   ├── DirtType.java          # DUST / LIQUID / STAIN enum with cleaning data
│   ├── Direction.java         # NORTH / SOUTH / EAST / WEST enum
│   ├── RoomType.java          # SALON / BEDROOM / OFFICE enum
│   └── Pet.java               # Cat agent with autonomous movement
│
├── view/
│   ├── SimulationView.java    # Main canvas rendering + left/bottom UI panels
│   └── RoomSelectionScreen.java # Opening room selection screen
│
├── controller/
│   └── SimulationController.java # Game loop, BFS pathfinding, algorithms, state machine
│
└── util/
    ├── RoomLoader.java        # Room layout and initial dirt loader
    └── SoundManager.java      # Procedural sound synthesis (javax.sound)
```

---

## Features

### Core Features
- 20x14 interactive grid room simulation
- Robot avoids walls and furniture automatically
- 3 dirt types with different cleaning durations and battery costs:
  - **Dust** — fast to clean, low battery cost
  - **Liquid** — medium cleaning time, medium battery cost
  - **Stain** — slow to clean, high battery cost
- Real-time stats: position, direction, battery level, cleaned area %, remaining dirt, elapsed time
- BFS pathfinding for shortest return path to charging station
- Gradual battery charging at the station (5% per tick)
- Robot automatically returns to charging station when battery ≤ 20%

### Cleaning Algorithms
1. **Random** — forward → right → left → backward priority
2. **Spiral** — outward expanding square spiral
3. **Wall Following** — right-hand rule edge tracing

### User Interface Controls
- Add dirt (Dust / Liquid / Stain) by clicking on the grid
- Add furniture (Sofa 6x2 / Table 3x3 / Wall 1x1) by clicking on the grid
- Remove placed furniture
- Adjust robot speed (0.5x – 3.0x)
- Manually set battery level via slider
- Start / Pause / Reset simulation
- Send robot back to charging station manually

### Bonus Features Implemented
- **Unreachable area detection** — flood fill from robot position
- **Multiple room layouts** — Salon, Bedroom, Office with room transition animations
- **Sound effects** — procedurally generated vacuum, cleaning, and charging sounds
- **Cleaning animation** — expanding ring and particle effect per dirt type
- **Cat (Pet) agent** — autonomous cat that moves around the salon

---

## MVC Architecture

| Layer | Classes |
|-------|---------|
| **Model** | `Robot`, `Room`, `Cell`, `DirtType`, `Direction`, `RoomType`, `Pet` |
| **View** | `SimulationView`, `RoomSelectionScreen` |
| **Controller** | `SimulationController` |
| **Utilities** | `RoomLoader`, `SoundManager` |

---

## Acknowledgment

This project was developed as part of the **BZ 214 Visual Programming** course.  
Special thanks to the course instructor and contributors.  
Special thanks to **Gökhan AZİZOĞLU**.

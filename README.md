# CivSim: Multi-Generational Interactive Ecosystem (MIE)

[![Java](https://img.shields.io/badge/Java-21%2B%20%7C%2025%20LTS-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.4-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18.3-61DAFB?logo=react&logoColor=black)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.6-3178C6?logo=typescript&logoColor=white)](https://www.typescriptlang.org/)
[![Vite](https://img.shields.io/badge/Vite-6.0-646CFF?logo=vite&logoColor=white)](https://vitejs.dev/)
[![Tailwind CSS](https://img.shields.io/badge/Tailwind-3.4-38B2AC?logo=tailwind-css&logoColor=white)](https://tailwindcss.com/)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**CivSim** is a deterministic, procedural civilization and fantasy ecosystem simulator. It models multi-generational human demographics, physical resource economies, warfare, and an emergent supernatural era (*"The Awakening"*), fully observable in real time through an interactive, strategy-game-inspired living world map.

---

## Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [System Architecture](#system-architecture)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Option 1: Quick Launch (All-in-One Application)](#option-1-quick-launch-all-in-one-application)
  - [Option 2: Frontend Development Mode (Hot Reloading)](#option-2-frontend-development-mode-hot-reloading)
- [Interactive Map & Controls](#interactive-map--controls)
- [API & Integration Overview](#api--integration-overview)
- [Project Structure](#project-structure)
- [License](#license)

---

## Overview

CivSim simulates the rise, evolution, and conflicts of civilizations across centuries. Worlds are seeded and strictly deterministic: identical random seeds generate identical demographic trajectories, economic trade flows, and historical milestones.

The simulation transitions from a **Mundane Era** focused on survival, agriculture, resource supply chains, and civic governance into an **Awakening Era** where supernatural forces, martial orders, ancient monster lairs, and aetheric anomalies emerge across the landscape.

---

## Key Features

### 1. Living World Map & Visual Hierarchy
- **Illustrated Settlements**: Visual progression from rural hamlets to fortified citadels with contextual badges, walls, and biome-appropriate styling.
- **Dynamic Terrain & Overlays**: Illustrated procedural canvas with biome gradients, road connections, danger zones, and resource deposits.
- **Contextual Inspection**: Instant selection and deep inspection of settlements, monster lairs, anomalies, families, and expeditions.
- **Adaptive Aesthetics**: Seamless theme transition from warm historical parchment in the Mundane Era to aetheric slate and indigo during the Awakening Era.

### 2. Multi-Generational Demographics & Dynasties
- **Genealogy & Lineage**: Detailed citizen lifespans, marriage networks, inheritance rights, and multi-generational family trees.
- **Clans & Organizations**: Noble houses, mercenary guilds, cultivation sects, and dark covens with independent leadership and diplomatic stances.
- **Ancestral Heirlooms & Blood Feuds**: Crafted weapons and forged artifacts passed through generations, creating inter-family vendettas, vengeance raids, or negotiated reconciliations.

### 3. Economy, Crafting & Warfare
- **Physical Stockpiles**: Discrete commodities (`Food`, `Wood`, `Ore`, `Tools`) with supply/demand dynamics and seasonal winter famine pressures.
- **Multi-Tiered Banking**: Metallic currency systems (`Iron`, `Silver`, `Gold`, `Platinum`) with settlement treasuries, loans, and civic investments.
- **Tactical Combat & Monster Ecology**: Multi-round battle resolution, territorial monster lairs with expanding threat zones, and high-stakes MonsterTide invasion waves.

### 4. Historical Time Travel & Alternate Timelines
- **Historical Snapshots**: Automated interval checkpoints capturing complete world states across centuries.
- **Timeline Branching**: Create alternate reality branches at historical dates to simulate "what-if" divergences under isolated random number generation.
- **Comparative Analytics**: Side-by-side time series and demographic comparisons between historical timelines.

---

## System Architecture

CivSim uses a decoupled architecture designed for thread-safe execution, high performance, and real-time streaming:

```text
┌────────────────────────────────────────────────────────────────────────┐
│                        CivSim Web Client                               │
│        (React 18, TypeScript, Tailwind CSS, Zustand, Vite)             │
└──────────────────┬────────────────────────────────▲────────────────────┘
                   │                                │
      HTTP REST    │                                │ Server-Sent Events
   (Commands/Data) │                                │ (Live Tick Stream)
                   ▼                                │
┌───────────────────────────────────────────────────┴────────────────────┐
│                      Spring Boot Service Layer                         │
│     - REST Controllers (World, Settlements, People, Battles, Timeline) │
│     - SSE Event Stream Engine (Buffered non-blocking dispatch)        │
└──────────────────┬────────────────────────────────▲────────────────────┘
                   │ Submit Tasks                   │ Publish Snapshots
                   ▼                                │
┌───────────────────────────────────────────────────┴────────────────────┐
│                    Simulation Runner (Single Writer)                   │
│     - Dedicated worker thread ensuring thread-safe mutations           │
│     - Copy-on-publish immutable state snapshots                        │
│     - Atomic file persistence (.tmp -> .json swap)                     │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │ Sequential Ticks
                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│                      Simulation Engine Domain Core                     │
│  - Demographics & Dynasties           - Economy & Banking              │
│  - Combat Engine & Monster Ecology    - Supernatural Classes & Mastery │
│  - Anomalies, Expeditions & Crafting  - Timeline & Checkpoints         │
└────────────────────────────────────────────────────────────────────────┘
```

- **Single-Writer Thread Safety**: All world mutations execute sequentially on an isolated simulation worker thread, preventing race conditions.
- **Non-Blocking Read Snapshots**: Web controllers serve immutable snapshot models published after each simulation tick, ensuring zero lock contention.
- **Atomic State Persistence**: Save games and checkpoints are written using two-phase atomic file swaps to eliminate file corruption.

---

## Getting Started

### Prerequisites

- **Java Development Kit (JDK)**: JDK 21 or higher (JDK 25 compatible).
- **Node.js**: Node 18 or higher with `npm` (required for web frontend development).

---

### Option 1: Quick Launch (All-in-One Application)

The compiled frontend is bundled with the backend application. A single command launches the web server and simulation engine together:

#### Windows:
```powershell
# From the project root
npm start
```
*Alternatively, double-click `run.bat` or execute `./run.ps1` in the project root.*

#### Manual Launch:
```powershell
cd backend
java -cp "bin;resources;lib/*" civsim.web.CivSimWebApplication
```

Once started, open your web browser at:
👉 **`http://localhost:8085`**

---

### Option 2: Frontend Development Mode (Hot Reloading)

To modify and live-edit the React/TypeScript frontend with instant hot-reloading:

#### 1. Start the Backend:
```powershell
npm run start
```
*(Or in `backend`: `java -cp "bin;resources;lib/*" civsim.web.CivSimWebApplication`)*

#### 2. Start the Frontend Dev Server:
```powershell
npm run dev
```
*(Or in `frontend`: `npm run dev`)*

Open your browser at:
👉 **`http://localhost:5173`**  
*(Vite automatically proxies API calls and live event streams to backend port `8085`).*

---

## Interactive Map & Controls

| Action | Input / Shortcut |
| :--- | :--- |
| **Pan Map** | Click and drag on the map canvas |
| **Zoom In / Out** | Mouse wheel (anchored to cursor position) or `+` / `-` keys |
| **Reset View** | Click **Reset View** or press `0` to fit the world |
| **Inspect Entity** | Click on any settlement, monster lair, anomaly, or expedition |
| **Minimap Navigation** | Click or drag on the radar minimap |
| **Global Search** | Type in the top search bar to center on any person, town, or faction |
| **Simulation Playback** | Play / Pause (`Space`), Step 1 Month, Step 1 Year via top controls |
| **Speed Adjustment** | Select between `0.5x`, `1.0x`, `2.0x`, `5.0x`, or `10.0x` |
| **Toggle Overlays** | Use the layer control dock to toggle roads, danger zones, and labels |
| **Dismiss Drawer** | Press `Escape` or click the close button |

---

## API & Integration Overview

The backend exposes a standardized REST API along with real-time SSE event streaming:

### Simulation Control
- `GET  /api/simulation/status` — Current state, speed, tick counter, and active era.
- `POST /api/simulation/pause` — Pause simulation execution.
- `POST /api/simulation/resume` — Resume active simulation execution.
- `POST /api/simulation/step/month` — Step forward by 1 calendar month.
- `POST /api/simulation/step/year` — Step forward by 1 calendar year (12 months).
- `POST /api/simulation/speed` — Set playback rate.

### World & Spatial Data
- `GET  /api/world/summary` — Global demographics, total wealth, and era status.
- `GET  /api/world/map` — 2D spatial coordinate snapshot of all visible world entities.
- `GET  /api/world/search?q={query}` — Search settlements, entities, and people.
- `GET  /api/world/overlays` — Heatmaps, territorial boundaries, and tactical paths.

### Entities & Chronicles
- `GET  /api/settlements/{id}` — In-depth profile, stockpiles, and defense metrics.
- `GET  /api/people/{id}` — Individual biography, family lineage, traits, and combat stats.
- `GET  /api/events/chronicle` — Paginated historical event log and milestones.

### Real-Time Streaming
- `GET  /api/events/live` — Server-Sent Events stream for live tick updates and world notifications.

---

## Project Structure

```text
CivSim/
├── README.md                    # Project documentation
├── package.json                 # Root workspace script runner (npm start, npm run dev, npm test)
├── run.bat                      # 1-click Windows root launcher
├── run.ps1                      # 1-click PowerShell root launcher
│
├── backend/                     # Authoritative Java Simulation Engine & Spring Boot Web Service
│   ├── src/                     # Java source code
│   │   └── civsim/              # SimulationEngine, subsystems, models, DTOs, controllers
│   ├── resources/               # Configuration and web distribution assets
│   │   ├── application.properties
│   │   └── static/              # Production web bundle served by backend
│   ├── lib/                     # Spring Boot, Tomcat, and Jackson JAR dependencies
│   ├── bin/                     # Compiled JVM bytecode classes
│   ├── run.bat                  # Backend launch script
│   ├── run.ps1                  # PowerShell backend launch script
│   └── package.json             # Backend build & test scripts
│
├── frontend/                    # Modern React 18 + TypeScript Strategy Game Interface
│   ├── src/                     # React components, living world map, HUD, store, and API client
│   ├── public/                  # Static web assets & Map 1 SVG atlas
│   ├── dist/                    # Production build distribution
│   ├── package.json             # Frontend dependencies & npm scripts
│   ├── vite.config.ts           # Vite proxy & build configuration
│   ├── tailwind.config.js       # Strategy game aesthetic themes
│   └── tsconfig.json            # Strict TypeScript configuration
│
├── saves/                       # Checkpoint saves and alternate timeline branches
└── scratch/                     # Ad-hoc development scripts & experimental prototypes
```

---

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

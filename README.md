# CivSim: Human Life & Civilization Simulator (MIE)

[![Java](https://img.shields.io/badge/Java-21%2B%20%7C%2025%20LTS-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.4-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18.3.1-61DAFB?logo=react&logoColor=black)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.6-3178C6?logo=typescript&logoColor=white)](https://www.typescriptlang.org/)
[![Vite](https://img.shields.io/badge/Vite-6.0-646CFF?logo=vite&logoColor=white)](https://vitejs.dev/)
[![Tailwind CSS](https://img.shields.io/badge/Tailwind-3.4-38B2AC?logo=tailwind-css&logoColor=white)](https://tailwindcss.com/)
[![Tests](https://img.shields.io/badge/Tests-713%20Passed%20(100%25)-brightgreen)]()

**CivSim (Multi-Generational Interactive Ecosystem)** is a deterministic, seed-driven procedural civilization simulator. It combines deep demographic, economic, and geopolitical simulation with a multi-stage supernatural fantasy emergence (*"The Awakening Era"*), fully observable through a responsive, real-time interactive 2D world map.

---

## Table of Contents

- [Key Highlights](#key-highlights)
- [System Architecture](#system-architecture)
- [Core Simulation Engine Features](#core-simulation-engine-features)
- [Interactive 2D World Map](#interactive-2d-world-map)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Option 1: Quick Launch (All-in-One Server)](#option-1-quick-launch-all-in-one-server)
  - [Option 2: React Development Mode (Hot-Reloading)](#option-2-react-development-mode-hot-reloading)
- [Interactive Map Controls & Shortcuts](#interactive-map-controls--shortcuts)
- [REST API & SSE Specifications](#rest-api--sse-specifications)
- [Automated Testing & Verification](#automated-testing--verification)
- [Project Directory Layout](#project-directory-layout)

---

## Key Highlights

- **100% Deterministic & Seed-Driven**: Identical PRNG seeds produce identical population trajectories, battle outcomes, economic fluctuations, and supernatural breakthrough events over centuries.
- **Single Mutation Owner Concurrency Model**: All state mutations are restricted to a single background worker thread (`CivSim-Worker-Thread`). External HTTP and SSE callers access immutable published read models (`WorldTickSnapshot`, `WorldMapSnapshot`) with zero lock contention.
- **Hybrid Canvas + SVG Interactive Map**: High-performance HTML5 Canvas rendering for spatial grids, biome gradients, and monster lair hazard territories; crisp, accessible SVG overlay for interactive settlement citadels, supernatural deposits, and exploration caravans.
- **Real-Time Live Synchronization**: Server-Sent Events (SSE) push stream with an in-memory 100-event ring buffer and `Last-Event-ID` reconnection catch-up replay.
- **Crash-Proof Atomic Persistence**: World states serialize to `.tmp` files and atomically rename to `.json` via file-system transactions with strict path-traversal sanitization.

---

## System Architecture

```text
+----------------------------------------------------------------------------------------------------+
|                                      CIVSIM PLATFORM ARCHITECTURE                                  |
+----------------------------------------------------------------------------------------------------+
|                                                                                                    |
|   [ Browser / Client ]                                                                             |
|      │                                                                                             |
|      ├──► HTTP REST Requests (GET /api/*, POST /api/simulation/*)                                  |
|      └──► EventSource Connection (GET /api/events/stream)                                          |
|               │                                                                                    |
|               ▼                                                                                    |
|   ┌───────────────────────────────────────────┐   ┌────────────────────────────────────────────┐  |
|   │         Spring Boot Web Layer             │   │       EventBroadcaster (SSE Engine)        │  |
|   │  - WorldController, SettlementController  │   │  - 50 Bounded Client Subscribers           │  |
|   │  - SimulationControlController            │   │  - 100-Event Ring Buffer Replay            │  |
|   │  - EventController, PersonController      │   │  - Non-blocking Dispatch Thread Pool       │  |
|   └─────────────────────┬─────────────────────┘   └──────────────────────▲─────────────────────┘  |
|                         │                                                │                        |
|                         ▼ Submit Command / Query                         │ Broadcast Events       |
|   ┌──────────────────────────────────────────────────────────────────────┴─────────────────────┐  |
|   │                          SimulationRunner (Single Writer Owner)                            │  |
|   │  - Dedicated Thread: "CivSim-Worker-Thread"                                                │  |
|   │  - Bounded Control Queue (100) & Bounded Query Queue (200)                                 │  |
|   │  - Copy-on-Publish Snapshots: volatile WorldTickSnapshot & WorldMapSnapshot                │  |
|   │  - Lifecycle FSM: INITIALIZING ⇄ PAUSED ⇄ RUNNING ⇄ STOPPED                                │  |
|   │  - WorldPersistenceService: Atomic .tmp -> .json file transactions                         │  |
|   └─────────────────────────────────────────────┬──────────────────────────────────────────────┘  |
|                                                 │ Authoritative Sequential Ticks                   |
|                                                 ▼                                                  |
|   ┌────────────────────────────────────────────────────────────────────────────────────────────┐  |
|   │                              SimulationEngine Domain Core                                  │  |
|   │  - Timeline & Seeded PRNG            - Demographics & Dynasties                            │  |
|   │  - Production, Stockpiles & Economy  - Warfare, Diplomacy & Combat Engine                  │  |
|   │  - Awakening, Classes & Mastery      - Monster Ecology, Lairs & Tides                      │  |
|   │  - Rare Anomalies & Expeditions      - Clan Heirlooms, Crafting & Blood Feuds              │  |
|   └────────────────────────────────────────────────────────────────────────────────────────────┘  |
+----------------------------------------------------------------------------------------------------+
```

---

## Core Simulation Engine Features

### 1. Mundane Era Foundations (Years 0–49)
- **Biological Demographics**: Lifespan aging curves, marriage pairing, pregnancy probabilities, inheritance rights, family houses, and mortality hazards.
- **Physical Stockpile Economy**: Four baseline physical commodities (`Food`, `Wood`, `Ore`, `Tools`). Starvation models enforce genuine famine risks during severe winters.
- **Multi-Tiered Currency & Banking**: Citizen transactions use `Iron`, `Silver`, `Gold`, and `Platinum`. Settlement banks provide loans, deposits, and civic treasury management.
- **Spatial Biomes & Logistics**: Settlements expand across `RIVER_VALLEY`, `HIGHLAND_MINE`, `FOREST`, and `COASTAL` biomes. Trade caravans physically traverse Euclidean distance vectors.

### 2. The Awakening Era (Year 50+)
- **Supernatural Classes**: 7 distinct paths (`VANGUARD`, `ELEMENTALIST`, `VITALIST`, `WARDEN`, `SEER`, `BEASTBINDER`, `RIFTWALKER`) under `AETHERIC` or `DARK` aspects.
- **10-Tier Cultivation**: Progression from `T1` to `T10`, breakthrough tribulations, mortality deferrals, and mastery ceilings.
- **Persistent Organizations**: Noble Clans, Awakened Orders, Dark Covens, and Mercenary Guilds with formal diplomatic treaties, leadership succession, and organizational influence.

### 3. Ecology, Expeditions & Intergenerational Conflict
- **Monster Ecology & MonsterTides**: Active Monster Lairs seeded at unmanaged supernatural sites, generational species mutations, Alpha/Ancient creatures, and multi-wave settlement invasion tides.
- **Aether Deposits & Precognition**: Seer class precognition detects forming resource anomalies; settlements organize physical expeditions to secure and extract rare supernatural minerals under strict 4-bucket conservation laws.
- **Clan Heirlooms & Equipment**: Monster core forging, runic enchanting, equipment wear, and ancestral heirlooms tracked across generations.
- **Multi-Generational Blood Feuds**: Historical grievances survive individual deaths, leading to clan revenge raids, diplomatic reparations, or generational reconciliation.

---

## Interactive 2D World Map

The frontend provides a strategy-game-inspired command dashboard built with React 18, TypeScript, Tailwind CSS, and Zustand:

- **Mathematical Aspect Ratio Preservation**: A uniform coordinate scale ensures a square $(120 \times 120)$ world coordinate grid without horizontal or vertical stretching.
- **Cursor-Centered Zoom**: Smooth mouse wheel zooming anchored to the mouse focal point (`0.5x` to `8.0x`).
- **Settlement Citadels**: Sized dynamically via a bounded square-root scaling function ($10\text{px} \le r \le 30\text{px}$) to keep hamlets clickable and megacities proportional.
- **Monster Lair Territory Hazard Rings**: Canvas renders exact territory circles (e.g. 36.0 world units scaled to pixels) with danger gradients.
- **Deep Inspection Drawer**: Inspect any settlement's stockpiles, demographic distribution, tech level, civic treasury, crafting facilities, and recent historical chronicles.
- **World Search Bar**: Real-time bounded search across settlements, monster lairs, anomalies, organizations, and living persons with instant camera centering.
- **Era-Based Aesthetic Switching**: The interface dynamically adapts its visual identity from warm parchment and bronze in the *Mundane Era* to tactical slate, aetheric cyan, and purple in the *Awakening Era*.

---

## Getting Started

### Prerequisites

- **Java Development Kit (JDK)**: JDK 21+ or JDK 25 LTS installed.
- **Node.js**: Node 18+ and npm installed (for frontend building and dev server).

---

### Option 1: Quick Launch (All-in-One Server)

The compiled frontend is pre-packaged inside the Java application. Running the single Java process serves both the web frontend and the simulation engine:

#### On Windows (PowerShell / Command Prompt):
```powershell
cd src
.\run.bat
```
*(Alternatively, in PowerShell you can run `npm start` or `./run.ps1`)*

#### Manual Java Command:
```powershell
cd src
java -cp "bin;lib/*" civsim.web.CivSimWebApplication
```

Open your browser and navigate to:
👉 **`http://localhost:8081/`**

---

### Option 2: React Development Mode (Hot-Reloading)

If you are modifying the React/TypeScript frontend code with live hot-reloading:

#### Terminal 1 — Start the Backend Engine:
```powershell
cd src
java -cp "bin;lib/*" civsim.web.CivSimWebApplication
```

#### Terminal 2 — Start the Vite Dev Server:
```powershell
cd src/web
npm run dev
```

Open your browser at:
👉 **`http://localhost:5173/`**  
*(Vite automatically proxies all `/api/*` and SSE streams to backend port `8081`).*

---

## Interactive Map Controls & Shortcuts

| Action | Control |
| :--- | :--- |
| **Pan / Drag** | Click and drag on map canvas |
| **Zoom In / Out** | Mouse wheel (centered at cursor), `+` / `-` keys, or HUD buttons |
| **Fit Whole World** | Press `0` or click the Maximize icon |
| **Reset View** | Click the Reset View icon (counter-clockwise arrow) |
| **Inspect Entity** | Click any Settlement, Monster Lair, Anomaly, or Expedition marker |
| **Minimap Navigation** | Click anywhere on the top-right radar minimap to pan the camera |
| **World Search** | Type in the search box (top-left) to find entities and jump to them |
| **Playback Controls** | Play / Pause, Step 1 Month, Step 1 Year (12 months) via top bar |
| **Simulation Speed** | Select speed: `0.5x`, `1.0x`, `2.0x`, `5.0x`, `10.0x` |
| **Layer & Filter Toggles** | Click the Layers or Filters buttons in the top header |
| **Close Drawers** | Press `Escape` or click the close button `(X)` |

---

## REST API & SSE Specifications

All endpoints run on `http://localhost:8081` and return JSON:

### World State & Map Endpoints
- `GET /api/world/summary` — High-level world demographics, stockpiles, and era status.
- `GET /api/world/map` — Complete 2D spatial coordinate snapshot of all visible entities.
- `GET /api/world/search?q={query}&limit=15` — Bounded search across world entities.
- `GET /api/world/snapshot` — Volatile `WorldTickSnapshot` read model.

### Simulation Playback Controls
- `GET /api/simulation/status` — Lifecycle FSM state, speed, tick counter, queue depths.
- `POST /api/simulation/pause` — Asynchronously pause simulation ticking (`202 Accepted`).
- `POST /api/simulation/resume` — Asynchronously resume simulation ticking (`202 Accepted`).
- `POST /api/simulation/step/month` — Step forward by exactly 1 calendar month.
- `POST /api/simulation/step/year` — Step forward by 12 consecutive calendar months.
- `POST /api/simulation/speed` — Set execution speed (`SLOW`, `NORMAL`, `FAST`, `VERY_FAST`, `MAXIMUM`).

### Entity Inspection Endpoints
- `GET /api/settlements/{id}` — Full settlement profile (stockpiles, tech, leader, defense).
- `GET /api/events?type={name}&size=20` — Paginated historical event chronicles.
- `GET /api/events/stream` — Live Server-Sent Events stream (`tick-completed`, `world-snapshot-updated`, `resync-required`).
- `POST /api/world/save` & `POST /api/world/load/{id}` — Atomic world state persistence.

---

## Automated Testing & Verification

CivSim maintains a comprehensive regression test suite ensuring determinism, memory boundaries, and mathematical correctness:

| Test Suite | Class / Script | Tests | Description |
| :--- | :--- | :--- | :--- |
| **Interactive Map** | `civsim.test.TestStage32InteractiveMap` | **59 / 59 PASS** | Snapshot contracts, coordinate bounds, bounded marker sizing |
| **Backend Integration** | `civsim.test.TestStage31Backend` | **122 / 122 PASS** | FSM lifecycle, bounded queues, SSE replay, atomic save/load |
| **Simulation Integrity Audit** | `civsim.test.TestSimulationIntegrityAudit` | **99 / 99 PASS** | Famine corrections, zero memory leaks, population caps |
| **Multi-Gen Blood Feuds** | `civsim.test.TestStage2C2BloodFeuds` | **84 / 84 PASS** | 250-year grievance inheritance, revenge, and reconciliations |
| **Heirlooms & Crafting** | `civsim.test.TestStage2C1EquipmentAndHeirlooms` | **120 / 120 PASS** | Monster core crafting, runic enchanting, heirloom recovery |
| **Monster Ecology & Lairs**| `civsim.test.TestStage2B5MonsterEcology` | **95 / 95 PASS** | MonsterTide invasions, species evolution, alpha caps |
| **Anomaly Skirmishes** | `civsim.test.TestStage2B4AnomalySkirmishes` | **51 / 51 PASS** | Coalition negotiations, skirmishes, 4-bucket conservation |
| **Spatial Exploration** | `civsim.test.TestStage2B3AnomalyExploration` | **78 / 78 PASS** | Seer precognition, expedition logistics, deposit lifecycle |
| **Spatial Coordinate Math** | `src/web/src/test/frontend-verification.cjs` | **25 / 25 PASS** | Bijective coordinates, aspect ratio, cursor zoom invariance |

### Running the Test Suites:

```powershell
# Run the Interactive Map Test Suite
java -cp "src/bin;src/lib/*" civsim.test.TestStage32InteractiveMap

# Run the Full Backend & Persistence Test Suite
java -cp "src/bin;src/lib/*" civsim.test.TestStage31Backend

# Run the Coordinate Transformation Math Tests
node src/web/src/test/frontend-verification.cjs
```

---

## Project Directory Layout

```text
CivSim/
├── .gitignore                   # Ignore rules for binaries, node_modules, and saves
├── README.md                    # Project documentation
└── src/
    ├── civsim/                  # Single Authoritative Java Simulation Engine
    │   ├── SimulationEngine.java# Central simulation state & monthly ticking pipeline
    │   ├── dto/                 # Immutable snapshot records and API DTOs
    │   ├── model/               # Domain entities (Person, Village, Monster, Anomaly)
    │   ├── persistence/         # Atomic .tmp to .json persistence service
    │   ├── runner/              # Single-writer execution coordinator & snapshot publisher
    │   ├── systems/             # Subsystems (Demographics, Warfare, Ecology, Progression)
    │   ├── test/                # Comprehensive regression and acceptance test suites
    │   └── web/                 # Spring Boot controllers, WebMvc config, SSE engine
    ├── web/                     # React + TypeScript Interactive World Map Frontend
    │   ├── src/
    │   │   ├── components/      # MapCanvas, SvgOverlay, InspectionDrawer, TopBar
    │   │   ├── hooks/           # useSimulationSSE live stream subscriber
    │   │   ├── services/        # REST API fetch client
    │   │   ├── store/           # Zustand UI state (layers, filters, selection)
    │   │   ├── utils/           # Bijective coordinate transformations & zoom math
    │   │   └── types/           # Strongly typed API DTO TypeScript definitions
    │   ├── package.json         # Frontend dependencies and build scripts
    │   └── vite.config.ts       # Vite proxy configuration (port 8081)
    ├── lib/                     # Spring Boot, Tomcat, and Jackson JAR dependencies
    ├── bin/                     # Compiled JVM class files & packaged static web assets
    ├── package.json             # Root npm scripts (npm start, npm run dev)
    ├── run.bat                  # 1-click Windows launch script (auto-opens browser)
    └── run.ps1                  # PowerShell launch script
```

---

## License

This project is licensed under the MIT License - see the LICENSE file for details.


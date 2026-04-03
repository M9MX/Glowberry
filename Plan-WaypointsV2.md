# Plan-WaypointsV2 — Specification & Implementation Guide (Glowberry / Cactus addon)

## Overview & Goals

**TL;DR:** Implement a WaypointsV2 **module** for Glowberry (Fabric, Java) to replace or augment existing waypoints with a robust UX for creating, editing, grouping, and rendering waypoints (location and block-based). This module integrates with Glowberry's module settings system to manage all configuration, keybinds, and options. Goals include reliable YAML storage for waypoint data with migrations and backups, performant in-world rendering with grouping and LOD, accessible controls and localization-ready labels, and an API for other modules/plugins to integrate.

**Architecture:** WaypointsV2 is a **Glowberry module**. All module settings (render distances, grouping radius, show-through-walls, auto-backup behavior, etc.) are registered and managed through Glowberry's module settings system. Keybinds are registered as module keybinds and saved with the module settings. Waypoint data (locations, blocks, categories) are persisted to YAML files in the module's data directory, managed internally by the module storage layer.

**Non-goals:** Rewriting Glowberry core; changing world/dimension semantics; introducing new runtime dependencies unless small (e.g., existing Fabric/Glowberry utility libs).

---

## Implementation Milestones & Checklist

### Milestone 0 — Project Scaffolding
- [ ] Add module skeleton: `src/main/java/com/glowberry/addons/waypointsv2/` (package)
- [ ] Add config files placeholder: `config/waypointsv2/` in run resources; add default YAML examples

### Milestone 1 — Data Model & Storage (Core)
- [ ] Implement immutable data classes: `Waypoint`, `BlockWaypoint`, `Category`
- [ ] Implement `WaypointStorage` with load/save, atomic file writes, versioning metadata, and backup strategy
- [ ] Add YAML schemas & example files under resources

### Milestone 2 — Settings & Keybinds
- [ ] Implement settings registration (`WaypointsSettings`) with defaults (render distances, grouping radius, show-through-walls)
- [ ] Implement keybinds: Add waypoint, Open waypoints screen

### Milestone 3 — UI: Add/Edit Modal + Waypoints Screen + Block-list Editor
- [ ] Implement Add Waypoint dialog with fields and validation
- [ ] Implement Waypoints screen with categories, expand/collapse, sorting and per-waypoint actions
- [ ] Implement Block-list editor screen for block-type waypoints (edit-only)
- [ ] Implement item search overlay (100 result cap) used by icon selector

### Milestone 4 — Rendering & Performance
- [ ] Implement `WaypointRenderer` with grouping, LOD rules, culling, batching, and dimension checks
- [ ] Implement combined-block rendering overlay for block-type waypoints

### Milestone 5 — API & Commands
- [ ] Expose `WaypointsManager` API for other modules: registerHook, getWaypoints, add/remove
- [ ] Optional CLI/commands for adding/listing/removing/tp

### Milestone 6 — Tests & QA
- [ ] Unit tests for storage and schema migration
- [ ] Integration tests for UI flows
- [ ] Manual performance QA in populated worlds

### Release Milestone
- [ ] Document migration steps and release notes
- [ ] Implement analytics/logging for migration failures and save errors

---

## UI / UX Design & Flows

### High-Level UX Goals
- Minimal friction to add/edit waypoints
- Clear, atomic differences between "create" and "edit" modes (some fields locked in edit)
- Efficient search when picking item icons; limit results to 100 to avoid UI lag
- Accessible: keyboard navigable, localization-ready labels

### 1. Add Waypoint Dialog (Modal)

**Purpose:** Create or edit a single waypoint

**Mode differences:**
- **Create:** All fields editable
- **Edit:** Block-list locked (if type is Blocks); dimension optionally editable by config

**Fields (layout left-to-right / stacked):**
- **Name** (text input) — Required
- **X, Y, Z** (three numeric fields) — Support integer only by default; provide "Use current coords" button
- **Dimension selector** (dropdown) — Defaults to player's current dimension when opening "Add"
- **Color picker:**
  - Visual color swatch
  - RGB numeric fields (R, G, B, 0–255)
  - Alpha slider (0–255 or 0–100%)
  - Preset palette grid (8–16 colors) for quick pick
- **Icon type selector** (radio or segmented control): `Text` / `Item` / `Text-Item`
  - If `Text` selected: show text input for overlay label
  - If `Item` selected: show small item slot + search button to open item search overlay (limit 100 results)
  - If `Text-Item`: show both; allow item selection and text
- **Category selector** (dropdown) with "Add new category" entry
  - If "Add new category" chosen, show inline small input to name and choose color/icon
- **Waypoint type selector**: `Location` vs `Blocks`
  - If `Location`: standard coordinate waypoint
  - If `Blocks`: open/link to block-list screen (edit-only; creation requires edit to add blocks)
- **Advanced options** (collapsible):
  - Teleport-safe toggle
  - Visibility toggles (show on HUD/minimap)
  - Custom LOD override
- **Buttons:**
  - `Save` (primary) — performs validation
  - `Cancel`
  - `Delete` (only in edit mode)

**Validation & error messages:**
- **Name:** Required. Error: "Name is required." / Duplicate in same category: warning "A waypoint with this name already exists in this category. Continue?"
- **Coordinates:** Must be valid integers within world bounds. Error: "Invalid coordinate: Y must be within 0..(worldMaxHeight−1)."
- **Item selection:** If `Item` or `Text-Item` and no item selected: warning "No item selected."
- **Block-type:** Creating a `Blocks` waypoint: if user tries to save without any block coordinates, error: "Block coordinates list cannot be empty for blocks-type waypoints."

**ASCII Wireframe:**
```
+-------------------------------------------+
| Add Waypoint                              |
| Name: [______________]                    |
| X: [  ] Y: [  ] Z: [  ] (Use current)     |
| Dimension: [Overworld v]                  |
| Color: [■] R:[__] G:[__] B:[__] A:[__]    |
| Presets: [■][■][■][■][■][■][■][■]        |
| Icon type: (o) Text  ( ) Item  ( ) T-Item |
| [If Text] Label: [_____________]          |
| [If Item] Icon: [slot] [Search]           |
| Category: [Travel v] [+ Add new]          |
| Waypoint type: (o) Location  ( ) Blocks   |
| [Advanced v]                              |
| [Cancel]              [Save]              |
+-------------------------------------------+
```

### 2. Item Search Overlay (Used by Icon Selector)

**Layout:**
- Search bar at top; result area below with grid of item icons + names; pagination/scroll
- Maximum results = 100. If search returns >100 results, show "Showing first 100 results — refine search."
- Fallback: if search empty, show default first 100 items loaded from game registry

**Performance:**
- Debounce search input (150–300ms)
- Asynchronous query that cancels previous requests
- Limit on concurrency to avoid blocking

**Interaction:**
- Click item selects and closes overlay
- Keyboard nav + enter to pick

### 3. Block-list Editor (Edit-Only)

**Purpose:** Maintain the list of specific block coordinates that a `Blocks` waypoint will highlight. This screen is **only reachable when editing** an existing blocks-type waypoint.

**Fields/controls:**
- Add block coordinate (enter X, Y, Z values or use "Target block" button to click on a block in-world to capture its coordinates)
- Remove selected
- List of all block coordinates with their XYZ values displayed
- Reorder by drag-drop (for rendering priority if desired)

**Validation:**
- Cannot save empty block coordinate list. Error: "At least one block coordinate must be specified."
- Coordinates must be valid integers within world bounds

### 4. Waypoints Screen (Main List UI)

**Layout:**
- Left: categories pane / filter
- Right: waypoints list grouped by category (accordion-style expand/collapse), showing per-waypoint row

**Per-category:**
- Header shows category name, color swatch, toggle to hide/show category, expand/collapse button

**Per-waypoint row:**
- Icon (item or text glyph), Name, Distance to player (live), Controls: Teleport, Edit, Remove (icons), Visibility toggle
- Sort options at top: Distance, Name, DateAdded, Custom order
- Distance display:
  - Show in blocks (integer), formatted (e.g., "345 m")
  - Optionally show "—" if in different dimension or show dimension label
  - Clicking distance sorts by ascending distance

**Actions:**
- **Teleport:** Triggers safe teleport if enabled; if unavailable, show confirmation "Teleport requires server/mod support. Run command?"
- **Edit:** Opens edit modal
- **Remove:** Confirmation dialog "Delete waypoint 'Name'?" with optional "Backup to file" checkbox

**ASCII Wireframe:**
```
+-------------------------------------------------------------+
| [Search] [Sort: Distance v] [Show Hidden] [Settings]        |
| Categories: [All] [Travel] [Home] [Mining]                  |
| ----------------------------------------------------------  |
| Travel (blue)  [v]   Hide                                    |
|   - Waypoint A   [icon] Name (123 m)  [TP][Edit][Remove]    |
|   - Waypoint B   [icon] Name (234 m)  [TP][Edit][Remove]    |
| Mining (green) [>]                                           |
+-------------------------------------------------------------+
```

### 5. Keybinds

**Default keybinds (configurable):**
- **Add waypoint:** `Keybind.ADD_WAYPOINT` (suggest default: Ctrl+K)
- **Open waypoints screen:** `Keybind.OPEN_WAYPOINTS` (suggest default: B)

**Behavior:**
- Add waypoint keybind opens Add dialog using current player position and selected dimension
- Open waypoints screen: opens main list UI

### 6. Settings (Module Settings via Glowberry's Module Settings System)

All settings are registered as **module settings** through Glowberry's settings API and persisted via the module's settings save mechanism (alongside keybinds).

**Settings categories:**

**Global toggles:**
- Module enabled (default: true)
- Show waypoints in spectator mode (default: false)
- Show-through-walls (toggle) (default: false)
- Render only when HUD enabled (default: false)

**Distances & LOD:**
- Label distance LOD thresholds:
  - Near threshold: 0..50 blocks (configurable)
  - Mid threshold: 51..100 blocks (configurable)
  - Far threshold: 101+ blocks (configurable)
- Max render distance (global cap) (default: 256 blocks)

**Grouping & Clustering:**
- Group by category: radius in blocks (default: 10)
- Individual category override allowed (per-category setting)

**Waypoint Data Storage:**
- Storage directory (relative to module data dir) — auto-managed by module
- Auto-backup on save (default: ON)
- Retain N backup copies (default: 5)
- Atomic save mode (default: ON)

**Keybinds (Module keybinds):**
- `waypointsv2.keybind.add_waypoint` (suggest default: Ctrl+K)
- `waypointsv2.keybind.open_waypoints` (suggest default: B)

**Localization:**
- Locale preference (inherits game locale by default)

---

## Data & Storage: YAML Schemas, Examples, Atomic Save Strategy

### Architecture: Module Data Files vs Module Settings

**Module Settings** (managed by Glowberry's module settings system):
- Render distance thresholds (NEAR, MID, FAR)
- Grouping radius
- Show-through-walls toggle
- Show in spectator mode toggle
- Render only when HUD enabled
- Max render distance cap
- Auto-backup retain count
- Atomic save mode toggle
- Per-category visibility overrides
- Keybinds (Add waypoint, Open waypoints screen)

**Module Data Files** (managed by WaypointStorage, stored in module's data directory):
- `location-waypoints.yml` — individual location waypoint records
- `blocks-waypoints.yml` — individual block-based waypoint records
- `categories.yml` — category metadata and organization
- `backups/` — timestamped backup copies for crash recovery

### Data Files (Default Paths)

Files are stored in the module's data directory (e.g., `.glowberry/modules/waypointsv2/data/` or similar):

```
waypointsv2/data/
  location-waypoints.yml  — location waypoints
  blocks-waypoints.yml    — block-list waypoints
  categories.yml          — categories metadata
  backups/
    location-waypoints.yml.bak.YYYYMMDDhhmmss
    blocks-waypoints.yml.bak.YYYYMMDDhhmmss
    categories.yml.bak.YYYYMMDDhhmmss
```

### General Storage Rules

- Use YAML mapping (top-level) with `meta:` key for schema `version` and `generatedAt`
- **Atomic save:** Write to temporary file `*.yml.tmp` then move/rename to final path (filesystem atomic rename). On Windows, replace existing file via atomic rename fallback (write to temp then replace)
- **Backups:** When saving, copy previous file to `*.yml.bak.YYYYMMDDhhmmss` (configurable keep count, default N=5)
- **Load order:** Load `categories.yml` first, then `location-waypoints.yml`, then `blocks-waypoints.yml`. This ensures category references resolve
- **Validation during load:** Check `meta.version` and run migration scripts if needed

### YAML Schema: `location-waypoints.yml`

**Top-level structure:** Mapping named `waypoints` (list) + `meta`

**Each waypoint object fields:**
- **id:** string (UUID recommended) — internal unique id
- **name:** string
- **categoryId:** string (reference to categories.yml)
- **type:** "location"
- **dimension:** string ("overworld", "the_nether", "the_end", or mod dimension id)
- **x, y, z:** integer coordinates
- **color:** `{ r: int, g: int, b: int, a?: int }` (a optional, default 255)
- **icon:** `{ kind: "text"|"item"|"text-item", text?: string, itemId?: string }`
- **createdAt, updatedAt:** ISO8601 timestamps
- **meta:** `{ version: int, source?: string }`

**Example:**
```yaml
meta:
  version: 1
  generatedAt: "2026-03-24T12:00:00Z"
waypoints:
  - id: "b3f9f2c6-1a2b-4d2a-8c8a-1234567890ab"
    name: "Home"
    categoryId: "travel_home"
    type: "location"
    dimension: "overworld"
    x: 123
    y: 64
    z: -45
    color: { r: 255, g: 200, b: 0, a: 230 }
    icon:
      kind: "item"
      itemId: "minecraft:compass"
    createdAt: "2026-03-10T09:00:00Z"
    updatedAt: "2026-03-10T09:00:00Z"
```

### YAML Schema: `blocks-waypoints.yml`

**Top-level:** `meta` + `waypoints` list

**Each block-waypoint fields:**
- **id:** string (UUID)
- **name:** string
- **categoryId:** string
- **type:** "blocks"
- **dimension:** string (required; blocks are dimension-specific)
- **blockCoordinates:** list of block coordinate entries:
  - **x:** integer
  - **y:** integer
  - **z:** integer
- **color, icon, createdAt, updatedAt, meta:** as per location-waypoints

**Example:**
```yaml
meta:
  version: 1
  generatedAt: "2026-03-24T12:00:00Z"
waypoints:
  - id: "a1a2a3b4-c5d6-4e7f-8a9b-abcdef012345"
    name: "CactusPatch"
    categoryId: "resources"
    type: "blocks"
    dimension: "overworld"
    blockCoordinates:
      - { x: 100, y: 64, z: 200 }
      - { x: 101, y: 64, z: 200 }
      - { x: 102, y: 64, z: 200 }
      - { x: 103, y: 64, z: 200 }
    color: { r: 0, g: 255, b: 0, a: 200 }
    icon:
      kind: "text"
      text: "CACT"
    createdAt: "2026-03-10T10:00:00Z"
```

### YAML Schema: `categories.yml`

**Top-level:** `meta` + `categories` list

**Each category fields:**
- **id:** string (unique)
- **name:** string (localizable key allowed)
- **color?:** `{ r, g, b, a }` (optional)
- **icon?:** `{ itemId or text }` (optional)
- **sortOrder?:** integer (default 0)
- **hidden?:** boolean (default false)
- **createdAt, updatedAt:** ISO8601 timestamps

**Example:**
```yaml
meta:
  version: 1
  generatedAt: "2026-03-24T12:00:00Z"
categories:
  - id: "travel_home"
    name: "Home"
    color: { r: 0, g: 128, b: 255, a: 255 }
    icon:
      kind: "item"
      itemId: "minecraft:bed"
    sortOrder: 10
    hidden: false
  - id: "resources"
    name: "Resources"
    color: { r: 0, g: 255, b: 0, a: 255 }
    sortOrder: 20
    hidden: false
```

### Versioning & Migration Plan

- Each YAML file must include `meta.version`. Versioning semantics:
  - **version 1:** initial schema as above
  - **future changes:** storage must implement MigrationStack that can transform older version to current version on load
- **Migration procedure:**
  1. Detect `meta.version < CURRENT_VERSION` (hard-coded constant)
  2. Load raw YAML into intermediate structures (preserve unknown fields)
  3. Apply sequential migration steps (v1→v2→v3...) with unit tests
  4. Validate result; if success: write migrated file as backup and new file with `meta.version = CURRENT_VERSION`
  5. If migration fails: log error, store original file under `*.migration-failed.YYYYMMDDHHmmss` and skip loading that file (or load as read-only)
- Keep migration code small and testable; store migration functions in `WaypointStorageMigrations`

### Atomic Save Strategy (Detailed)

**Save transaction:**
1. Serialize YAML to string
2. Write to temporary file `location-waypoints.yml.tmp` using safe write (fsync if available)
3. Rename `location-waypoints.yml.tmp` → `location-waypoints.yml` (atomic rename on POSIX; on Windows, delete then rename or use platform-safe replace function)
4. Rotate backups: keep latest `N` backups configured

**On crash recovery:**
- If `.tmp` exists at startup, attempt to rename to final or remove if invalid; log and alert user

### Load Order
1. `categories.yml` — load categories into `WaypointsManager` (categories registry)
2. `location-waypoints.yml` — load location waypoints and associate categories; for missing categories create temporary "Uncategorized"
3. `blocks-waypoints.yml` — load block-based waypoints
4. Run validation pass linking icons (item IDs) to registry (missing items flagged but not fatal)

---

## Rendering Rules & Performance Considerations

### Goals
- Render waypoints in-world without significant FPS drop
- Group waypoints by category within configurable radius to reduce clutter
- Respect dimension visibility

### Key Behaviors

#### 1. Show/Hide by Dimension
- Each waypoint carries `dimension` field
- Renderer must filter to player's current dimension by default or show icon with "dim label" if configured to show cross-dimension

#### 2. Grouping by Category (Configurable)
- **Group radius** (global default 10 blocks)
- When multiple waypoints from same category are within radius, render a grouped marker with count and combined icon/text
- **Grouping algorithm:**
  - For each category, cluster waypoints using simple grid-based spatial hashing or agglomerative clustering with radius threshold
  - Represent group center as average of member positions (vector average)
  - For clusters >1, render a group icon showing a count badge

#### 3. Combined-Block Rendering Overlay
- For `Blocks` waypoints, render small translucent overlay on the blocks at the stored coordinates
- The overlay is a colored tint/glow effect on top of the block
- Can be toggled to show through walls or not (configurable per user setting)
- When player is within a configurable trigger distance, blocks are highlighted; when farther away, overlays may fade or hide based on LOD settings
- Use a combined-block mask to batch overlays for blocks in same chunk to re-use vertex buffers

#### 4. LOD Rules
- Define thresholds: NEAR ≤ 50 blocks, MID 51–100, FAR > 100 (defaults, configurable)
- **Text/labels:**
  - NEAR: show full label, icon, and distance
  - MID: show icon + distance only
  - FAR: show only small icon or dot; optional hide labels
- **Icon scaling:**
  - Scale inversely to distance: `scale = clamp(1.0 - (distance / maxRenderDistance), minScale, 1.0)`
- **Distance thresholds** must be configurable by the user

#### 5. Distance Display & Sorting
- Distance computed in Euclidean horizontal/3D as configured; default show Euclidean block distance

#### 6. Color Handling
- Colors stored as RGBA ints
- Renderer must convert to linear color space if necessary and respect alpha blending
- Apply alpha clamp [0, 255]

#### 7. Performance Considerations
- **Frustum culling:** Do not prepare render data for waypoints outside camera frustum
- **Chunk-based batching:** Group render calls per chunk and per texture atlas to reduce draw calls
- **Limit max active on-screen waypoints:** If > N (configurable, default 200), hide least-recently-used or faraway markers
- **Update frequency:** Compute clustering & sorting at low frequency (e.g., 5x per second) and only update spatial data structures when player moves > threshold or world changes
- **Efficient data structures:** KD-tree or spatial hash grid for distance queries
- **Async precomputation:** For combined-block overlays, compute masks off main render thread if possible (respect Fabric rules)
- **GPU resource reuse:** Reuse vertex buffers across frames

### Rendering Pseudo-Flow
**Each frame:**
1. If player moved > threshold or tick mod updateInterval == 0 → recompute visible set and clusters
2. For visible clusters / waypoints:
   - For each entity to render, apply LOD and transform to screen-space
   - Batch icons (same texture atlas) into single draw call
   - Render label fonts separately with text batching
3. For combined-block overlays:
   - Use chunk-based overlay buffers; only update buffers when block sets change

---

## Editing & Lifecycle

### Create vs Edit Modal Differences

**Create (Add):**
- All primary fields editable
- Block lists for `Blocks` type cannot be added inline; create will create an empty blocks waypoint and open edit screen to add blocks (or present a "Create & Edit" flow)

**Edit:**
- All fields editable except block-list editing which is exclusively performed in Block-list editor screen
- Show `Delete` action
- Track `updatedAt` field on save

### Block-List Edit-Only Restriction Rationale
Block-type waypoints represent potentially large lists of block coordinates and require a more advanced editing UI (add/remove coordinates, click-to-target blocks). To avoid accidental misconfiguration from quick modal, restrict coordinate editing to dedicated editor.

### Validation Rules
- On save:
  - Validate coordinate ranges and block-coordinate list not empty for block-type waypoints
  - Validate category existence (create "Uncategorized" if missing)
  - If validation fails, present inline errors and prevent save

### Undo/Redo & Backups

**Simple undo/redo:**
- Implement an in-memory command stack for current session for add/edit/remove with depth limit (e.g., 20)
- Expose `undo`/`redo` actions in the Waypoints screen (Ctrl+Z/Ctrl+Y)

**Persistent backups:**
- Each save rotates a timestamped backup. Keep last N backups (default N=5)
- Provide "Export/Import" for manual backup restore

**Crash recovery:**
- On load, if a YAML file is corrupted, attempt to load the most recent backup

### Lifecycle Events
Events that modules can listen to:
- `WaypointAdded(Waypoint)`
- `WaypointUpdated(Waypoint)`
- `WaypointRemoved(waypointId)`
- `CategoryAdded/Updated/Removed`

Emit events after successful save.

---

## API / Design for Module Integration

### Suggested Java Packages & Classes

```
com.glowberry.addons.waypointsv2/
  ├─ WaypointsManager (singleton/registry)
  ├─ Waypoint (interface / base class)
  ├─ LocationWaypoint (extends Waypoint)
  ├─ BlocksWaypoint (extends Waypoint)
  ├─ WaypointCategory
  ├─ WaypointStorage (load/save/migrate)
  ├─ WaypointRenderer (rendering logic)
  ├─ WaypointsSettings (config holder)
  ├─ WaypointEvent (events hub)
  └─ WaypointCommands (optional command handlers)
```

### Class Suggestions & Key Members

#### 1. Waypoint (POJO/Immutable Pattern)
**Fields:**
- `UUID id`
- `String name`
- `String categoryId`
- `String type` // "location" or "blocks"
- `String dimension`
- `Color color` (RGBA)
- `Icon icon`
- `Instant createdAt, updatedAt`

**Methods:**
- `String getSummary()`
- `double getDistanceTo(Player)`
- `Map<String, Object> toYamlMap()`

#### 2. LocationWaypoint (extends Waypoint)
**Fields:**
- `int x, y, z`

**Methods:**
- `Vec3d getPositionVec()`

#### 3. BlocksWaypoint (extends Waypoint)
**Fields:**
- `List<BlockCoordinate> blockCoordinates` // x, y, z coordinate pairs

**Methods:**
- `List<BlockCoordinate> getBlockCoordinates()`
- `void addBlockCoordinate(int x, int y, int z)`
- `void removeBlockCoordinate(int x, int y, int z)`

#### 4. WaypointsManager
**Methods:**
- `List<Waypoint> getAll()`
- `List<Waypoint> getByCategory(String categoryId)`
- `Waypoint getById(UUID id)`
- `void addWaypoint(Waypoint wp)`
- `void updateWaypoint(Waypoint wp)`
- `void removeWaypoint(UUID id)`
- `void save()` // delegate to storage
- `void load()`
- `void registerListener(WaypointListener l)`
- `void unregisterListener(WaypointListener l)`

#### 5. WaypointStorage
**Methods:**
- `void saveAll(Path storageDir) throws IOException`
- `void saveLocations(Path file) throws IOException`
- `void saveBlocks(Path file) throws IOException`
- `LoadResult loadAll(Path storageDir) throws IOException`
- `MigrationResult migrateIfNeeded(Path file)`

#### 6. WaypointRenderer
**Methods:**
- `void tickRender(Player player, MatrixStack stack, Camera camera, float partialTicks)`
- `void rebuildClusters()`
- `void setSettings(WaypointsSettings s)`

### Example Method Signatures & Pseudocode

**Saving with atomic write:**
```java
void atomicSave(Path finalPath, String serializedYaml) throws IOException {
    Path tmp = finalPath.resolveSibling(finalPath.getFileName().toString() + ".tmp");
    Files.writeString(tmp, serializedYaml, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    // attempt atomic replace
    try {
        Files.move(tmp, finalPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    } catch (AtomicMoveNotSupportedException ex) {
        Files.deleteIfExists(finalPath);
        Files.move(tmp, finalPath, StandardCopyOption.REPLACE_EXISTING);
    }
}
```

**Load with migration check:**
```java
LoadResult loadLocations(Path file) {
    YamlNode root = Yaml.load(file);
    int version = root.meta.version.orElse(1);
    if (version < CURRENT_VERSION) {
        root = MigrationRunner.migrate(root, version, CURRENT_VERSION);
        // write migrated file as new
        atomicSave(file, Yaml.dump(root));
    }
    // parse waypoints
    return parseWaypoints(root);
}
```

**WaypointsManager.addWaypoint:**
```java
public void addWaypoint(Waypoint wp) {
    waypoints.put(wp.getId(), wp);
    fireEvent(new WaypointAddedEvent(wp));
    storage.saveAll(storageDir); // consider batching saves
}
```

### Event Hooks
Create a small event bus or use Glowberry event system:
- `WaypointsManager.fireEvent(new WaypointAddedEvent(wp))`
- Listeners may be registered to perform extra behavior (e.g., log, UI updates)

### Commands (Optional)
- `/waypoint add <name>` (create at current pos)
- `/waypoint tp <id|name>` (teleport if permitted)
- `/waypoint list [category]`
- `/waypoint remove <id|name>`

---

## Item Search Behavior (Detailed)

### Requirements
- Provide fast, responsive search for items when selecting an icon
- Cap results to first 100 results to limit memory and UI rendering cost
- Fallback behavior: when search input empty, show default first 100 registry items sorted by popularity or item id

### UI Behavior
- Search bar with debounce (150–300ms)
- Results shown as grid of item icons with names; show count and "reached limit" notice
- Provide keyboard navigation and Enter to select

### Implementation Notes
- Query over in-game item registry (Fabric API registry lookup)
- For performance, maintain a cached index of item names and tags, updated on resource reload events
- Searching should be case-insensitive and support partial matches
- If search returns >100 results, truncate and show message "Displaying first 100 results. Narrow your search." Optionally allow user to filter by namespace (minecraft/modid)

### Edge Cases
- Items that are not available client-side (mod removed) should be filtered
- If an item id cannot be resolved, show it with a warning icon and do not allow selection

---

## Accessibility & Edge Cases

### Duplicate Names
- Allow duplicate names but warn on save in same category (prevent accidental confusion)
- Internal identification done by UUID

### Overlapping Waypoints
- When two or more waypoints are in same position:
  - If same category and within grouping radius, cluster them
  - If user selects marker, show a small popup list to choose between them (index by name/distance/time)
  - Provide "cycle" hotkey to iterate overlapping markers

### Invalid Coordinates
- Validate and clamp values. Y must be between 0..worldHeight-1 (reference world settings)
- If invalid, present clear message
- For coordinates outside loaded dimensions, still allow saving but flag as "out-of-range"

### Missing Categories
- When a waypoint references a missing category on load, create a temporary "Uncategorized" category and log warning

### Mod Interactions
- Item/block IDs may change if mods are removed
- On load, unresolved items/blocks should be noted:
  - Add `icon.missingItem: true` or similar metadata for admin UI to fix
  - For block-waypoints that reference missing blocks, keep entries but mark them disabled (do not render) and show error banner

### Threading & Thread-Safety
- Storage and migration must not block render thread
- Load on background thread during startup with synchronous API to finish before UI usage

### Security & Permissions
- Teleport actions must verify permission/compatibility
- If teleportation requires server support, show fallback instructions

### Localization
- All user-facing strings must be localized
- Provide default English keys and support translation files

---

## Testing Checklist & QA Steps

### Unit Tests
- [ ] YAML serialization/deserialization round-trip tests (location, blocks, categories)
- [ ] Migration tests for all prior schema versions
- [ ] Atomic save behavior simulation (tmp file, rename, failure cases)
- [ ] Validation tests (invalid coordinates, empty block-coordinate list)

### Integration Tests
- [ ] UI flow tests (add/edit/delete) — with headless UI mocks if available
- [ ] Renderer integration test: rendering many waypoints to measure frame time
- [ ] Item search performance tests (with registry mocked to large item set)

### Manual QA Checklist
- [ ] Add a location waypoint at current pos
- [ ] Add a blocks waypoint and edit block list
- [ ] Test grouping by placing many waypoints within radius; verify cluster badges
- [ ] Test LOD: move player through NEAR/MID/FAR thresholds and verify label/icon visibility
- [ ] Test cross-dimension waypoints and dimension filters
- [ ] Test save/load with multiple categories; ensure category ordering persists
- [ ] Test undo/redo actions in UI
- [ ] Test backup rotation, inspect backup files
- [ ] Simulate corrupt YAML and ensure fallback/backup loads and logs

### Performance QA
- [ ] Stress test with 1000+ waypoints across the world; measure FPS
- [ ] Verify frustum culling effectiveness: 99% of off-screen waypoints should not produce render allocations
- [ ] Verify combined-block overlay performance when large set of blocks present (e.g., large farm)

### Accessibility QA
- [ ] Keyboard navigability for modals and lists
- [ ] Color contrast tests for color pickers and icons

---

## Estimated Effort & Next Steps

### Estimated Effort (Developer-Days, 1 Dev)
- **Data model & storage:** 1–2 days
- **Settings & keybinds:** 0.5 day
- **Add/Edit modal + Waypoints screen (UI):** 3–5 days
- **Item search overlay & block editor:** 2–3 days
- **Rendering engine & LOD/clustering:** 4–6 days
- **API/events/commands:** 1–2 days
- **Tests + QA + perf tuning:** 2–4 days
- **TOTAL:** ~14–22 developer-days (rough); **complexity: medium-high** due to rendering and performance tuning

### Priority & Next Steps (What to Implement First)
1. **Implement data model, storage, and migration:** Critical to avoid data loss
2. **Implement WaypointsManager and Settings** so other parts can query and persist
3. **Create basic Waypoints screen and Add modal** (minimal fields) to enable user testing
4. **Implement renderer minimal** (single-icon rendering w/ LOD) and gradually add grouping and combined overlays
5. **Implement item-search and block-list editor**
6. **Finish UX polish, settings, and events.** Add tests and perform performance QA

---

## Summary

This specification provides a comprehensive, developer-ready guide to implementing WaypointsV2 for Glowberry. Start with storage and data model, followed by basic UI, then rendering and performance optimizations. Keep migration and backup strategies robust to protect user data. All components should be localization-ready and designed with accessibility in mind.

For questions or implementation details, refer to the specific sections above and use the provided pseudocode and YAML examples as templates.


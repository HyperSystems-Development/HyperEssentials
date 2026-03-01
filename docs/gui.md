# GUI System

> **Status:** All 6 player pages + 4 admin pages complete. Remaining admin pages: Players, Moderation, Announcements, Settings (Phase 5).

## Overview

HyperEssentials provides a dual GUI system (Player + Admin) with shared navigation, following the same patterns as HyperFactions. The system supports 14 total pages across two navigation contexts.

**Player GUI (6 tabs):** Dashboard, Homes, Warps, Kits, TPA, Stats
**Admin GUI (8 tabs):** Dashboard, Players, Warps, Spawns, Kits, Moderation, Announcements, Settings

### Opening GUIs

| Command | Opens |
|---------|-------|
| `/he` (no args) | Player Dashboard (with nav bar to all player tabs) |
| `/homes` | Homes page directly |
| `/warps` | Warps page directly |
| `/kits` | Kits page directly |
| `/he admin` | Admin Dashboard (requires `admin.gui` permission) |

## Components

### GuiManager
Central hub managing two `PageRegistry` instances (player and admin) plus an `ActivePageTracker`. Provides convenience methods `openPlayerPage()` and `openAdminPage()` that delegate to `PlayerPageOpener` / `AdminPageOpener`.

### PageRegistry
Dynamic registry where modules register GUI page entries. Each entry includes:
- `id` — page identifier (e.g., `"homes"`)
- `displayName` — nav bar label
- `module` — owning module name
- `permission` — required permission (nullable)
- `supplier` — page factory
- `showsInNavBar` — whether to show in navigation
- `order` — sort order in nav bar (gaps of 10 for future inserts)

When a module is disabled, its pages are automatically unregistered.

### Page Registration Order

**Player pages:** Dashboard(0), Homes(10), Warps(20), Kits(30), TPA(40), Stats(50)
**Admin pages:** Dashboard(0), Players(10), Warps(20), Spawns(30), Kits(40), Moderation(50), Announcements(60), Settings(70)

### PlayerPageOpener / AdminPageOpener
Static utilities to resolve a page from the registry, check permissions, and open it. `AdminPageOpener` additionally requires `admin.gui` permission.

### NavBarHelper
Builds the shared navigation bar and handles navigation events. Provides:
- `setupBar()` — player nav bar with "HyperEssentials" title
- `setupAdminBar()` — admin nav bar with "HE Admin" title
- `handleNavEvent()` — routes navigation with `GuiType` (PLAYER or ADMIN)

### Event Data Classes

| Class | Fields | Purpose |
|-------|--------|---------|
| `PlayerPageData` | button, navTarget, target, page | Player page events |
| `AdminPageData` | button, navTarget, target, value, filter, page | Admin page events (extra fields for search/filter) |

### ActivePageTracker
Thread-safe tracker mapping players to their currently open page. Used for push-refresh updates.

### RefreshablePage
Interface for pages that support real-time content refresh without full page rebuild.

### GuiColors
Centralized semantic color constants: brand gold, text (primary/heading/muted/label), status (online/offline/active/inactive), semantic (success/danger/warning/info), backgrounds (dark/panel/card/nav), dividers. Includes helper methods `forModuleEnabled()` and `forOnlineStatus()`.

### UIHelper
Formatting utilities: `formatCoords()`, `formatDuration()`, `formatRelativeTime()`, `formatLimit()`, `formatPlaytime()`, `truncate()`, `formatWorldName()`, `parseColorCodes()`.

## UI Resources

```
Common/UI/Custom/HyperEssentials/
  shared/
    styles.ui              TextButtonStyle definitions (Gold, Green, Red, Aqua, Flat variants)
    nav_bar.ui             Navigation bar template with brand title
    nav_button.ui          Inactive nav button (MenuItem)
    nav_button_active.ui   Active nav button (TextButton, highlighted)
    error_page.ui          Generic error display
    empty_state.ui         Empty list state (title + message)
    confirm_modal.ui       Confirmation modal (message + Confirm/Cancel buttons)
    stat_row.ui            Reusable label:value row for stats display
  homes/
    homes_page.ui          Browse homes list with count/limit header
    home_entry.ui          Home row: name, world, coords, Teleport/Delete buttons
  warps/
    warps_page.ui          Browse warps with category grouping
    warp_entry.ui          Warp row: name, category, world, coords, Teleport button
    warp_category_header.ui  Category section divider
  kits/
    kits_page.ui           Browse available kits with count header
    kit_entry.ui           Kit row: name, items, cooldown, Claim/Preview buttons
  teleport/
    tpa_page.ui            TPA requests list with toggle bar
    tpa_entry.ui           TPA request row: requester, type, time, Accept/Deny
  player/
    dashboard.ui           Welcome screen with stat cards and quick actions
    stats.ui               Player stats and status indicators
  admin/
    admin_dashboard.ui     Server overview with stats and module grid
    admin_module_card.ui   Module status card (name + enabled/disabled)
    admin_warps.ui         Manage warps list with create/delete
    admin_warp_entry.ui    Warp row: name, category, world, coords, Delete
    admin_spawns.ui        Manage spawns list with create/delete
    admin_spawn_entry.ui   Spawn row: name, default badge, world, coords, Delete
    admin_kits.ui          Manage kits list with create/delete
    admin_kit_entry.ui     Kit row: name, items, cooldown, one-time, Delete
```

## Style System

Styles follow HyperFactions' proven pattern using `TextButtonStyle(...)` tuple syntax with `$C.@DefaultSquareButtonDefaultBackground` native backgrounds:

- `@ButtonStyle` — default neutral button
- `@GoldButtonStyle` — brand accent actions
- `@GreenButtonStyle` — success/confirm actions
- `@RedButtonStyle` — danger text actions
- `@AquaButtonStyle` — info actions
- `@InvisibleButtonStyle` — transparent click overlays
- `@DisabledButtonStyle` — grayed out, non-interactive
- `@FlatRedButtonStyle` — solid red background (delete/ban)
- `@FlatGreenButtonStyle` — solid green background (confirm)
- `@FlatGoldButtonStyle` — solid gold background (brand accent)

## Registering Pages

Pages are registered centrally in `HyperEssentials.registerPages()` after all module managers are initialized:

```java
playerReg.registerEntry(new PageRegistry.Entry(
    "homes", "Homes", "homes", Permissions.HOME_LIST,
    (player, ref, store, playerRef, gm) ->
        new HomesPage(player, playerRef, homes.getHomeManager(), warmupManager, gm),
    true, 10
));
```

## Command GUI Integration

Commands try to open their GUI page first, falling back to text output:

```java
// In execute():
if (tryOpenGui(store, ref, playerRef)) return;
// ... text fallback ...

private boolean tryOpenGui(Store store, Ref ref, PlayerRef playerRef) {
    if (!HyperEssentialsAPI.isAvailable()) return false;
    GuiManager gm = HyperEssentialsAPI.getInstance().getGuiManager();
    if (gm.getPlayerRegistry().getEntry("homes") == null) return false;
    Player player = store.getComponent(ref, Player.getComponentType());
    if (player == null) return false;
    return gm.openPlayerPage("homes", player, ref, store, playerRef);
}
```

## Implemented Player Pages

### HomesPage
- **File:** `gui/player/HomesPage.java`
- **Features:** Browse homes with count/limit header, teleport with warmup, delete homes
- **Dynamic list:** clears `#HomeList`, appends `home_entry.ui` entries with indexed selectors
- **Teleport:** Uses `WarmupManager.startWarmup()` → `Teleport.createForPlayer()`, closes GUI on teleport
- **Events:** Teleport, Delete buttons per entry; Nav bar navigation

### WarpsPage
- **File:** `gui/player/WarpsPage.java`
- **Features:** Browse warps grouped by category, teleport with warmup
- **Category grouping:** Inserts `warp_category_header.ui` before each category's warp entries
- **Teleport:** Same warmup pattern as HomesPage, closes GUI on teleport
- **Events:** Teleport button per entry; Nav bar navigation

### KitsPage
- **File:** `gui/player/KitsPage.java`
- **Features:** Browse available kits, claim with cooldown display, preview
- **Cooldown display:** Shows remaining cooldown time, "Ready", "One-time kit", or nothing
- **Claim:** Calls `kitManager.claimKit()` and rebuilds list to update cooldown status
- **Events:** Claim, Preview buttons per entry; Nav bar navigation

### PlayerDashboardPage
- **File:** `gui/player/PlayerDashboardPage.java`
- **Features:** Welcome message, 3 stat cards (homes count, online players, TPA requests), quick action buttons (Homes, Warps, Kits), playtime and first join date
- **Dependencies:** Optional HomeManager, TpaManager, UtilityManager (graceful degradation if any are null)
- **Events:** NavDirect buttons for quick page navigation; Nav bar navigation

### TpaPage
- **File:** `gui/player/TpaPage.java`
- **Features:** Incoming TPA requests with per-entry accept/deny, toggle TPA acceptance, request count, time remaining
- **Dynamic list:** Clears `#RequestList`, appends `tpa_entry.ui` entries with indexed selectors
- **Events:** Accept, Deny buttons per request (pass requester UUID as target); Toggle button; Nav bar navigation
- **Updates:** Rebuilds request list after accept/deny/toggle actions

### StatsPage
- **File:** `gui/player/StatsPage.java`
- **Features:** Player stats (first joined, last login, total playtime, current session) and status indicators (AFK, Fly, God, Infinite Stamina)
- **Dynamic lists:** Uses `stat_row.ui` for both `#StatsList` and `#StatusList`
- **Status coloring:** Active status indicators colored green (`#4aff7f`)
- **Events:** Nav bar navigation

## Implemented Admin Pages

### AdminDashboardPage
- **File:** `gui/admin/AdminDashboardPage.java`
- **Features:** Server overview — online player count, warp/spawn/kit stats cards, module status grid
- **Module grid:** Lists all registered modules with name, enabled/disabled status, color-coded indicator dot
- **Events:** Nav bar navigation only (read-only dashboard)

### AdminWarpsPage
- **File:** `gui/admin/AdminWarpsPage.java`
- **Features:** List all warps sorted by category then name, create at player location, delete
- **Create:** Generates auto-named warp at admin's current position/world
- **Events:** Create button, Delete button per entry; Nav bar navigation

### AdminSpawnsPage
- **File:** `gui/admin/AdminSpawnsPage.java`
- **Features:** List all spawns with default badge, create at player location, delete
- **Sorting:** Default spawn listed first, then alphabetical
- **Events:** Create button, Delete button per entry; Nav bar navigation

### AdminKitsPage
- **File:** `gui/admin/AdminKitsPage.java`
- **Features:** List all kits with item count, cooldown, one-time badge; create from inventory, delete
- **Create:** Captures admin's current inventory as a new kit
- **Events:** Create button, Delete button per entry; Nav bar navigation

## Page Implementation Pattern

```java
public class HomesPage extends InteractiveCustomUIPage<PlayerPageData> {
  public HomesPage(Player player, PlayerRef playerRef, HomeManager homeManager,
                   WarmupManager warmupManager, GuiManager guiManager) {
    super(playerRef, CustomPageLifetime.CanDismiss, PlayerPageData.CODEC);
  }

  @Override
  public void build(Ref ref, UICommandBuilder cmd, UIEventBuilder events, Store store) {
    cmd.append(UIPaths.HOMES_PAGE);
    NavBarHelper.setupBar(playerRef, "homes", guiManager.getPlayerRegistry(), cmd, events);
    buildHomeList(cmd, events);  // populate dynamic list
  }

  @Override
  public void handleDataEvent(Ref ref, Store store, PlayerPageData data) {
    if ("Nav".equals(data.button)) {
      NavBarHelper.handleNavEvent(data.navTarget, player, ref, store, playerRef, guiManager, GuiType.PLAYER);
      return;
    }
    switch (data.button) {
      case "Teleport" -> handleTeleport(ref, data.target);
      case "Delete" -> handleDelete(ref, store, data.target);
    }
  }

  private void rebuildList() {
    UICommandBuilder cmd = new UICommandBuilder();
    UIEventBuilder events = new UIEventBuilder();
    buildHomeList(cmd, events);
    sendUpdate(cmd, events, false);  // partial update, not full rebuild
  }
}
```

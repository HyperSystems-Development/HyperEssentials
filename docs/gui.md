# GUI System

> **Status:** Foundation infrastructure complete. Page registration ready. Phase 2-5 pages pending.

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
  homes/                   Home page templates (Phase 2)
  warps/                   Warp page templates (Phase 2)
  kits/                    Kit page templates (Phase 2)
  teleport/                TPA page templates (Phase 3)
  player/                  Dashboard and stats templates (Phase 3)
  admin/                   Admin page templates (Phase 4-5)
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

## Registering a Page

Modules register pages in their `onEnable()`:

```java
guiManager.getPlayerRegistry().registerEntry(new PageRegistry.Entry(
    "homes",                          // id
    "Homes",                          // displayName
    "homes",                          // module
    Permissions.HOME_GUI,             // permission
    this::createHomesPage,            // supplier
    true,                             // showsInNavBar
    10                                // order
));
```

## Page Implementation Pattern

```java
public class HomesPage extends InteractiveCustomUIPage<PlayerPageData> {
  public HomesPage(PlayerRef playerRef, HomeManager homeManager, GuiManager guiManager) {
    super(playerRef, CustomPageLifetime.CanDismiss, PlayerPageData.CODEC);
  }

  @Override
  public void build(Ref ref, UICommandBuilder cmd, UIEventBuilder events, Store store) {
    cmd.append(UIPaths.HOMES_PAGE);
    NavBarHelper.setupBar(playerRef, "homes", guiManager.getPlayerRegistry(), cmd, events);
    // populate content...
  }

  @Override
  public void handleDataEvent(Ref ref, Store store, PlayerPageData data) {
    if ("Nav".equals(data.button)) {
      NavBarHelper.handleNavEvent(data.navTarget, player, ref, store, playerRef, guiManager);
      return;
    }
    // handle page-specific events...
  }
}
```

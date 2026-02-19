# GUI System

> **Status:** Framework complete, no module pages implemented yet.

## Overview

HyperEssentials provides a shared GUI framework that modules use to register interactive pages. The system supports a unified navigation bar across all pages.

## Components

### GuiManager
Central hub managing two `PageRegistry` instances (player and admin) plus an `ActivePageTracker`.

### PageRegistry
Dynamic registry where modules register GUI page entries. Each entry includes:
- `id` — page identifier (e.g., `"homes"`)
- `displayName` — nav bar label
- `module` — owning module name
- `permission` — required permission (nullable)
- `supplier` — page factory
- `showsInNavBar` — whether to show in navigation
- `order` — sort order in nav bar

When a module is disabled, its pages are automatically unregistered.

### NavBarHelper
Builds the shared navigation bar and handles navigation events. Filters entries by player permissions.

### ActivePageTracker
Thread-safe tracker mapping players to their currently open page. Used for push-refresh updates.

### RefreshablePage
Interface for pages that support real-time content refresh without full page rebuild.

## UI Resources

```
Common/UI/Custom/HyperEssentials/
  shared/
    styles.ui              Color constants and reusable styles
    nav_bar.ui             Navigation bar template
    nav_button.ui          Inactive nav button
    nav_button_active.ui   Active nav button (highlighted)
    error_page.ui          Generic error display
  homes/                   (empty — future)
  warps/                   (empty — future)
  spawns/                  (empty — future)
  teleport/                (empty — future)
  admin/                   (empty — future)
  kits/                    (empty — future)
```

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

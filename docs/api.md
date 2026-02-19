# API

> **Status:** Stub only.

## Overview

HyperEssentials provides a public API via `HyperEssentialsAPI` for other plugins to interact with.

## Usage

```java
if (HyperEssentialsAPI.isAvailable()) {
    HyperEssentials he = HyperEssentialsAPI.getInstance();

    // Check if a module is enabled
    boolean homesEnabled = he.isModuleEnabled("homes");

    // Get a specific module
    HomesModule homes = he.getModule(HomesModule.class);
}
```

## API Methods

| Method | Description |
|--------|-------------|
| `HyperEssentialsAPI.isAvailable()` | Check if HyperEssentials is loaded |
| `HyperEssentialsAPI.getInstance()` | Get the core singleton |
| `isModuleEnabled(String)` | Check if a module is enabled |
| `getModule(Class<T>)` | Get a module instance by class |
| `getConfigManager()` | Access configuration |
| `getGuiManager()` | Access GUI system |
| `getStorageProvider()` | Access storage |
| `getWarmupManager()` | Access warmup system |

## Events

The `EventBus` provides publish-subscribe for cross-plugin events. Event types will be defined as modules are implemented.

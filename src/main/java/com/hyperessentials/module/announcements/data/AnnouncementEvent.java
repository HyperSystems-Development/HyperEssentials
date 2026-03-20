package com.hyperessentials.module.announcements.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * An event-triggered announcement (join, leave, first_join).
 *
 * @param id         unique identifier
 * @param eventType  "join", "leave", or "first_join"
 * @param message    the announcement text with placeholders ({player}, {online}, {max})
 * @param type       delivery type (CHAT or NOTIFICATION)
 * @param enabled    whether the event announcement is active
 * @param permission required permission to see (null = all players)
 */
public record AnnouncementEvent(
    @NotNull UUID id,
    @NotNull String eventType,
    @NotNull String message,
    @NotNull AnnouncementType type,
    boolean enabled,
    @Nullable String permission
) {

  /** Returns a copy with the given message. */
  @NotNull
  public AnnouncementEvent withMessage(@NotNull String newMessage) {
    return new AnnouncementEvent(id, eventType, newMessage, type, enabled, permission);
  }

  /** Returns a copy with the given type. */
  @NotNull
  public AnnouncementEvent withType(@NotNull AnnouncementType newType) {
    return new AnnouncementEvent(id, eventType, message, newType, enabled, permission);
  }

  /** Returns a copy with the given enabled state. */
  @NotNull
  public AnnouncementEvent withEnabled(boolean newEnabled) {
    return new AnnouncementEvent(id, eventType, message, type, newEnabled, permission);
  }
}

package com.hyperessentials.module.announcements.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * A single announcement definition with type, schedule, and filters.
 *
 * @param id             unique identifier
 * @param message        the announcement text (supports color codes)
 * @param type           delivery type (CHAT or NOTIFICATION)
 * @param enabled        whether the announcement is active
 * @param priority       display priority (higher = more important)
 * @param permission     required permission to see (null = all players)
 * @param world          world filter (null = all worlds)
 * @param cronExpression cron schedule override (null = use global interval)
 * @param order          position in rotation order
 */
public record Announcement(
    @NotNull UUID id,
    @NotNull String message,
    @NotNull AnnouncementType type,
    boolean enabled,
    int priority,
    @Nullable String permission,
    @Nullable String world,
    @Nullable String cronExpression,
    int order
) {

  public Announcement {
    if (priority < 0) priority = 0;
    if (order < 0) order = 0;
  }

  /** Returns a copy with the given message. */
  @NotNull
  public Announcement withMessage(@NotNull String newMessage) {
    return new Announcement(id, newMessage, type, enabled, priority, permission, world, cronExpression, order);
  }

  /** Returns a copy with the given type. */
  @NotNull
  public Announcement withType(@NotNull AnnouncementType newType) {
    return new Announcement(id, message, newType, enabled, priority, permission, world, cronExpression, order);
  }

  /** Returns a copy with the given enabled state. */
  @NotNull
  public Announcement withEnabled(boolean newEnabled) {
    return new Announcement(id, message, type, newEnabled, priority, permission, world, cronExpression, order);
  }

  /** Returns a copy with the given permission. */
  @NotNull
  public Announcement withPermission(@Nullable String newPermission) {
    return new Announcement(id, message, type, enabled, priority, newPermission, world, cronExpression, order);
  }

  /** Returns a copy with the given world filter. */
  @NotNull
  public Announcement withWorld(@Nullable String newWorld) {
    return new Announcement(id, message, type, enabled, priority, permission, newWorld, cronExpression, order);
  }

  /** Returns a copy with the given cron expression. */
  @NotNull
  public Announcement withCronExpression(@Nullable String newCron) {
    return new Announcement(id, message, type, enabled, priority, permission, world, newCron, order);
  }

  /** Returns a copy with the given order. */
  @NotNull
  public Announcement withOrder(int newOrder) {
    return new Announcement(id, message, type, enabled, priority, permission, world, cronExpression, newOrder);
  }

  /** Returns a copy with the given priority. */
  @NotNull
  public Announcement withPriority(int newPriority) {
    return new Announcement(id, message, type, enabled, newPriority, permission, world, cronExpression, order);
  }
}

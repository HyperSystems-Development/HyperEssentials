package com.hyperessentials.gui.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import org.jetbrains.annotations.Nullable;

/**
 * Event data codec for the player settings page.
 */
public class PlayerSettingsData {

  public @Nullable String button;
  public @Nullable String navTarget;
  public @Nullable String language;

  public static final BuilderCodec<PlayerSettingsData> CODEC = BuilderCodec
      .builder(PlayerSettingsData.class, PlayerSettingsData::new)
      .addField(
          new KeyedCodec<>("Button", Codec.STRING),
          (data, v) -> data.button = v,
          data -> data.button
      )
      .addField(
          new KeyedCodec<>("NavTarget", Codec.STRING),
          (data, v) -> data.navTarget = v,
          data -> data.navTarget
      )
      .addField(
          new KeyedCodec<>("@Language", Codec.STRING),
          (data, v) -> data.language = v,
          data -> data.language
      )
      .build();

  public PlayerSettingsData() {}
}

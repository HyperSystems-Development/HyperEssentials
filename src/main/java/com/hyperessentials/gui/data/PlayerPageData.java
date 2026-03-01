package com.hyperessentials.gui.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import org.jetbrains.annotations.Nullable;

/**
 * Event data codec for all player GUI pages.
 * Follows the HyperFactions NavAwareData pattern.
 */
public class PlayerPageData {

  public @Nullable String button;
  public @Nullable String navTarget;
  public @Nullable String target;
  public int page;

  public static final BuilderCodec<PlayerPageData> CODEC = BuilderCodec
      .builder(PlayerPageData.class, PlayerPageData::new)
      .addField(
          new KeyedCodec<>("Button", Codec.STRING),
          (data, value) -> data.button = value,
          data -> data.button
      )
      .addField(
          new KeyedCodec<>("NavTarget", Codec.STRING),
          (data, value) -> data.navTarget = value,
          data -> data.navTarget
      )
      .addField(
          new KeyedCodec<>("Target", Codec.STRING),
          (data, value) -> data.target = value,
          data -> data.target
      )
      .addField(
          new KeyedCodec<>("Page", Codec.INTEGER),
          (data, value) -> data.page = value,
          data -> data.page
      )
      .build();

  public PlayerPageData() {}
}

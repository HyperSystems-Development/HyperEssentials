package com.hyperessentials.gui.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import org.jetbrains.annotations.Nullable;

/**
 * Event data codec for all admin GUI pages.
 * Extends the player pattern with additional fields for admin actions.
 */
public class AdminPageData {

  public @Nullable String button;
  public @Nullable String navTarget;
  public @Nullable String target;
  public @Nullable String value;
  public @Nullable String filter;
  public int page;

  public static final BuilderCodec<AdminPageData> CODEC = BuilderCodec
      .builder(AdminPageData.class, AdminPageData::new)
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
          new KeyedCodec<>("Target", Codec.STRING),
          (data, v) -> data.target = v,
          data -> data.target
      )
      .addField(
          new KeyedCodec<>("Value", Codec.STRING),
          (data, v) -> data.value = v,
          data -> data.value
      )
      .addField(
          new KeyedCodec<>("Filter", Codec.STRING),
          (data, v) -> data.filter = v,
          data -> data.filter
      )
      .addField(
          new KeyedCodec<>("Page", Codec.INTEGER),
          (data, v) -> data.page = v,
          data -> data.page
      )
      .build();

  public AdminPageData() {}
}

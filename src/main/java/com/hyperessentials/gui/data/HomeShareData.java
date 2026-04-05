package com.hyperessentials.gui.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import org.jetbrains.annotations.Nullable;

/**
 * Event data codec for the home share page.
 * Includes a dynamic search text field read via @ prefix.
 */
public class HomeShareData {

  public @Nullable String button;
  public @Nullable String target;
  public @Nullable String searchText;

  public static final BuilderCodec<HomeShareData> CODEC = BuilderCodec
      .builder(HomeShareData.class, HomeShareData::new)
      .addField(
          new KeyedCodec<>("Button", Codec.STRING),
          (data, value) -> data.button = value,
          data -> data.button
      )
      .addField(
          new KeyedCodec<>("Target", Codec.STRING),
          (data, value) -> data.target = value,
          data -> data.target
      )
      .addField(
          new KeyedCodec<>("@SearchText", Codec.STRING),
          (data, value) -> data.searchText = value,
          data -> data.searchText
      )
      .build();

  public HomeShareData() {}
}

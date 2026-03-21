package com.hyperessentials.gui.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import org.jetbrains.annotations.Nullable;

/**
 * Event data codec for the Admin Config editor page.
 */
public class AdminConfigData {

  public @Nullable String button;
  public @Nullable String navTarget;
  public @Nullable String tab;
  public @Nullable String settingKey;

  // Dynamic input fields (read from UI elements via @ prefix)
  public @Nullable String numInput;
  public @Nullable String strInput;
  public @Nullable String colorValue;
  public @Nullable String enumValue;

  public static final BuilderCodec<AdminConfigData> CODEC = BuilderCodec
      .builder(AdminConfigData.class, AdminConfigData::new)
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
          new KeyedCodec<>("Tab", Codec.STRING),
          (data, v) -> data.tab = v,
          data -> data.tab
      )
      .addField(
          new KeyedCodec<>("SettingKey", Codec.STRING),
          (data, v) -> data.settingKey = v,
          data -> data.settingKey
      )
      .addField(
          new KeyedCodec<>("@numInput", Codec.STRING),
          (data, v) -> data.numInput = v,
          data -> data.numInput
      )
      .addField(
          new KeyedCodec<>("@strInput", Codec.STRING),
          (data, v) -> data.strInput = v,
          data -> data.strInput
      )
      .addField(
          new KeyedCodec<>("@colorValue", Codec.STRING),
          (data, v) -> data.colorValue = v,
          data -> data.colorValue
      )
      .addField(
          new KeyedCodec<>("@enumValue", Codec.STRING),
          (data, v) -> data.enumValue = v,
          data -> data.enumValue
      )
      .build();

  public AdminConfigData() {}
}

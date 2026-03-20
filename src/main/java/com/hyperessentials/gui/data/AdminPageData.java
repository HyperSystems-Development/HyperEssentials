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

  // Dynamic text input fields (read from UI elements via @ prefix)
  public @Nullable String inputName;
  public @Nullable String inputCategory;
  public @Nullable String inputDescription;
  public @Nullable String inputPermission;

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
      .addField(
          new KeyedCodec<>("@InputName", Codec.STRING),
          (data, v) -> data.inputName = v,
          data -> data.inputName
      )
      .addField(
          new KeyedCodec<>("@InputCategory", Codec.STRING),
          (data, v) -> data.inputCategory = v,
          data -> data.inputCategory
      )
      .addField(
          new KeyedCodec<>("@InputDescription", Codec.STRING),
          (data, v) -> data.inputDescription = v,
          data -> data.inputDescription
      )
      .addField(
          new KeyedCodec<>("@InputPermission", Codec.STRING),
          (data, v) -> data.inputPermission = v,
          data -> data.inputPermission
      )
      .build();

  public AdminPageData() {}
}

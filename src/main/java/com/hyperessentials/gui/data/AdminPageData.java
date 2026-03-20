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
  public @Nullable String inputCooldown;
  public @Nullable String inputSearch;
  public @Nullable String inputDuration;
  public @Nullable String inputReason;

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
      .addField(
          new KeyedCodec<>("@InputCooldown", Codec.STRING),
          (data, v) -> data.inputCooldown = v,
          data -> data.inputCooldown
      )
      .addField(
          new KeyedCodec<>("@SearchInput", Codec.STRING),
          (data, v) -> data.inputSearch = v,
          data -> data.inputSearch
      )
      .addField(
          new KeyedCodec<>("@DurationInput", Codec.STRING),
          (data, v) -> data.inputDuration = v,
          data -> data.inputDuration
      )
      .addField(
          new KeyedCodec<>("@ReasonInput", Codec.STRING),
          (data, v) -> data.inputReason = v,
          data -> data.inputReason
      )
      .build();

  public AdminPageData() {}
}

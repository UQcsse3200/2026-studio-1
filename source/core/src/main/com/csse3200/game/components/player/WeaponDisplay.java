package com.csse3200.game.components.player;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.csse3200.game.components.loot.WeaponItem;
import com.csse3200.game.components.loot.WeaponType;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.UIComponent;

public class WeaponDisplay extends UIComponent {
  private WeaponItem weapon;
  private Table table;
  private Label weaponLabel;
  private Image weaponImage;

  // Current bonus from the Sword Damage upgrade (0 when inactive/expired). Updated purely via
  // the "swordDamageBonusChanged" event fired on this same entity by UpgradesDisplay.
  private int swordDamageBonus = 0;

  public WeaponDisplay(WeaponItem weapon) {
    this.weapon = weapon;
  }

  @Override
  public void create() {
    super.create();
    addActors();
    entity.getEvents().addListener("swordDamageBonusChanged", this::onSwordDamageBonusChanged);
    entity.getEvents().addListener("activeSlotChanged", this::onActiveSlotChanged);
    entity.getEvents().addListener("inventoryChanged", this::onInventoryChanged);
  }

  private void addActors() {
    table = new Table();
    table.top().left();
    table.setFillParent(true);
    table.padTop(125f).padLeft(10f);

    weaponImage = new Image(getWeaponTexture());
    applyTierColor();

    // The "large" skin style defaults to a dark font colour that is unreadable against this
    // dungeon background, so we copy the style and override just its font colour to white.
    Label.LabelStyle whiteLargeStyle =
        new Label.LabelStyle(skin.get("default", Label.LabelStyle.class));
    whiteLargeStyle.fontColor = Color.WHITE;
    weaponLabel = new Label(buildLabelText(), whiteLargeStyle);

    table.add(weaponImage).size(45f).padRight(10f);
    table.add(weaponLabel).left();

    stage.addActor(table);
  }

  /** Called when the player switches their active inventory slot. */
  private void onActiveSlotChanged(int activeSlot) {
    refreshActiveWeapon();
  }

  /** Called when inventory contents change (e.g. picking up a new weapon while it's active). */
  private void onInventoryChanged() {
    refreshActiveWeapon();
  }

  /** Re-reads the active weapon from the inventory and refreshes the HUD if it changed. */
  private void refreshActiveWeapon() {
    InventoryComponent inventory = entity.getComponent(InventoryComponent.class);
    if (inventory == null) {
      return;
    }

    if (!(inventory.getActiveItem() instanceof WeaponItem activeWeapon)) {
      // Active slot is empty or holds a non-weapon item - keep showing the last weapon.
      return;
    }

    if (activeWeapon == weapon) {
      return;
    }

    weapon = activeWeapon;
    weaponImage.setDrawable(new TextureRegionDrawable(getWeaponTexture()));
    applyTierColor();
    weaponLabel.setText(buildLabelText());
  }

  private Texture getWeaponTexture() {
    String imagePath =
        switch (weapon.getWeaponType()) {
          case BOW -> "images/items/bow.png";
          case DAGGER -> "images/dagger.png";
          case SWORD -> "images/items/sword.png";
          case AXE -> "images/items/axe.png";
        };

    return ServiceLocator.getResourceService().getAsset(imagePath, Texture.class);
  }

  private void applyTierColor() {
    switch (weapon.getTier()) {
      case 1 -> weaponImage.setColor(Color.WHITE);
      case 2 -> weaponImage.setColor(Color.GOLD);
      case 3 -> weaponImage.setColor(Color.PURPLE);
      default -> weaponImage.setColor(Color.WHITE);
    }
  }

  /**
   * Called whenever UpgradesDisplay's Sword Damage effect changes (including back to 0 on expiry).
   */
  private void onSwordDamageBonusChanged(int bonus) {
    swordDamageBonus = bonus;
    weaponLabel.setText(buildLabelText());
  }

  private String buildLabelText() {
    int bonus = weapon.getWeaponType() == WeaponType.SWORD ? swordDamageBonus : 0;
    return String.format(
        "Weapon: %s\nType: %s\nTier: %d\nDamage: %d",
        weapon.getName(), weapon.getWeaponType(), weapon.getTier(), weapon.getDamage() + bonus);
  }

  @Override
  public void draw(SpriteBatch batch) {}

  @Override
  public void dispose() {
    super.dispose();
    weaponImage.remove();
    weaponLabel.remove();
    table.remove();
  }
}

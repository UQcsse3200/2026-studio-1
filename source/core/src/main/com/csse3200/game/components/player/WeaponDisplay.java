package com.csse3200.game.components.player;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.csse3200.game.components.loot.WeaponItem;
import com.csse3200.game.components.loot.WeaponType;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.UIComponent;

public class WeaponDisplay extends UIComponent {
  private final WeaponItem weapon;
  private Table table;
  private Label weaponLabel;
  private Image weaponImage;

  // Current bonus from the Sword Damage upgrade (0 when inactive/expired). Updated purely via
  // the "swordDamageBonusChanged" event fired on this same entity by UpgradesDisplay - this
  // class has no direct reference to UpgradesDisplay and doesn't need one.
  private int swordDamageBonus = 0;

  public WeaponDisplay(WeaponItem weapon) {
    this.weapon = weapon;
  }

  @Override
  public void create() {
    super.create();
    addActors();
    entity.getEvents().addListener("swordDamageBonusChanged", this::onSwordDamageBonusChanged);
  }

  private void addActors() {
    table = new Table();
    table.top().left();
    table.setFillParent(true);
    table.padTop(125f).padLeft(10f);

    String imagePath =
        weapon.getWeaponType() == WeaponType.BOW
            ? "images/items/bow.png"
            : "images/items/sword.png";

    weaponImage = new Image(ServiceLocator.getResourceService().getAsset(imagePath, Texture.class));

    switch (weapon.getTier()) {
      case 1:
        weaponImage.setColor(Color.WHITE);
        break;
      case 2:
        weaponImage.setColor(Color.GOLD);
        break;
      case 3:
        weaponImage.setColor(Color.PURPLE);
        break;
      default:
        weaponImage.setColor(Color.WHITE);
        break;
    }

    String text =
        String.format(
            "Weapon: %s\nType: %s\nTier: %d\nDamage: %d",
            weapon.getName(), weapon.getWeaponType(), weapon.getTier(), weapon.getDamage());

    weaponLabel = new Label(text, skin, "subtitle");

    table.add(weaponImage).size(45f).padRight(10f);
    table.add(weaponLabel).left();

    stage.addActor(table);
  }

  /**
   * Called whenever UpgradesDisplay's Sword Damage effect changes (including back to 0 on expiry).
   */
  private void onSwordDamageBonusChanged(int bonus) {
    swordDamageBonus = bonus;
    weaponLabel.setText(buildLabelText());
  }

  private String buildLabelText() {
    return String.format(
        "Weapon: %s\nType: %s\nDamage: %d",
        weapon.getName(), weapon.getWeaponType(), weapon.getDamage() + swordDamageBonus);
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

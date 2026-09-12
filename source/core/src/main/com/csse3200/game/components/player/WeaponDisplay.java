package com.csse3200.game.components.player;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.csse3200.game.components.loot.WeaponItem;
import com.csse3200.game.components.loot.WeaponType;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.UIComponent;

/**
 * Displays information about the player's currently equipped weapon.
 *
 * <p>The damage line shown is weapon.getDamage() (the weapon's own fixed stat) PLUS whatever Sword
 * Damage upgrade bonus is currently active - the upgrade modifies CombatStatsComponent. baseAttack
 * directly, which is a separate number from the weapon's own damage stat, so this class tracks the
 * upgrade's bonus itself (via an event) rather than reading baseAttack back off
 * CombatStatsComponent.
 */
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
    table.padTop(100f).padLeft(10f);

    String imagePath =
        weapon.getWeaponType() == WeaponType.BOW ? "images/bow.png" : "images/sword.png";

    weaponImage = new Image(ServiceLocator.getResourceService().getAsset(imagePath, Texture.class));

    weaponLabel = new Label(buildLabelText(), skin, "large");

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
  public void draw(SpriteBatch batch) {
    // Drawing handled by the stage.
  }

  @Override
  public void dispose() {
    super.dispose();
    weaponImage.remove();
    weaponLabel.remove();
    table.remove();
  }
}

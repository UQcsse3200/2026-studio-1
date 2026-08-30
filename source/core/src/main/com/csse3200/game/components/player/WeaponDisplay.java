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

/** Displays information about the player's currently equipped weapon. */
public class WeaponDisplay extends UIComponent {
  private final WeaponItem weapon;
  private Table table;
  private Label weaponLabel;
  private Image weaponImage;

  public WeaponDisplay(WeaponItem weapon) {
    this.weapon = weapon;
  }

  @Override
  public void create() {
    super.create();
    addActors();
  }

  private void addActors() {
    table = new Table();
    table.top().left();
    table.setFillParent(true);
    table.padTop(100f).padLeft(10f);

    String imagePath =
        weapon.getWeaponType() == WeaponType.BOW ? "images/bow.png" : "images/sword.png";

    weaponImage = new Image(ServiceLocator.getResourceService().getAsset(imagePath, Texture.class));

    String text =
        String.format(
            "Weapon: %s\nType: %s\nDamage: %d",
            weapon.getName(), weapon.getWeaponType(), weapon.getDamage());

    weaponLabel = new Label(text, skin, "large");

    table.add(weaponImage).size(45f).padRight(10f);
    table.add(weaponLabel).left();

    stage.addActor(table);
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

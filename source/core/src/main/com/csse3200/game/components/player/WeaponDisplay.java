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

    weaponImage =
        new Image(ServiceLocator.getResourceService().getAsset(imagePath, Texture.class));

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

    weaponLabel = new Label(text, skin, "large");

    table.add(weaponImage).size(45f).padRight(10f);
    table.add(weaponLabel).left();

    stage.addActor(table);
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
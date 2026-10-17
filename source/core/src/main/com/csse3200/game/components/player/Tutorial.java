package com.csse3200.game.components.player;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.csse3200.game.ui.UIComponent;
import java.util.ArrayList;
import java.util.List;

/**
 * Minecraft-style controls guide. All textures (bevelled panel, keycaps, divider) are generated at
 * runtime as pixel art, so no image assets are needed. Toggle with the "toggleTutorial" event.
 */
public class Tutorial extends UIComponent {

  private static final Color ACCENT = new Color(0.33f, 0.93f, 0.45f, 1f);
  private static final Color ACCENT_DIM = new Color(0.33f, 0.93f, 0.45f, 0.45f);
  private static final Color MUTED = new Color(0.72f, 0.72f, 0.78f, 1f);
  private static final Color SHADOW = new Color(0f, 0f, 0f, 0.65f);

  private final List<Texture> textures = new ArrayList<>();
  private Label.LabelStyle style;
  private Drawable panel;
  private Drawable keycap;
  private Drawable solid;

  private Table rootTable;
  private boolean tutorialVisible = true;

  @Override
  public void create() {
    super.create();

    // White-based style so setColor(...) tints show as written (the skin default can be dark).
    style = new Label.LabelStyle(skin.get("default", Label.LabelStyle.class));
    style.fontColor = Color.WHITE;

    panel =
        patch(
            10,
            4f,
            new Color(0f, 0f, 0f, 1f),
            new Color(0.5f, 0.5f, 0.54f, 1f),
            new Color(0.07f, 0.07f, 0.08f, 1f),
            new Color(0.14f, 0.14f, 0.16f, 0.92f));
    keycap =
        patch(
            8,
            3f,
            new Color(0f, 0f, 0f, 1f),
            new Color(0.68f, 0.68f, 0.72f, 1f),
            new Color(0.12f, 0.12f, 0.14f, 1f),
            new Color(0.32f, 0.32f, 0.36f, 1f));
    solid = solid();

    addActors();

    entity.getEvents().addListener("toggleTutorial", this::toggleTutorial);
  }

  /** Builds and centres the guide. */
  private void addActors() {

    rootTable = new Table();
    rootTable.setFillParent(true);
    rootTable.align(Align.top);
    rootTable.padTop(480f);

    Table card = new Table();
    card.setBackground(panel);
    card.pad(28f);

    card.add(text("FIELD GUIDE", ACCENT, 1.5f)).padBottom(2f).row();
    card.add(text("CONTROLS & ACTIONS", MUTED, 0.8f)).padBottom(6f).row();
    card.add(line()).growX().padBottom(6f).row();

    Table left = new Table();
    left.top().left();
    section(
        left,
        "MOVEMENT",
        new String[][] {
          {"W A S D", "Move"}, {"W", "Jump"}, {"LEFT SHIFT", "Slide"}, {"LEFT CTRL", "Crouch"}
        });
    section(left, "INTERACTION", new String[][] {{"E", "Interact"}, {"Q", "Drop Item"}});
    section(left, "MENUS", new String[][] {{"J", "Quest Menu"}, {"T", "Tutorial"}});

    Table right = new Table();
    right.top().left();
    section(
        right,
        "COMBAT",
        new String[][] {{"SPACE", "Attack"}, {"L", "Dash"}, {"B", "Shield"}, {"R", "Bribe Enemy"}});
    section(
        right,
        "INVENTORY",
        new String[][] {
          {"1 - 5", "Select Item"}, {"6 - 7", "Switch Pet"}, {"CLICK", "Select Slot"}
        });

    Table columns = new Table();
    columns.add(left).width(380f).top().padRight(36f);
    columns.add(right).width(380f).top();
    card.add(columns).padBottom(16f).row();

    Actor footer = text("PRESS T TO CLOSE", MUTED, 0.7f);
    footer.addAction(
        Actions.forever(Actions.sequence(Actions.alpha(0.35f, 0.9f), Actions.alpha(1f, 0.9f))));
    card.add(footer);

    rootTable.add(card);
    stage.addActor(rootTable);
  }

  /** Adds a accent-titled section: underline, then one keycap + description row per control. */
  private void section(Table column, String title, String[][] rows) {
    column.add(text(title, ACCENT, 1f)).left().padTop(14f).row();
    column.add(line()).growX().padBottom(8f).row();

    for (String[] control : rows) {
      Table cap = new Table();
      cap.setBackground(keycap);
      cap.add(text(control[0], Color.WHITE, 0.7f));

      Table row = new Table();
      row.add(cap).width(150f).height(42f).padRight(14f);
      row.add(text(control[1], Color.WHITE, 0.85f)).left().expandX();

      column.add(row).growX().padBottom(6f).row();
    }
  }

  /** Text with a Minecraft-style drop shadow (dark copy offset down-right). */
  private Actor text(String value, Color color, float scale) {
    Stack stack = new Stack();

    for (int i = 0; i < 2; i++) {
      boolean shadow = i == 0;
      Label label = new Label(value, style);
      label.setFontScale(scale);
      label.setColor(shadow ? SHADOW : color);

      Table layer = new Table();
      layer.add(label).pad(shadow ? 2f : 0f, shadow ? 2f : 0f, shadow ? 0f : 2f, shadow ? 0f : 2f);
      stack.add(layer);
    }

    return stack;
  }

  /** A thin accent divider line. */
  private Actor line() {
    Image line = new Image(solid);
    line.setColor(ACCENT_DIM);
    Table holder = new Table();
    holder.add(line).growX().height(2f);
    return holder;
  }

  /** Builds a scalable bevelled nine-patch (outline, highlight top/left, shade bottom/right). */
  private Drawable patch(int size, float scale, Color outline, Color hi, Color shade, Color fill) {
    Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
    pixmap.setBlending(Pixmap.Blending.None);
    int last = size - 1;

    for (int y = 0; y < size; y++) {
      for (int x = 0; x < size; x++) {
        int ring = Math.min(Math.min(x, y), Math.min(last - x, last - y));
        Color color = fill;
        if (ring == 0) {
          color = outline;
        } else if (ring == 1) {
          color = (y == 1 || x == 1) ? hi : shade;
        }
        pixmap.drawPixel(x, y, Color.rgba8888(color));
      }
    }

    NinePatch ninePatch = new NinePatch(texture(pixmap), 3, 3, 3, 3);
    ninePatch.scale(scale, scale);
    return new NinePatchDrawable(ninePatch);
  }

  /** A 1x1 white drawable, tinted via setColor wherever a solid colour is needed. */
  private Drawable solid() {
    Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
    pixmap.drawPixel(0, 0, Color.rgba8888(Color.WHITE));
    return new TextureRegionDrawable(new TextureRegion(texture(pixmap)));
  }

  /** Uploads a pixmap as a crisp texture, tracks it for disposal and frees the pixmap. */
  private Texture texture(Pixmap pixmap) {
    Texture texture = new Texture(pixmap);
    texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    pixmap.dispose();
    textures.add(texture);
    return texture;
  }

  private void toggleTutorial() {
    rootTable.clearActions();

    if (tutorialVisible) {
      tutorialVisible = false;
      rootTable.addAction(Actions.sequence(Actions.fadeOut(0.15f), Actions.visible(false)));
    } else {
      tutorialVisible = true;
      rootTable.setVisible(true);
      rootTable.getColor().a = 0f;
      rootTable.addAction(Actions.fadeIn(0.2f));
    }
  }

  @Override
  public void draw(SpriteBatch batch) {
    // Drawing is handled by the stage.
  }

  @Override
  public void dispose() {
    if (rootTable != null) {
      rootTable.remove();
      rootTable = null;
    }

    for (Texture texture : textures) {
      texture.dispose();
    }
    textures.clear();

    super.dispose();
  }
}

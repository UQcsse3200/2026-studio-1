package com.csse3200.game.components.player;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.csse3200.game.components.effects.FrozenCause;
import com.csse3200.game.components.effects.SpeedEffectComponent;
import com.csse3200.game.components.enemies.attacks.PetrifyEffectComponent;
import com.csse3200.game.ui.UIComponent;
import java.util.Locale;

/**
 * Shows a countdown label ("Frozen 1.8s" or "Petrified 1.8s") while the player cannot move. Reads
 * the remaining time from {@link SpeedEffectComponent} every frame and hides itself when the effect
 * ends, the player dies or the component is disposed.
 *
 * <p>Style reference: {@link SpecialAttackCooldownDisplay} (a UIComponent that owns one Table and
 * updates its label each frame). The text colour comes from {@link
 * FrozenTintComponent#colourFor(String)}, so the label and the player's tint always match.
 */
public class FrozenIndicatorDisplay extends UIComponent {
  /** Distance from the top of the screen, below the bribe message and clear of the hearts. */
  private static final float TOP_PADDING = 70f;

  private Table table;
  private Label label;
  private SpeedEffectComponent speedEffect;
  private String cause;

  /**
   * Builds the label table and starts listening for the cause, start, end and death events.
   *
   * @throws IllegalStateException if the entity has no SpeedEffectComponent
   */
  @Override
  public void create() {
    super.create();
    speedEffect = entity.getComponent(SpeedEffectComponent.class);
    if (speedEffect == null) {
      throw new IllegalStateException("Frozen indicator display requires SpeedEffectComponent.");
    }

    table = new Table();
    table.top();
    table.setFillParent(true);
    table.padTop(TOP_PADDING);

    label = new Label("", skin, "default");
    table.add(label);
    table.setVisible(false);

    entity.getEvents().addListener(FrozenCause.STATUS_CAUSE_EVENT, this::onCause);
    entity.getEvents().addListener(SpeedEffectComponent.FROZEN_START_EVENT, this::show);
    entity.getEvents().addListener(SpeedEffectComponent.FROZEN_END_EVENT, this::hide);
    entity.getEvents().addListener("death", this::hide);

    stage.addActor(table);
  }

  /** Remembers why the player is being stopped; a null cause is ignored. */
  private void onCause(String newCause) {
    if (newCause != null) {
      cause = newCause;
    }
  }

  private void show() {
    table.setVisible(true);
  }

  /** Hides the label and forgets the cause, so the next freeze starts from a clean slate. */
  private void hide() {
    table.setVisible(false);
    cause = null;
  }

  /** Refreshes the label text and colour from the remaining time; does nothing while hidden. */
  @Override
  public void update() {
    if (table == null || !table.isVisible()) {
      return;
    }
    label.setText(formatLabel(cause, speedEffect.getFrozenTicksRemaining()));
    label.setColor(FrozenTintComponent.colourFor(cause));
  }

  /**
   * Turns remaining ticks into the label text.
   *
   * @param cause "Frozen" or "Petrified"; any other value, or null, is shown as "Frozen"
   * @param ticks ticks remaining; a negative number is shown as zero
   * @return text such as "Frozen 1.8s", one decimal place, never below "0.0s"
   */
  static String formatLabel(String cause, int ticks) {
    String word = FrozenCause.PETRIFIED.equals(cause) ? FrozenCause.PETRIFIED : FrozenCause.FROZEN;
    float seconds = Math.max(ticks, 0) / (float) PetrifyEffectComponent.TICKS_PER_SECOND;
    return String.format(Locale.ENGLISH, "%s %.1fs", word, seconds);
  }

  @Override
  public void draw(SpriteBatch batch) {
    // Drawing is handled by the stage
  }

  /** Removes the table from the stage so no label is left behind. */
  @Override
  public void dispose() {
    super.dispose();
    if (table != null) {
      table.remove();
    }
  }
}

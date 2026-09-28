package com.csse3200.game.components.difficulty;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.csse3200.game.ui.UIComponent;


public class DifficultySelectDisplay extends UIComponent {
    private static final float Z_INDEX = 2f;

    // Same look as the main menu
    private static final Color PANEL_COLOR = new Color(0f, 0f, 0f, 0.5f);
    private static final Color SELECTED_BG = new Color(0.15f, 0.35f, 0.55f, 0.9f);
    private static final Color SELECTED_TEXT = Color.CYAN;
    private static final Color UNSELECTED_TEXT = Color.WHITE;
    private static final float LEFT_PANEL_WIDTH_FRACTION = 0.30f;
    private static final float MENU_ITEM_FONT_SCALE = 1.6f;

    static final String[] MENU_ITEMS = {"Easy", "Normal", "Hard", "Back"};
    static final String[] EVENTS = {"easy", "normal", "hard", "back"};
    private static final String[] DESCRIPTIONS = {
            "A gentler run. Enemies are weaker and go down faster.",
            "The standard experience, as the game is meant to be played.",
            "For experienced players. Enemies hit harder and take more hits to beat.",
            "Return to the main menu."
    };
    private static final int DEFAULT_INDEX = 1; // Normal

    Label[] buttons;
    Label descriptionLabel;
    int selectedIndex = DEFAULT_INDEX;

    @Override
    public void create() {
        super.create();
        addActors();
        entity.getEvents().addListener("navigateUp", this::navigateUp);
        entity.getEvents().addListener("navigateDown", this::navigateDown);
        entity.getEvents().addListener("confirmSelection", this::confirmSelection);
    }

    private void addActors() {
        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();
        float panelWidth = screenWidth * LEFT_PANEL_WIDTH_FRACTION;

        // Left panel: the options
        Table optionsPanel = new Table();
        optionsPanel.setBackground(skin.newDrawable("white", PANEL_COLOR));
        optionsPanel.center();

        buttons = new Label[MENU_ITEMS.length];
        for (int i = 0; i < MENU_ITEMS.length; i++) {
            Label label = createLabel(MENU_ITEMS[i], UNSELECTED_TEXT, MENU_ITEM_FONT_SCALE);
            buttons[i] = label;
            Table row = new Table();
            row.add(label).pad(6f, 15f, 6f, 15f).left().expandX();
            addRowInteraction(row, i);
            optionsPanel.add(row).width(panelWidth * 0.75f).padBottom(4f);
            optionsPanel.row();
        }

        // Right side: heading, description box, controls hint
        Label heading = createLabel("Choose Difficulty", Color.BLACK, 2f);

        descriptionLabel = createLabel("", Color.WHITE, 1.3f);
        descriptionLabel.setWrap(true);
        Table descriptionBox = new Table();
        descriptionBox.setBackground(skin.newDrawable("white", PANEL_COLOR));
        descriptionBox.add(descriptionLabel).width(screenWidth * 0.40f).pad(20f);

        Label hint = createLabel("Up/Down: select   Enter: confirm   Esc: back", Color.BLACK, 1f);

        Table rightSide = new Table();
        rightSide.add(heading).padBottom(30f).row();
        rightSide.add(descriptionBox).padBottom(20f).row();
        rightSide.add(hint);

        Table root = new Table();
        root.setFillParent(true);
        root.left().top();
        root.add(optionsPanel).width(panelWidth).height(screenHeight);
        root.add(rightSide).expand();
        stage.addActor(root);

        updateHighlight();
    }

    private Label createLabel(String text, Color colour, float scale) {
        Label label = new Label(text, skin);
        Label.LabelStyle style = new Label.LabelStyle(label.getStyle());
        style.fontColor = colour;
        label.setStyle(style);
        label.setFontScale(scale);
        return label;
    }

    private void addRowInteraction(Table row, int rowIndex) {
        row.addListener(
                new InputListener() {
                    @Override
                    public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                        selectedIndex = rowIndex;
                        updateHighlight();
                    }

                    @Override
                    public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                        selectedIndex = rowIndex;
                        confirmSelection();
                        return true;
                    }
                });
    }

    void navigateUp() {
        selectedIndex = (selectedIndex - 1 + buttons.length) % buttons.length;
        updateHighlight();
    }

    void navigateDown() {
        selectedIndex = (selectedIndex + 1) % buttons.length;
        updateHighlight();
    }

    void updateHighlight() {
        for (int i = 0; i < buttons.length; i++) {
            boolean selected = i == selectedIndex;
            buttons[i].getStyle().fontColor = selected ? SELECTED_TEXT : UNSELECTED_TEXT;
            Table row = (Table) buttons[i].getParent();
            row.setBackground(selected ? skin.newDrawable("white", SELECTED_BG) : null);
        }
        descriptionLabel.setText(DESCRIPTIONS[selectedIndex]);
    }

    private void confirmSelection() {
        entity.getEvents().trigger(EVENTS[selectedIndex]);
    }

    @Override
    public void draw(SpriteBatch batch) {
        // Drawn by the stage
    }

    @Override
    public float getZIndex() {
        return Z_INDEX;
    }
}
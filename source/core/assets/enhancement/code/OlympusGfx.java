package com.olympusrun.gfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.utils.Array;

/** Olympus Run — background, ambience & HUD helpers for the asset pack. Viewport 1280x720, world units = pixels. */
public final class OlympusGfx {

    public static Texture tex(String path) {
        Texture t = new Texture(Gdx.files.internal(path));
        t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        return t;
    }

    /** Vertical parallax for a vertical climb. Layer height must be VIEW_H + (WORLD_H - VIEW_H) * factor
     *  (the pack ships them pre-sized, see manifest.json). Camera bottom 0 = level bottom. */
    public static final class VLayer {
        final Texture t; final float f;
        public VLayer(String path, float factor) { t = tex(path); f = factor; }
        public void draw(SpriteBatch b, float camLeft, float camBottom) {
            b.draw(t, camLeft, camBottom * (1f - f));
        }
    }

    /** Seamless loop layer (wisps, rain, embers, fog): scrolls by camera * factor plus its own drift. */
    public static final class LoopLayer {
        final Texture t; final float f, driftX, driftY; float ox, oy;
        public LoopLayer(String path, float factor, float driftX, float driftY) {
            t = tex(path); t.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
            f = factor; this.driftX = driftX; this.driftY = driftY;
        }
        public void update(float dt) { ox += driftX * dt; oy += driftY * dt; }
        public void draw(SpriteBatch b, float camLeft, float camBottom, int vw, int vh) {
            int sx = (int) (camLeft * f + ox), sy = (int) (-(camBottom * f) - oy);   // texture y is down
            b.draw(t, camLeft, camBottom, vw, vh, sx, sy, vw, vh, false, false);
        }
    }

    /** Horizontal strip sheet → animation (frames laid left to right). */
    public static Animation<TextureRegion> strip(String path, int frames, float frameTime, Animation.PlayMode mode) {
        Texture t = tex(path);
        TextureRegion[] row = TextureRegion.split(t, t.getWidth() / frames, t.getHeight())[0];
        Animation<TextureRegion> a = new Animation<>(frameTime, new Array<>(row));
        a.setPlayMode(mode);
        return a;
    }

    /** Nine-patch from a plain PNG; split values (already scaled) are in ui/hud/ninepatch-splits.json.
     *  If you pack with TexturePacker, use the .9.png files instead — splits are read automatically. */
    public static NinePatch nine(String path, int split) {
        return new NinePatch(tex(path), split, split, split, split);
    }

    /** Additive light sprite (fx/light-*.png) — torches, lava, sun. */
    public static void drawLight(SpriteBatch b, Texture light, float cx, float cy, float size, Color c) {
        int src = b.getBlendSrcFunc(), dst = b.getBlendDstFunc();
        b.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        b.setColor(c); b.draw(light, cx - size / 2f, cy - size / 2f, size, size); b.setColor(Color.WHITE);
        b.setBlendFunction(src, dst);
    }

    /** Title card: kicker, title, ornament, subtitle — fade in 0.6s, hold 2.2s, fade out 0.8s. */
    public static final class TitleCard {
        final Texture card; float t = -1f;
        public TitleCard(String path) { card = tex(path); }
        public void show() { t = 0f; }
        public void draw(SpriteBatch b, float dt, float camLeft, float camBottom, int vw, int vh) {
            if (t < 0f) return; t += dt;
            float a = t < 0.6f ? t / 0.6f : t < 2.8f ? 1f : Math.max(0f, 1f - (t - 2.8f) / 0.8f);
            if (a <= 0f) { t = -1f; return; }
            b.setColor(1, 1, 1, a);
            b.draw(card, camLeft + (vw - card.getWidth()) / 2f, camBottom + vh * 0.62f - card.getHeight() / 2f);
            b.setColor(Color.WHITE);
        }
    }

    /* ------------------------------------------------------------------ usage (Level 2)
    VLayer sky   = new VLayer("backgrounds/lv2-olympus/0-sky.png",        0.10f);
    VLayer sea   = new VLayer("backgrounds/lv2-olympus/1-far-sea.png",    0.20f);
    VLayer range = new VLayer("backgrounds/lv2-olympus/2-ranges.png",     0.35f);
    VLayer banks = new VLayer("backgrounds/lv2-olympus/3-cloudbanks.png", 0.85f);
    LoopLayer wisps = new LoopLayer("backgrounds/lv2-olympus/4-wisps-loop.png", 1.15f, 8f, 0f);
    LoopLayer rain  = new LoopLayer("backgrounds/lv2-olympus/5-storm-rain-loop.png", 1.0f, -40f, -260f); // storm belt only
    Animation<TextureRegion> bolt = strip("animation/lv2/lightning-bolt-sheet-6f.png", 6, 0.05f, Animation.PlayMode.NORMAL);
    Animation<TextureRegion> bird = strip("animation/lv2/bird-flap-sheet-4f.png", 4, 0.12f, Animation.PlayMode.LOOP);
    TitleCard card = new TitleCard("ui/titles/lv2-olympus-card.png");

    render: sky, sea, range, sun-rays (additive, alpha 0.6 + 0.4*sin(t*0.8)), banks, drifting cloud-*.png sprites,
            birds, TILE MAP, player/enemies, wisps, rain (if in storm), vignette, HUD, title card.
    ------------------------------------------------------------------ */
    private OlympusGfx() {}
}

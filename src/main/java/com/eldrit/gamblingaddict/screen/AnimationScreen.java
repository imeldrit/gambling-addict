package com.eldrit.gamblingaddict.screen;

import com.eldrit.gamblingaddict.anim.AnimationQueue;
import com.eldrit.gamblingaddict.anim.GambleSession;
import com.eldrit.gamblingaddict.config.ModConfig;
import com.eldrit.gamblingaddict.render.RenderCompat;
import com.eldrit.gamblingaddict.util.SoundSuppressor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public abstract class AnimationScreen extends Screen {
    protected final GambleSession session;

    private GambleSession.@Nullable Outcome verdict = null;

    private boolean released = false;

    protected int ticks = 0;

    protected AnimationScreen(Component title, GambleSession session) {
        super(Minecraft.getInstance(), Minecraft.getInstance().font, title);
        this.session = session;
        SoundSuppressor.begin();
    }

    protected void revealNow() {
        outcome();
        SoundSuppressor.end();
    }

    protected GambleSession.Outcome outcome() {
        if (verdict == null) {
            verdict = session.resolve();
        }
        return verdict;
    }

    protected boolean decided() {
        return verdict != null;
    }

    protected boolean won() {
        if (verdict != null) {
            return verdict == GambleSession.Outcome.WIN;
        }
        return session.peek() == GambleSession.Outcome.WIN;
    }

    protected float time(float delta) {
        return (ticks + delta) * speedMultiplier();
    }

    protected float time() {
        return time(0.0f);
    }

    protected abstract float speedMultiplier();

    @Override
    public void tick() {
        super.tick();
        ticks++;
        onAnimationTick(time());
    }

    protected abstract void onAnimationTick(float t);

    protected void finishAndClose() {
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public void removed() {
        super.removed();
        SoundSuppressor.end();
        if (!released) {
            released = true;
            AnimationQueue.finish(session);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (ModConfig.get().clickToSkip) {
            finishAndClose();
            return true;
        }
        return super.mouseClicked(event, doubled);
    }

    protected void dimBackground(GuiGraphicsExtractor ctx) {
        float a = Math.max(0.2f, Math.min(1.0f, ModConfig.get().backgroundOpacity));
        ctx.fill(0, 0, this.width, this.height, RenderCompat.withAlpha(0x060609, a));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        dimBackground(ctx);
        renderAnimation(ctx, time(delta), delta);
    }

    protected abstract void renderAnimation(GuiGraphicsExtractor ctx, float t, float delta);

    protected static float clamp01(float v) {
        return v < 0.0f ? 0.0f : (v > 1.0f ? 1.0f : v);
    }

    protected static float easeOutCubic(float p) {
        float inv = 1.0f - clamp01(p);
        return 1.0f - inv * inv * inv;
    }

    protected static float easeInQuad(float p) {
        float c = clamp01(p);
        return c * c;
    }

    protected static float lerp(float a, float b, float p) {
        return a + (b - a) * p;
    }
}

package com.eldrit.gamblingaddict.tracker;

import com.eldrit.gamblingaddict.GamblingAddictClient;
import com.eldrit.gamblingaddict.config.ModConfig;
import com.eldrit.gamblingaddict.render.RenderCompat;
import com.eldrit.gamblingaddict.slayer.SlayerTracker;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public final class TrackerHud {
    private static final Identifier ID = Identifier.fromNamespaceAndPath(GamblingAddictClient.MOD_ID, "trackers");
    private static final int PAD = 4;
    private static final int LINE = 11;

    private TrackerHud() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            MobCapTracker.tick(client);
            BlazeAttunementTracker.tick(client);
        });
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            String plain = SlayerTracker.strip(message.getString());
            AbilityCooldownTracker.onChat(plain);
            if (!overlay) {
                RagnarockAxeTracker.onChat(plain);
            }
        });
        RagnarockAxeTracker.register();
        HudElementRegistry.addLast(ID, TrackerHud::render);
    }

    private static List<String> lines() {
        ModConfig cfg = ModConfig.get();
        List<String> lines = new ArrayList<>();
        if (cfg.mobCapCounter) {
            lines.add(MobCapTracker.line());
        }
        String attunement = BlazeAttunementTracker.line();
        if (attunement != null) {
            lines.add(attunement);
        }
        String cooldown = AbilityCooldownTracker.cooldownLine();
        if (cooldown != null) {
            lines.add(cooldown);
        }
        String buff = AbilityCooldownTracker.buffLine();
        if (buff != null) {
            lines.add(buff);
        }
        return lines;
    }

    private static void render(GuiGraphicsExtractor ctx, DeltaTracker delta) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.options.hideGui) {
            return;
        }
        Screen screen = client.screen;
        if (screen != null && !(screen instanceof net.minecraft.client.gui.screens.ChatScreen)) {
            return;
        }
        List<String> lines = lines();
        if (lines.isEmpty()) {
            return;
        }
        ModConfig cfg = ModConfig.get();
        int screenW = client.getWindow().getGuiScaledWidth();
        int screenH = client.getWindow().getGuiScaledHeight();

        int width = 0;
        for (String line : lines) {
            width = Math.max(width, client.font.width(line));
        }
        int height = lines.size() * LINE;
        int x = (int) (cfg.hudX * (screenW - width - PAD * 2));
        int y = (int) (cfg.hudY * (screenH - height - PAD * 2));

        ctx.fill(x, y, x + width + PAD * 2, y + height + PAD * 2, RenderCompat.withAlpha(0x000000, 0.45f));
        RenderCompat.outline(ctx, x, y, width + PAD * 2, height + PAD * 2, RenderCompat.withAlpha(0xFFFFFF, 0.12f));
        for (int i = 0; i < lines.size(); i++) {
            ctx.text(client.font, Component.literal(lines.get(i)), x + PAD, y + PAD + i * LINE, 0xFFFFFFFF, true);
        }
    }
}

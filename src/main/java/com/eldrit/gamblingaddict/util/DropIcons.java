package com.eldrit.gamblingaddict.util;

import com.eldrit.gamblingaddict.GamblingAddictClient;
import com.eldrit.gamblingaddict.loot.Drop;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class DropIcons {
    public static final String JUDGEMENT_CORE_TEXTURE = "ewogICJ0aW1lc3RhbXAiIDogMTY1ODkwNDIzNTQ3MywKICAicHJvZmlsZUlkIiA6ICIxNDU1MDNhNDRjZmI0NzcwYmM3NWNjMTRjYjUwMDE4NyIsCiAgInByb2ZpbGVOYW1lIiA6ICJMaWtlbHlFcmljIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzk1ZjM3MjZmNmJmYmM4MWYwNzAyNWYxZDZjYmVjN2Y4ZjdmYTdhYmRmM2EwYTg2YjgxOTdlMDkyMjE0ODYwYmEiCiAgICB9CiAgfQp9";
    public static final String PRIMORDIAL_EYE_TEXTURE = "ewogICJ0aW1lc3RhbXAiIDogMTczODI0MzI4OTc5MSwKICAicHJvZmlsZUlkIiA6ICJlMjc5NjliODYyNWY0NDg1YjkyNmM5NTBhMDljMWMwMSIsCiAgInByb2ZpbGVOYW1lIiA6ICJNaVp6YVhQIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2FhZDQ0YjRjNDYzNTY0OGQ1YmQyMTIxMGRjYjZiMzE0ZDQ4OWRjZDc1YmM3OTMyYzU2OTBlMTJkODg5ODJhMzQiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ==";
    public static final String SHRIVELED_WASP_TEXTURE = "ewogICJ0aW1lc3RhbXAiIDogMTczODU4NDU2NTIxNCwKICAicHJvZmlsZUlkIiA6ICI5YjhhN2NlMmJlYjI0NjdkYTJjZmU4MzQ1YTNjOTZkOCIsCiAgInByb2ZpbGVOYW1lIiA6ICJTdGFyR2FtZXJTaGFsb20iLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMWRkYzkwM2JhODBmNDQwMDBmZWNlZjdjY2NmMjdmYWU2ZjBkY2IyZDA5NGM1Njc1OTJiZjJjMWRiNGIyZTk3MCIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9";
    public static final String BRICK_RED_DYE_TEXTURE = "ewogICJ0aW1lc3RhbXAiIDogMTc0MTE2MDk0Njc4NSwKICAicHJvZmlsZUlkIiA6ICJlYTA4ZjhlZTdiOTg0YmFlYWM3N2JhYzk3ZWVkYzE4NSIsCiAgInByb2ZpbGVOYW1lIiA6ICJXYXlkZXJUTSIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9jZTExMzE5OGE1NTg5NDZhZTM2ZTg4MGRiOWM1MTg1NzIyMWIyY2JlYzA5ZjRmZjUxOTJlMTM4NWU4ZjhlYzJhIiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0=";
    public static final String WARDEN_HEART_TEXTURE = "ewogICJ0aW1lc3RhbXAiIDogMTYxMDQwMjM1OTkxMSwKICAicHJvZmlsZUlkIiA6ICJiYjdjY2E3MTA0MzQ0NDEyOGQzMDg5ZTEzYmRmYWI1OSIsCiAgInByb2ZpbGVOYW1lIiA6ICJsYXVyZW5jaW8wMSIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS8xOWEzYjVmYWIwMWRmYWJmMWY4YTgxNDQ0MGE4Y2U4MGRlMTA5YjEzNWE1MWFlZjIyNWM5ZWU5OGZlNDQ3ZTAiCiAgICB9CiAgfQp9";
    public static final String BEHEADED_HORROR_TEXTURE = "ewogICJ0aW1lc3RhbXAiIDogMTYyMjIzNjA5NzQ0NCwKICAicHJvZmlsZUlkIiA6ICI5ZDIyZGRhOTVmZGI0MjFmOGZhNjAzNTI1YThkZmE4ZCIsCiAgInByb2ZpbGVOYW1lIiA6ICJTYWZlRHJpZnQ0OCIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9kYmFkOTllZDNjODIwYjc5NzgxOTBhZDA4YTkzNGE2OGRmYTkwZDk5ODY4MjVkYTFjOTdmNmYyMWY0OWFkNjI2IgogICAgfQogIH0KfQ==";
    public static final String CELESTE_DYE_TEXTURE = "ewogICJ0aW1lc3RhbXAiIDogMTc0MTE2MDk5NzIyMywKICAicHJvZmlsZUlkIiA6ICJjZjc4YzFkZjE3ZTI0Y2Q5YTIxYmU4NWQ0NDk5ZWE4ZiIsCiAgInByb2ZpbGVOYW1lIiA6ICJNYXR0c0FybW9yU3RhbmRzIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzgwMjg5MTgxMmE1MmVkNmVkNDM3NjcyYjNlNmIyMTQ0OTViOTY1MTgwMGFlZjg3NTQ0YWM1YmQxOTM3NTgyOTciLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ==";

    public static final String TIKI_MASK_TEXTURE = "ewogICJ0aW1lc3RhbXAiIDogMTczOTYzMjQ0NTQ2MywKICAicHJvZmlsZUlkIiA6ICJiNjZiYTQ5ZWFiZWM0YmY5OWVhOWY5NDBjMTcxYTkzMyIsCiAgInByb2ZpbGVOYW1lIiA6ICJNYXRlaUxpa2VzTWluaW4iLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzVmZDZiOWE1OWVjNWI5N2RiOGJkYzE1OGZiZDVmOTFlZjdiMzE3Yjg1OWZjZWJlNmQwOWU3YmQ4MGVhY2E5ZCIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9";
    public static final String TROUBLED_BUBBLE_TEXTURE = "ewogICJ0aW1lc3RhbXAiIDogMTc0MDY3NTY1MTY4MCwKICAicHJvZmlsZUlkIiA6ICJmYjZkM2E5Zjk3MWY0ZTdlYmQ0MjE2Yjk0MjE5NDA3NCIsCiAgInByb2ZpbGVOYW1lIiA6ICJtYXJjaXhkZCIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9iZGZjYzUzMGFkNGY2NDVhMjAxYWJjMDU5MTJmMTk0ODYyNjExYzMzNzM5YTA4MjZhOGJiOWFkMGQ5Yjk4Mzc1IiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0=";
    public static final String AQUAMARINE_DYE_TEXTURE = "ewogICJ0aW1lc3RhbXAiIDogMTc0MTE2MDg4OTYzMiwKICAicHJvZmlsZUlkIiA6ICI1ZTEwYjc5MTY2ZTA0MTNkYjI2MThkMzE3MTc2M2Y4NCIsCiAgInByb2ZpbGVOYW1lIiA6ICJPTUZfQmxvY2tCdXN0ZXIiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDcxOTE1NjYzYzI3ZmNjNGViNTBiMzM0YWEwM2ZmZjE3NWVkMDM2M2Y0ZWYzZTAyOGI5Y2M3NzFjODFmNTQ1ZiIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9";
    public static final String RADIOACTIVE_VIAL_TEXTURE = "ewogICJ0aW1lc3RhbXAiIDogMTYxMDA0OTczMzc3OSwKICAicHJvZmlsZUlkIiA6ICIxOTI1MjFiNGVmZGI0MjVjODkzMWYwMmE4NDk2ZTExYiIsCiAgInByb2ZpbGVOYW1lIiA6ICJTZXJpYWxpemFibGUiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOWI1NzRjMDNiMWM4ZjUyNTM1Zjk2YTIwYTQxNzYwMjBiYjI4MGUwYWY1MmZjMTI4NWQzZWNiZDM1MTcyODBjNCIKICAgIH0KICB9Cn0=";
    public static final String CARMINE_DYE_TEXTURE = "ewogICJ0aW1lc3RhbXAiIDogMTc0MTE2MDk2ODI0MiwKICAicHJvZmlsZUlkIiA6ICJiMzMwZWE4OWE3MTY0OWZjYTMwZmI5ZjU5MTQ5ZjNjMCIsCiAgInByb2ZpbGVOYW1lIiA6ICJUQUxLX0FMT1RfIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzgxNTBiNDJlNjQ5NzEzNzIxN2Q1ZDhmNTY1MDNmOThkOGY1NDljYjAzNGZiZWFlNmZkZDU0MzE1NzA1MThlMzgiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ==";
    public static final String CHAIN_END_TIMES_TEXTURE = "ewogICJ0aW1lc3RhbXAiIDogMTczOTY2NDM4ODI2NCwKICAicHJvZmlsZUlkIiA6ICJlZGUyYzdhMGFjNjM0MTNiYjA5ZDNmMGJlZTllYzhlYyIsCiAgInByb2ZpbGVOYW1lIiA6ICJ0aGVEZXZKYWRlIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzkzZDliYzYyOTRlY2I4NWQ5YzRhZjY2MTM2MDI3ZmFmNjc0MTNmMWRiYWJiZTFiNmRmZWUyODkwODNmYzIwNDkiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ==";
    public static final String BURNT_TEXTS_TEXTURE = "ewogICJ0aW1lc3RhbXAiIDogMTczOTY2NjM2MzI2MSwKICAicHJvZmlsZUlkIiA6ICI2MTU1NTMyOTY2OWM0ZDA5YmFiOGJlNDNkYWUwYTRjMyIsCiAgInByb2ZpbGVOYW1lIiA6ICJmaTAxNSIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS83OGJkMTg4OWJmOGE2MzVlYzcxZmMwNWIxZWQ1NDg3NjM3NzhlM2QzNzE5MjZkMzlhNjJjMjlkMzUwYzQ4YjQ5IiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0=";

    private DropIcons() {
    }

    public static ItemStack of(Drop drop) {
        return switch (drop) {
            case JUDGEMENT_CORE ->
                    icon(JUDGEMENT_CORE_TEXTURE, drop.displayName(), ChatFormatting.LIGHT_PURPLE, Items.HEART_OF_THE_SEA);
            case PRIMORDIAL_EYE ->
                    icon(PRIMORDIAL_EYE_TEXTURE, drop.displayName(), ChatFormatting.LIGHT_PURPLE, Items.ENDER_EYE);
            case WARDEN_HEART ->
                    icon(WARDEN_HEART_TEXTURE, drop.displayName(), ChatFormatting.GOLD, Items.NETHER_STAR);
            case BEHEADED_HORROR ->
                    icon(BEHEADED_HORROR_TEXTURE, drop.displayName(), ChatFormatting.DARK_PURPLE, Items.ZOMBIE_HEAD);
            case CELESTE_DYE ->
                    icon(CELESTE_DYE_TEXTURE, drop.displayName(), ChatFormatting.AQUA, Items.LIGHT_BLUE_DYE);
            case TIKI_MASK ->
                    icon(TIKI_MASK_TEXTURE, drop.displayName(), ChatFormatting.GOLD, Items.JUNGLE_LOG);
            case TROUBLED_BUBBLE ->
                    icon(TROUBLED_BUBBLE_TEXTURE, drop.displayName(), ChatFormatting.GOLD, Items.HEART_OF_THE_SEA);
            case AQUAMARINE_DYE ->
                    icon(AQUAMARINE_DYE_TEXTURE, drop.displayName(), ChatFormatting.AQUA, Items.CYAN_DYE);
            case RADIOACTIVE_VIAL ->
                    icon(RADIOACTIVE_VIAL_TEXTURE, drop.displayName(), ChatFormatting.LIGHT_PURPLE, Items.EXPERIENCE_BOTTLE);
            case CARMINE_DYE ->
                    icon(CARMINE_DYE_TEXTURE, drop.displayName(), ChatFormatting.DARK_RED, Items.RED_DYE);
            case CHAIN_OF_THE_END_TIMES ->
                    icon(CHAIN_END_TIMES_TEXTURE, drop.displayName(), ChatFormatting.GOLD, Items.IRON_CHAIN);
            case BURNT_TEXTS ->
                    icon(BURNT_TEXTS_TEXTURE, drop.displayName(), ChatFormatting.GOLD, Items.WRITTEN_BOOK);
            case SCYTHE_BLADE -> named(new ItemStack(Items.DIAMOND), drop.displayName(), ChatFormatting.GOLD);
            case REVENANT_CATALYST -> named(new ItemStack(Items.PAPER), drop.displayName(), ChatFormatting.DARK_PURPLE);
            case REVENANT_VISCERA -> named(new ItemStack(Items.COOKED_PORKCHOP), drop.displayName(), ChatFormatting.BLUE);
            case OVERFLUX_CAPACITOR -> named(new ItemStack(Items.QUARTZ), drop.displayName(), ChatFormatting.DARK_PURPLE);
            case GRIZZLY_BAIT -> named(new ItemStack(Items.SALMON), drop.displayName(), ChatFormatting.BLUE);
            case RED_CLAW_EGG -> named(new ItemStack(Items.PAPER), drop.displayName(), ChatFormatting.DARK_PURPLE);
            case BOBBIN_SCRIPTURES -> named(new ItemStack(Items.PAPER), drop.displayName(), ChatFormatting.BLUE);
            case MAGMA_LORD_FRAGMENT -> named(new ItemStack(Items.PAPER), drop.displayName(), ChatFormatting.GOLD);
            case ATTRIBUTE_SHARD -> named(new ItemStack(Items.PRISMARINE_SHARD), drop.displayName(), ChatFormatting.GREEN);
            case FLASH_BOOK -> named(new ItemStack(Items.ENCHANTED_BOOK), drop.displayName(), ChatFormatting.BLUE);
            case BRIMSTONE_HANDLE -> named(new ItemStack(Items.STICK), drop.displayName(), ChatFormatting.DARK_PURPLE);
        };
    }

    public static ItemStack of(Drop drop, boolean foil) {
        ItemStack stack = of(drop);
        return foil ? foil(stack) : stack;
    }

    public static ItemStack foil(ItemStack stack) {
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, Boolean.TRUE);
        return stack;
    }

    public static ItemStack shriveledWasp() {
        return icon(SHRIVELED_WASP_TEXTURE, "Shriveled Wasp", ChatFormatting.BLUE, Items.FERMENTED_SPIDER_EYE);
    }

    public static ItemStack brickRedDye() {
        return icon(BRICK_RED_DYE_TEXTURE, "Brick Red Dye", ChatFormatting.DARK_PURPLE, Items.RED_DYE);
    }

    public static ItemStack silverMagmafish() {
        return named(new ItemStack(Items.PAPER), "Silver Magmafish", ChatFormatting.DARK_PURPLE);
    }

    public static ItemStack enchantedJungleWood() {
        return foil(named(new ItemStack(Items.JUNGLE_LOG), "Enchanted Jungle Wood", ChatFormatting.GREEN));
    }

    public static ItemStack enchantedCoal() {
        return foil(named(new ItemStack(Items.COAL), "Enchanted Coal", ChatFormatting.GREEN));
    }

    public static ItemStack plainBook() {
        return named(new ItemStack(Items.BOOK), "Book", ChatFormatting.WHITE);
    }

    public static ItemStack chest() {
        return new ItemStack(Items.CHEST);
    }

    private static ItemStack icon(String texture, String name, ChatFormatting colour, Item fallback) {
        if (texture.isEmpty()) {
            return named(new ItemStack(fallback), name, colour);
        }
        return skull(texture, name, colour, new ItemStack(fallback));
    }

    private static ItemStack named(ItemStack stack, String name, ChatFormatting colour) {
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name).withStyle(colour));
        return stack;
    }

    private static ItemStack skull(String textureValue, String name, ChatFormatting colour, ItemStack fallback) {
        try {
            ItemStack stack = new ItemStack(Items.PLAYER_HEAD);

            Multimap<String, Property> backing = ArrayListMultimap.create();
            backing.put("textures", new Property("textures", textureValue));
            PropertyMap properties = new PropertyMap(backing);

            UUID stableId = UUID.nameUUIDFromBytes(textureValue.getBytes(StandardCharsets.UTF_8));
            GameProfile profile = new GameProfile(stableId, "gamblingaddict", properties);
            stack.set(DataComponents.PROFILE, ResolvableProfile.createResolved(profile));

            return named(stack, name, colour);
        } catch (Throwable t) {
            GamblingAddictClient.LOGGER.warn(
                    "[GamblingAddict] could not build skull icon for {}, using fallback item", name, t);
            return named(fallback, name, colour);
        }
    }
}

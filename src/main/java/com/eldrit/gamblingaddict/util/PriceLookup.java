package com.eldrit.gamblingaddict.util;

import com.eldrit.gamblingaddict.GamblingAddictClient;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class PriceLookup {
    private static final String ENDPOINT = "https://sky.coflnet.com/api/item/price/%s/bin";

    private static final long TTL_MS = 10 * 60 * 1000L;

    public static final String ID_JUDGEMENT_CORE = "JUDGEMENT_CORE";
    public static final String ID_PRIMORDIAL_EYE = "PRIMORDIAL_EYE";
    public static final String ID_WARDEN_HEART = "WARDEN_HEART";

    private record Entry(long price, long fetchedAt) {
        boolean fresh() {
            return System.currentTimeMillis() - fetchedAt < TTL_MS;
        }
    }

    private static final Map<String, Entry> CACHE = new ConcurrentHashMap<>();
    private static final Set<String> IN_FLIGHT = ConcurrentHashMap.newKeySet();

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private PriceLookup() {
    }

    public static void prefetch(String itemId) {
        if (itemId == null || itemId.isEmpty()) {
            return;
        }
        Entry cached = CACHE.get(itemId);
        if (cached != null && cached.fresh()) {
            return;
        }
        if (!IN_FLIGHT.add(itemId)) {
            return;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format(ENDPOINT, itemId)))
                .timeout(Duration.ofSeconds(8))
                .header("User-Agent", "GamblingAddict/1.1 (Fabric client mod)")
                .GET()
                .build();

        CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() != 200) {
                        cacheUnknown(itemId, "HTTP " + response.statusCode());
                        return;
                    }
                    JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                    if (!json.has("lowest") || json.get("lowest").isJsonNull()) {
                        cacheUnknown(itemId, "no 'lowest' field");
                        return;
                    }
                    long lowest = json.get("lowest").getAsLong();
                    CACHE.put(itemId, new Entry(lowest, System.currentTimeMillis()));
                    GamblingAddictClient.LOGGER.info("[GamblingAddict] {} lowest BIN = {}", itemId, lowest);
                })
                .exceptionally(t -> {
                    cacheUnknown(itemId, t.toString());
                    return null;
                })
                .whenComplete((v, t) -> IN_FLIGHT.remove(itemId));
    }

    private static void cacheUnknown(String itemId, String why) {
        GamblingAddictClient.LOGGER.info("[GamblingAddict] price lookup for {} unavailable ({})", itemId, why);
        CACHE.put(itemId, new Entry(-1L, System.currentTimeMillis()));
    }

    public static OptionalLong cached(String itemId) {
        if (itemId == null || itemId.isEmpty()) {
            return OptionalLong.empty();
        }
        Entry entry = CACHE.get(itemId);
        if (entry == null || entry.price() < 0 || !entry.fresh()) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(entry.price());
    }

    public static String formatCoins(long coins) {
        if (coins >= 1_000_000_000L) {
            return String.format(Locale.ROOT, "%.2fB", coins / 1_000_000_000.0);
        }
        if (coins >= 1_000_000L) {
            return String.format(Locale.ROOT, "%.1fM", coins / 1_000_000.0);
        }
        if (coins >= 1_000L) {
            return String.format(Locale.ROOT, "%.1fK", coins / 1_000.0);
        }
        return Long.toString(coins);
    }

    public static String priceSuffix(String itemId) {
        OptionalLong price = cached(itemId);
        return price.isPresent() ? " (+" + formatCoins(price.getAsLong()) + " Coins)" : "";
    }
}

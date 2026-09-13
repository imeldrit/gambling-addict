package com.eldrit.gamblingaddict.config.gui;

import java.util.ArrayList;
import java.util.List;

public class ConfigCategory {
    public final String name;
    public final String blurb;
    public final List<ConfigEntry> entries = new ArrayList<>();

    public ConfigCategory(String name, String blurb) {
        this.name = name;
        this.blurb = blurb;
    }

    public ConfigCategory add(ConfigEntry entry) {
        entries.add(entry);
        return this;
    }
}

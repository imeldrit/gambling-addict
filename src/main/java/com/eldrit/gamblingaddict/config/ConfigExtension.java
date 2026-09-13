package com.eldrit.gamblingaddict.config;

import com.eldrit.gamblingaddict.config.gui.ConfigCategory;

import java.util.List;

public interface ConfigExtension {
    List<ConfigCategory> categories();

    default void save() {
    }
}

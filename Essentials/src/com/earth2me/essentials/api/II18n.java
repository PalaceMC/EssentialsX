package com.earth2me.essentials.api;

import java.util.Locale;

@SuppressWarnings("unused")
public interface II18n {
    /**
     * Gets the current locale setting
     *
     * @return the current locale, if not set it will return the default locale
     */
    Locale getCurrentLocale();
}

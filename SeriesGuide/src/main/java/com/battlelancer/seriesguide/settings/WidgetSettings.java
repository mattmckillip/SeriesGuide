/*
 * Copyright 2014 Uwe Trottmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.battlelancer.seriesguide.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v4.content.ContextCompat;
import com.battlelancer.seriesguide.R;

/**
 * Access some widget related settings values.
 */
public class WidgetSettings {

    public interface Type {
        int UPCOMING = 0;
        int RECENT = 1;
        int SHOWS = 2;
    }

    public static final String SETTINGS_FILE = "ListWidgetPreferences";

    public static final String KEY_PREFIX_WIDGET_BACKGROUND_OPACITY = "background_color_";

    public static final String KEY_PREFIX_WIDGET_THEME = "theme_";

    public static final String KEY_PREFIX_WIDGET_LISTTYPE = "type_";

    public static final String KEY_PREFIX_WIDGET_HIDE_WATCHED = "unwatched_";

    public static final String KEY_PREFIX_WIDGET_ONLY_FAVORITES = "only_favorites_";

    public static final String KEY_PREFIX_WIDGET_SHOWS_SORT_ORDER = "shows_order_";

    public static final String DEFAULT_WIDGET_BACKGROUND_OPACITY = "100";
    private static final int DEFAULT_WIDGET_BACKGROUND_OPACITY_INT = 100;

    /**
     * Returns the type of episodes that the widget should display.
     *
     * @return One of {@link com.battlelancer.seriesguide.settings.WidgetSettings.Type}.
     */
    public static int getWidgetListType(Context context, int appWidgetId) {
        int type = Type.UPCOMING;
        try {
            type = Integer.parseInt(context.getSharedPreferences(SETTINGS_FILE, 0)
                    .getString(KEY_PREFIX_WIDGET_LISTTYPE + appWidgetId, "0"));
        } catch (NumberFormatException ignored) {
        }

        return type;
    }

    /**
     * Returns the sort order of shows. Should be used when the widget is set to the shows type.
     *
     * @return A {@link com.battlelancer.seriesguide.settings.ShowsDistillationSettings.ShowsSortOrder}
     * id.
     */
    public static int getWidgetShowsSortOrderId(Context context, int appWidgetId) {
        int sortOrder = 0;
        try {
            sortOrder = Integer.parseInt(context.getSharedPreferences(SETTINGS_FILE, 0)
                    .getString(KEY_PREFIX_WIDGET_SHOWS_SORT_ORDER + appWidgetId, "0"));
        } catch (NumberFormatException ignored) {
        }

        if (sortOrder == 1) {
            return ShowsDistillationSettings.ShowsSortOrder.TITLE_ID;
        } else {
            return ShowsDistillationSettings.ShowsSortOrder.EPISODE_REVERSE_ID;
        }
    }

    /**
     * Returns if this widget should not show watched episodes.
     */
    public static boolean isHidingWatchedEpisodes(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(SETTINGS_FILE, 0);
        return prefs.getBoolean(KEY_PREFIX_WIDGET_HIDE_WATCHED + appWidgetId, false);
    }

    /**
     * Returns if this widget should only show episodes of favorited shows.
     */
    public static boolean isOnlyFavoriteShows(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(SETTINGS_FILE, 0);
        return prefs.getBoolean(KEY_PREFIX_WIDGET_ONLY_FAVORITES + appWidgetId, false);
    }

    /**
     * Returns if this widget should use a light theme instead of the default one.
     */
    public static boolean isLightTheme(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(SETTINGS_FILE, 0);

        boolean isLightTheme = false;
        try {
            isLightTheme =
                    Integer.parseInt(prefs.getString(KEY_PREFIX_WIDGET_THEME + appWidgetId, "0"))
                            == 1;
        } catch (NumberFormatException ignored) {
        }
        return isLightTheme;
    }

    /**
     * Returns if this widget should use a completely dark theme (header is not colored) instead of
     * the regular one.
     */
    public static boolean isDarkTheme(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(SETTINGS_FILE, 0);

        boolean isDarkTheme = false;
        try {
            isDarkTheme =
                    Integer.parseInt(prefs.getString(KEY_PREFIX_WIDGET_THEME + appWidgetId, "0"))
                            == 2;
        } catch (NumberFormatException ignored) {
        }
        return isDarkTheme;
    }

    /**
     * Calculates the background color for this widget based on user preference.
     *
     * @param lightBackground If true, will return white with alpha. Otherwise black with alpha. See
     * {@link #isLightTheme}.
     */
    public static int getWidgetBackgroundColor(Context context, int appWidgetId,
            boolean lightBackground) {
        int opacity = DEFAULT_WIDGET_BACKGROUND_OPACITY_INT;
        try {
            opacity = Integer.parseInt(context.getSharedPreferences(SETTINGS_FILE, 0)
                    .getString(KEY_PREFIX_WIDGET_BACKGROUND_OPACITY + appWidgetId,
                            DEFAULT_WIDGET_BACKGROUND_OPACITY));
        } catch (NumberFormatException ignored) {
        }

        int baseColor = ContextCompat.getColor(context,
                lightBackground ? R.color.grey_50 : R.color.grey_850);
        // strip alpha from base color
        baseColor = baseColor & 0xFFFFFF;
        // add new alpha
        int alpha = (opacity * 255 / 100) << 24;
        return alpha | baseColor;
    }
}


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

package com.battlelancer.seriesguide.dashclock;

import android.content.Intent;
import android.database.Cursor;
import android.text.TextUtils;
import android.text.format.DateUtils;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.adapters.CalendarAdapter;
import com.battlelancer.seriesguide.settings.DashClockSettings;
import com.battlelancer.seriesguide.ui.ShowsActivity;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.TextTools;
import com.battlelancer.seriesguide.util.TimeTools;
import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;
import java.util.Date;

public class UpcomingEpisodeExtension extends DashClockExtension {

    @Override
    protected void onInitialize(boolean isReconnect) {
        super.onInitialize(isReconnect);
        setUpdateWhenScreenOn(true);
    }

    @Override
    protected void onUpdateData(int arg0) {
        final Cursor upcomingEpisodes = DBUtils.getUpcomingEpisodes(getApplicationContext(), false,
                true);
        final long customCurrentTime = TimeTools.getCurrentTime(getApplicationContext());
        int hourThreshold = DashClockSettings.getUpcomingTreshold(getApplicationContext());
        long latestTimeToInclude = customCurrentTime + hourThreshold * DateUtils.HOUR_IN_MILLIS;

        // Ensure there are episodes to show
        if (upcomingEpisodes != null) {
            if (upcomingEpisodes.moveToFirst()) {

                // Ensure those episodes are within the user set time frame
                long releaseTime = upcomingEpisodes
                        .getLong(CalendarAdapter.Query.RELEASE_TIME_MS);
                if (releaseTime <= latestTimeToInclude) {
                    // build our DashClock panel

                    // title and episode of first show, like 'Title 1x01'
                    String expandedTitle = TextTools.getShowWithEpisodeNumber(
                            getApplicationContext(),
                            upcomingEpisodes.getString(CalendarAdapter.Query.SHOW_TITLE),
                            upcomingEpisodes.getInt(CalendarAdapter.Query.NUMBER),
                            upcomingEpisodes.getInt(CalendarAdapter.Query.SEASON)
                    );

                    // get the actual release time
                    Date actualRelease = TimeTools.applyUserOffset(this, releaseTime);
                    String absoluteTime = TimeTools.formatToLocalTime(this, actualRelease);
                    String releaseDay = TimeTools.formatToLocalDay(actualRelease);

                    // time and network, e.g. 'Mon 10:00, Network'
                    StringBuilder expandedBody = new StringBuilder();
                    if (!DateUtils.isToday(actualRelease.getTime())) {
                        expandedBody.append(releaseDay).append(" ");
                    }
                    expandedBody.append(absoluteTime);
                    String network = upcomingEpisodes
                            .getString(CalendarAdapter.Query.SHOW_NETWORK);
                    if (!TextUtils.isEmpty(network)) {
                        expandedBody.append(" — ").append(network);
                    }

                    // more than one episode at this time? Append e.g. '3 more'
                    int additionalEpisodes = 0;
                    while (upcomingEpisodes.moveToNext()
                            && releaseTime == upcomingEpisodes
                            .getLong(CalendarAdapter.Query.RELEASE_TIME_MS)) {
                        additionalEpisodes++;
                    }
                    if (additionalEpisodes > 0) {
                        expandedBody.append("\n");
                        expandedBody.append(getString(R.string.more, additionalEpisodes));
                    }

                    publishUpdate(new ExtensionData()
                            .visible(true)
                            .icon(R.drawable.ic_notification)
                            // 'Fri\n15:00'
                            .status(releaseDay + "\n" + absoluteTime)
                            .expandedTitle(expandedTitle)
                            .expandedBody(expandedBody.toString())
                            .clickIntent(
                                    new Intent(getApplicationContext(),
                                            ShowsActivity.class)
                                            .putExtra(
                                                    ShowsActivity.InitBundle.SELECTED_TAB,
                                                    ShowsActivity.InitBundle.INDEX_TAB_UPCOMING)));
                    upcomingEpisodes.close();
                    return;
                }
            }
            upcomingEpisodes.close();
        }

        // nothing to show
        publishUpdate(null);
    }
}

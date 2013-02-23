package org.solovyev.android.messenger.longpoll;

import org.jetbrains.annotations.Nullable;

/**
 * User: serso
 * Date: 6/24/12
 * Time: 12:09 AM
 */
public interface RealmLongPollService {

    @Nullable
    Object startLongPolling();


    @Nullable
    LongPollResult waitForResult(@Nullable Object longPollingData);
}
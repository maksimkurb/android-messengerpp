package org.solovyev.android.messenger.users;

import android.content.Context;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.solovyev.android.list.ListItemArrayAdapter;
import org.solovyev.android.messenger.AbstractAsyncLoader;
import org.solovyev.android.messenger.AbstractMessengerListItemAdapter;
import org.solovyev.android.messenger.MessengerApplication;

import java.util.Comparator;
import java.util.List;

/**
* User: serso
* Date: 6/2/12
* Time: 3:12 PM
*/
class ContactsAsyncLoader extends AbstractAsyncLoader<User, ContactListItem> {

    ContactsAsyncLoader(@NotNull User user, @NotNull Context context, @NotNull ListItemArrayAdapter<ContactListItem> adapter, @Nullable Runnable onPostExecute) {
        super(user, context, adapter, onPostExecute);
    }

    @NotNull
    protected List<User> getElements(@NotNull Context context) {
        return MessengerApplication.getServiceLocator().getUserService().getUserContacts(getUser().getId(), context);
    }

    @Override
    protected Comparator<? super ContactListItem> getComparator() {
        return AbstractMessengerListItemAdapter.ListItemComparator.getInstance();
    }

    @NotNull
    @Override
    protected ContactListItem createListItem(@NotNull User contact) {
        return new ContactListItem(getUser(), contact);
    }
}
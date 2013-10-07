package org.solovyev.android.messenger;

import android.app.Application;
import com.google.inject.Inject;
import org.joda.time.DateTime;
import org.solovyev.android.messenger.accounts.*;
import org.solovyev.android.messenger.chats.ApiChat;
import org.solovyev.android.messenger.chats.ChatService;
import org.solovyev.android.messenger.entities.Entities;
import org.solovyev.android.messenger.entities.Entity;
import org.solovyev.android.messenger.messages.ChatMessage;
import org.solovyev.android.messenger.messages.ChatMessageImpl;
import org.solovyev.android.messenger.messages.ChatMessageService;
import org.solovyev.android.messenger.messages.LiteChatMessageImpl;
import org.solovyev.android.messenger.realms.TestRealm;
import org.solovyev.android.messenger.security.InvalidCredentialsException;
import org.solovyev.android.messenger.users.MutableUser;
import org.solovyev.android.messenger.users.User;
import org.solovyev.android.messenger.users.UserService;
import org.solovyev.android.properties.MutableAProperties;
import org.solovyev.common.text.Strings;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.solovyev.android.messenger.chats.Chats.newPrivateApiChat;
import static org.solovyev.android.messenger.entities.Entities.newEntity;
import static org.solovyev.android.messenger.messages.Messages.newLiteMessage;
import static org.solovyev.android.messenger.messages.Messages.newMessage;
import static org.solovyev.android.messenger.users.User.*;
import static org.solovyev.android.messenger.users.Users.newEmptyUser;

public abstract class DefaultMessengerTest extends AbstractMessengerTest {

	private static final int ACCOUNT_1_USER_COUNT = 3;
	private static final int ACCOUNT_2_USER_COUNT = 20;
	private static final int ACCOUNT_3_USER_COUNT = 200;

	@Nonnull
	@Inject
	private AccountService accountService;

	@Nonnull
	@Inject
	private UserService userService;

	@Nonnull
	@Inject
	private ChatService chatService;

	@Nonnull
	@Inject
	private ChatMessageService messageService;

	@Nonnull
	@Inject
	private TestRealm realm;

	@Nonnull
	private final List<AccountData> accountDataList = new ArrayList<AccountData>();

	@Nonnull
	protected AbstractTestMessengerModule newModule(@Nonnull Application application) {
		return new DefaultTestMessengerModule(application);
	}

	protected void populateDatabase() throws Exception {
		accountDataList.add(createAccountData(0, ACCOUNT_1_USER_COUNT));
		accountDataList.add(createAccountData(1, ACCOUNT_2_USER_COUNT));
		accountDataList.add(createAccountData(2, ACCOUNT_3_USER_COUNT));
	}

	@Nonnull
	private AccountData createAccountData(int index, int count) throws AccountException, InvalidCredentialsException, AccountAlreadyExistsException {
		final AccountData result = new AccountData(accountService.saveAccount(new TestAccountBuilder(realm, new TestAccountConfiguration("test_" + index, index), null)));
		result.users.addAll(addUsers(result.account, count));
		final User user = result.account.getUser();
		result.users.add(0, user);
		for (User contact : result.getContacts()) {
			final List<ChatMessage> messages = new ArrayList<ChatMessage>();
			for(int i = 0; i < 10; i++) {
				messages.add(generateMessage(i, user, contact, result.account));
			}
			result.chats.add(newPrivateApiChat(chatService.getPrivateChatId(user.getEntity(), contact.getEntity()), Arrays.asList(user, contact), messages));
		}
		chatService.mergeUserChats(user.getEntity(), result.chats);
		return result;
	}

	private ChatMessageImpl generateMessage(int i, @Nonnull User user, @Nonnull User contact, @Nonnull TestAccount account) {
		final LiteChatMessageImpl liteMessage = newLiteMessage(Entities.generateEntity(account));

		if (i % 2 == 0) {
			liteMessage.setAuthor(user.getEntity());
			liteMessage.setRecipient(contact.getEntity());
		} else {
			liteMessage.setAuthor(contact.getEntity());
			liteMessage.setRecipient(user.getEntity());
		}

		liteMessage.setBody(Strings.generateRandomString(10));
		liteMessage.setSendDate(new DateTime(0).plusMinutes(i));
		return newMessage(liteMessage, false);
	}

	@Nonnull
	private List<User> addUsers(@Nonnull Account account, int count) {
		final List<User> contacts = new ArrayList<User>();
		for(int i = 0; i < count; i++) {
			contacts.add(getContactForAccount(account, i));
		}
		userService.mergeUserContacts(account.getUser().getEntity(), contacts, false, false);
		return contacts;
	}

	@Nonnull
	protected User getContactForAccount(@Nonnull Account account, int i) {
		final MutableUser user = newEmptyUser(getEntityForContact(account, i));
		final MutableAProperties properties = user.getProperties();
		properties.setProperty(PROPERTY_LAST_NAME, "last_name_" + i);
		properties.setProperty(PROPERTY_FIRST_NAME, "first_name_" + i);
		properties.setProperty(PROPERTY_ONLINE, String.valueOf(i % 2));
		properties.setProperty(PROPERTY_PHONE, "phone_" + i);
		return user;
	}

	@Nonnull
	protected Entity getEntityForContact(@Nonnull Account account, int i) {
		return newEntity(account.getId(), String.valueOf(i));
	}

	@Nonnull
	public TestAccount getAccount1() {
		return accountDataList.get(0).account;
	}

	@Nonnull
	public TestAccount getAccount2() {
		return accountDataList.get(1).account;
	}

	@Nonnull
	public TestAccount getAccount3() {
		return accountDataList.get(2).account;
	}

	@Nonnull
	public List<User> getUsers1() {
		return accountDataList.get(0).users;
	}

	@Nonnull
	public List<User> getUsers2() {
		return accountDataList.get(1).users;
	}

	@Nonnull
	public List<User> getUsers3() {
		return accountDataList.get(2).users;
	}

	@Nonnull
	protected AccountService getAccountService() {
		return accountService;
	}

	@Nonnull
	public AccountData getAccountData1() {
		return accountDataList.get(0);
	}

	@Nonnull
	public AccountData getAccountData2() {
		return accountDataList.get(1);
	}

	@Nonnull
	public AccountData getAccountData3() {
		return accountDataList.get(2);
	}

	public static final class AccountData {

		@Nonnull
		private final TestAccount account;

		@Nonnull
		private final List<User> users = new ArrayList<User>();

		@Nonnull
		private final List<ApiChat> chats = new ArrayList<ApiChat>();

		private AccountData(@Nonnull TestAccount account) {
			this.account = account;
		}

		@Nonnull
		public TestAccount getAccount() {
			return account;
		}

		@Nonnull
		public List<User> getUsers() {
			return users;
		}

		@Nonnull
		public List<User> getContacts() {
			return users.subList(1, users.size());
		}

		@Nonnull
		public List<ApiChat> getChats() {
			return chats;
		}
	}
}
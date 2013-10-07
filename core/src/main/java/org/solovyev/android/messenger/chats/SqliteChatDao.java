package org.solovyev.android.messenger.chats;

import android.app.Application;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.solovyev.android.db.*;
import org.solovyev.android.db.properties.PropertyByIdDbQuery;
import org.solovyev.android.messenger.MergeDaoResult;
import org.solovyev.android.messenger.MergeDaoResultImpl;
import org.solovyev.android.messenger.db.StringIdMapper;
import org.solovyev.android.messenger.entities.Entity;
import org.solovyev.android.messenger.entities.EntityMapper;
import org.solovyev.android.messenger.messages.ChatMessage;
import org.solovyev.android.messenger.messages.SqliteChatMessageDao;
import org.solovyev.android.messenger.users.User;
import org.solovyev.android.messenger.users.UserService;
import org.solovyev.android.properties.AProperty;
import org.solovyev.common.Converter;
import org.solovyev.common.collections.Collections;

import javax.annotation.Nonnull;
import javax.inject.Singleton;
import java.util.*;

import static com.google.common.base.Predicates.equalTo;
import static com.google.common.collect.Iterables.find;
import static org.solovyev.android.db.AndroidDbUtils.*;

/**
 * User: serso
 * Date: 6/6/12
 * Time: 3:27 PM
 */
@Singleton
public class SqliteChatDao extends AbstractSQLiteHelper implements ChatDao {

    /*
	**********************************************************************
    *
    *                           AUTO INJECTED FIELDS
    *
    **********************************************************************
    */

	@Inject
	@Nonnull
	private UserService userService;

	/*
	**********************************************************************
	*
	*                           FIELDS
	*
	**********************************************************************
	*/

	@Nonnull
	private final Dao<Chat> dao;

	@Inject
	public SqliteChatDao(@Nonnull Application context, @Nonnull SQLiteOpenHelper sqliteOpenHelper) {
		super(context, sqliteOpenHelper);
		dao = new SqliteDao<Chat>("chats", "id", new ChatDaoMapper(this), context, sqliteOpenHelper);
	}

	@Nonnull
	@Override
	public List<String> readChatIdsByUserId(@Nonnull String userId) {
		return doDbQuery(getSqliteOpenHelper(), new LoadChatIdsByUserId(getContext(), userId, getSqliteOpenHelper()));
	}

	@Override
	public long update(@Nonnull Chat chat) {
		final long rows = dao.update(chat);
		if (rows >= 0) {
			doDbExecs(getSqliteOpenHelper(), Arrays.<DbExec>asList(new DeleteChatProperties(chat), new InsertChatProperties(chat)));
		}

		return rows;
	}

	@Override
	public void deleteAll() {
		doDbExec(getSqliteOpenHelper(), DeleteAllRowsDbExec.newInstance("user_chats"));
		doDbExec(getSqliteOpenHelper(), DeleteAllRowsDbExec.newInstance("chat_properties"));
		dao.deleteAll();
	}

	@Nonnull
	@Override
	public Map<Entity, Integer> getUnreadChats() {
		return doDbQuery(getSqliteOpenHelper(), new UnreadChatsLoader(getContext(), getSqliteOpenHelper()));
	}

	@Override
	public void delete(@Nonnull User user, @Nonnull Chat chat) {
		doDbExec(getSqliteOpenHelper(), new RemoveChats(user.getId(), chat));
	}

	@Nonnull
	@Override
	public Collection<String> readAllIds() {
		return dao.readAllIds();
	}

	@Nonnull
	@Override
	public List<AProperty> readPropertiesById(@Nonnull String chatId) {
		return doDbQuery(getSqliteOpenHelper(), new LoadChatPropertiesDbQuery(chatId, getContext(), getSqliteOpenHelper()));
	}

	@Nonnull
	@Override
	public List<Chat> readChatsByUserId(@Nonnull String userId) {
		return doDbQuery(getSqliteOpenHelper(), new LoadChatsByUserId(getContext(), userId, getSqliteOpenHelper(), this));
	}

	@Nonnull
	@Override
	public List<User> readParticipants(@Nonnull String chatId) {
		return doDbQuery(getSqliteOpenHelper(), new LoadChatParticipants(getContext(), chatId, userService, getSqliteOpenHelper()));
	}

	@Override
	public Chat read(@Nonnull String chatId) {
		return dao.read(chatId);
	}

	@Nonnull
	@Override
	public Collection<Chat> readAll() {
		return dao.readAll();
	}

	@Override
	public long create(@Nonnull Chat chat) {
		return dao.create(chat);
	}

	@Override
	public void delete(@Nonnull Chat chat) {
		deleteById(chat.getId());
	}

	@Override
	public void deleteById(@Nonnull String id) {
		dao.deleteById(id);
	}

	private static final class LoadChatParticipants extends AbstractDbQuery<List<User>> {

		@Nonnull
		private final String chatId;

		@Nonnull
		private final UserService userService;

		private LoadChatParticipants(@Nonnull Context context,
									 @Nonnull String chatId,
									 @Nonnull UserService userService,
									 @Nonnull SQLiteOpenHelper sqliteOpenHelper) {
			super(context, sqliteOpenHelper);
			this.chatId = chatId;
			this.userService = userService;
		}

		@Nonnull
		@Override
		public Cursor createCursor(@Nonnull SQLiteDatabase db) {
			return db.query("user_chats", null, "chat_id = ? ", new String[]{chatId}, null, null, null);
		}

		@Nonnull
		@Override
		public List<User> retrieveData(@Nonnull Cursor cursor) {
			return new ListMapper<User>(new ChatParticipantMapper(userService)).convert(cursor);
		}
	}

	private static final class LoadChatsByUserId extends AbstractDbQuery<List<Chat>> {

		@Nonnull
		private final String userId;

		@Nonnull
		private final ChatDao chatDao;

		private LoadChatsByUserId(@Nonnull Context context, @Nonnull String userId, @Nonnull SQLiteOpenHelper sqliteOpenHelper, @Nonnull ChatDao chatDao) {
			super(context, sqliteOpenHelper);
			this.userId = userId;
			this.chatDao = chatDao;
		}

		@Nonnull
		@Override
		public Cursor createCursor(@Nonnull SQLiteDatabase db) {
			return db.query("chats", null, "id in (select chat_id from user_chats where user_id = ? ) ", new String[]{userId}, null, null, null);
		}

		@Nonnull
		@Override
		public List<Chat> retrieveData(@Nonnull Cursor cursor) {
			return new ListMapper<Chat>(new ChatMapper(chatDao)).convert(cursor);
		}
	}

	public static final class LoadChatPropertiesDbQuery extends PropertyByIdDbQuery {

		public LoadChatPropertiesDbQuery(@Nonnull String chatId, @Nonnull Context context, @Nonnull SQLiteOpenHelper sqliteOpenHelper) {
			super(context, sqliteOpenHelper, "chat_properties", "chat_id", chatId);
		}
	}

	@Nonnull
	@Override
	public MergeDaoResult<ApiChat, String> mergeChats(@Nonnull String userId, @Nonnull List<? extends ApiChat> apiChats) {
		final MergeDaoResultImpl<ApiChat, String> result = getMergeResult(userId, apiChats);

		final List<DbExec> execs = new ArrayList<DbExec>();

		if (!result.getRemovedObjectIds().isEmpty()) {
			execs.addAll(RemoveChats.newInstances(userId, result.getRemovedObjectIds()));
		}

		for (ApiChat updatedChat : result.getUpdatedObjects()) {
			execs.add(new UpdateChat(updatedChat.getChat()));
			execs.add(new DeleteChatProperties(updatedChat.getChat()));
			execs.add(new InsertChatProperties(updatedChat.getChat()));
		}

		for (ApiChat addedChatLink : result.getAddedObjectLinks()) {
			execs.add(new UpdateChat(addedChatLink.getChat()));
			execs.add(new DeleteChatProperties(addedChatLink.getChat()));
			execs.add(new InsertChatProperties(addedChatLink.getChat()));
			execs.add(new InsertChatLink(userId, addedChatLink.getChat().getEntity().getEntityId()));
		}

		for (ApiChat addedChat : result.getAddedObjects()) {
			execs.add(new InsertChat(addedChat.getChat()));
			execs.add(new InsertChatProperties(addedChat.getChat()));
			execs.add(new InsertChatLink(userId, addedChat.getChat().getEntity().getEntityId()));
			for (ChatMessage chatMessage : addedChat.getMessages()) {
				execs.add(new SqliteChatMessageDao.InsertMessage(addedChat.getChat(), chatMessage));
			}

			for (User participant : addedChat.getParticipants()) {
				if (!participant.getEntity().getEntityId().equals(userId)) {
					execs.add(new InsertChatLink(participant.getEntity().getEntityId(), addedChat.getChat().getEntity().getEntityId()));
				}
			}
		}

		doDbExecs(getSqliteOpenHelper(), execs);

		return result;
	}

	private MergeDaoResultImpl<ApiChat, String> getMergeResult(@Nonnull String userId, List<? extends ApiChat> apiChats) {
		// !!! actually not all chats are loaded and we cannot delete the chat just because it is not in the list
		return getMergeResult(userId, apiChats, false, false);
	}

	private MergeDaoResultImpl<ApiChat, String> getMergeResult(@Nonnull String userId, List<? extends ApiChat> apiChats, boolean allowRemoval, boolean allowUpdate) {
		final MergeDaoResultImpl<ApiChat, String> result = new MergeDaoResultImpl<ApiChat, String>(apiChats);

		final List<String> idsFromDb = readChatIdsByUserId(userId);
		for (final String idFromDb : idsFromDb) {
			try {
				// chat exists both in db and on remote server => just update chat properties
				final ApiChat updatedObject = find(apiChats, new ChatByIdFinder(idFromDb));
				if (allowUpdate) {
					result.addUpdatedObject(updatedObject);
				}
			} catch (NoSuchElementException e) {
				if (allowRemoval) {
					// chat was removed on remote server => need to remove from local db
					result.addRemovedObjectId(idFromDb);
				}
			}
		}

		final Collection<String> allIdsFromDb = readAllIds();
		for (ApiChat apiChat : apiChats) {
			try {
				// chat exists both in db and on remote server => case already covered above
				find(idsFromDb, equalTo(apiChat.getChat().getEntity().getEntityId()));
			} catch (NoSuchElementException e) {
				// chat was added on remote server => need to add to local db
				if (allIdsFromDb.contains(apiChat.getChat().getEntity().getEntityId())) {
					// only link must be added - chat already in chats table
					result.addAddedObjectLink(apiChat);
				} else {
					// no chat information in local db is available - full chat insertion
					result.addAddedObject(apiChat);
				}
			}
		}
		return result;
	}

	private static final class LoadChatIdsByUserId extends AbstractDbQuery<List<String>> {

		@Nonnull
		private final String userId;

		private LoadChatIdsByUserId(@Nonnull Context context, @Nonnull String userId, @Nonnull SQLiteOpenHelper sqliteOpenHelper) {
			super(context, sqliteOpenHelper);
			this.userId = userId;
		}

		@Nonnull
		@Override
		public Cursor createCursor(@Nonnull SQLiteDatabase db) {
			return db.query("chats", null, "id in (select chat_id from user_chats where user_id = ? ) ", new String[]{userId}, null, null, null);
		}

		@Nonnull
		@Override
		public List<String> retrieveData(@Nonnull Cursor cursor) {
			return new ListMapper<String>(StringIdMapper.getInstance()).convert(cursor);
		}
	}

	private static class ChatByIdFinder implements Predicate<ApiChat> {

		@Nonnull
		private final String chatId;

		public ChatByIdFinder(@Nonnull String chatId) {
			this.chatId = chatId;
		}

		@Override
		public boolean apply(@javax.annotation.Nullable ApiChat apiChat) {
			return apiChat != null && chatId.equals(apiChat.getChat().getEntity().getEntityId());
		}
	}

	private static final class RemoveChats implements DbExec {

		@Nonnull
		private String userId;

		@Nonnull
		private List<String> chatIds;

		private RemoveChats(@Nonnull String userId, @Nonnull List<String> chatIds) {
			this.userId = userId;
			this.chatIds = chatIds;
		}

		private RemoveChats(@Nonnull String userId, @Nonnull Chat chat) {
			this.userId = userId;
			this.chatIds = Arrays.asList(chat.getId());
		}

		@Nonnull
		private static List<RemoveChats> newInstances(@Nonnull String userId, @Nonnull List<String> chatIds) {
			final List<RemoveChats> result = new ArrayList<RemoveChats>();

			for (List<String> chatIdsChunk : Collections.split(chatIds, MAX_IN_COUNT)) {
				result.add(new RemoveChats(userId, chatIdsChunk));
			}

			return result;
		}

		@Override
		public long exec(@Nonnull SQLiteDatabase db) {
			return db.delete("user_chats", "user_id = ? and chat_id in " + AndroidDbUtils.inClause(chatIds), AndroidDbUtils.inClauseValues(chatIds, userId));
		}
	}


	private static final class UpdateChat extends AbstractObjectDbExec<Chat> {

		private UpdateChat(@Nonnull Chat chat) {
			super(chat);
		}

		@Override
		public long exec(@Nonnull SQLiteDatabase db) {
			final Chat chat = getNotNullObject();

			final ContentValues values = toContentValues(chat);

			return db.update("chats", values, "id = ?", new String[]{String.valueOf(chat.getEntity().getEntityId())});
		}
	}

	private static final class InsertChat extends AbstractObjectDbExec<Chat> {

		private InsertChat(@Nonnull Chat chat) {
			super(chat);
		}

		@Override
		public long exec(@Nonnull SQLiteDatabase db) {
			final Chat chat = getNotNullObject();

			final ContentValues values = toContentValues(chat);

			return db.insert("chats", null, values);
		}
	}

	@Nonnull
	private static ContentValues toContentValues(@Nonnull Chat chat) {
		final DateTimeFormatter dateTimeFormatter = ISODateTimeFormat.basicDateTime();

		final DateTime lastMessagesSyncDate = chat.getLastMessagesSyncDate();

		final ContentValues values = new ContentValues();

		values.put("id", chat.getEntity().getEntityId());
		values.put("account_id", chat.getEntity().getAccountId());
		values.put("realm_chat_id", chat.getEntity().getAccountEntityId());
		values.put("last_messages_sync_date", lastMessagesSyncDate == null ? null : dateTimeFormatter.print(lastMessagesSyncDate));

		return values;
	}

	private static final class DeleteChatProperties extends AbstractObjectDbExec<Chat> {

		private DeleteChatProperties(@Nonnull Chat chat) {
			super(chat);
		}

		@Override
		public long exec(@Nonnull SQLiteDatabase db) {
			final Chat chat = getNotNullObject();

			return db.delete("chat_properties", "chat_id = ?", new String[]{String.valueOf(chat.getEntity().getEntityId())});
		}
	}

	private static final class InsertChatProperties extends AbstractObjectDbExec<Chat> {

		private InsertChatProperties(@Nonnull Chat chat) {
			super(chat);
		}

		@Override
		public long exec(@Nonnull SQLiteDatabase db) {
			long result = 0;
			final Chat chat = getNotNullObject();

			for (AProperty property : chat.getProperties()) {
				final ContentValues values = new ContentValues();
				values.put("chat_id", chat.getEntity().getEntityId());
				values.put("property_name", property.getName());
				values.put("property_value", property.getValue());
				final long id = db.insert("chat_properties", null, values);
				if (id == SQL_ERROR) {
					result = SQL_ERROR;
				}
			}

			return result;
		}
	}

	private static final class InsertChatLink implements DbExec {

		@Nonnull
		private String userId;

		@Nonnull
		private String chatId;

		private InsertChatLink(@Nonnull String userId, @Nonnull String chatId) {
			this.userId = userId;
			this.chatId = chatId;
		}

		@Override
		public long exec(@Nonnull SQLiteDatabase db) {
			final ContentValues values = new ContentValues();
			values.put("user_id", userId);
			values.put("chat_id", chatId);
			return db.insert("user_chats", null, values);
		}
	}

	private static final class UnreadChatsLoader extends AbstractDbQuery<Map<Entity, Integer>> {

		protected UnreadChatsLoader(@Nonnull Context context, @Nonnull SQLiteOpenHelper sqliteOpenHelper) {
			super(context, sqliteOpenHelper);
		}

		@Nonnull
		@Override
		public Cursor createCursor(@Nonnull SQLiteDatabase db) {
			return db.rawQuery("select c.id, c.account_id, c.realm_chat_id, count(*) from chats c, messages m where c.id = m.chat_id and m.read = 0 group by c.id, c.account_id, c.realm_chat_id", null);
		}

		@Nonnull
		@Override
		public Map<Entity, Integer> retrieveData(@Nonnull Cursor cursor) {
			final Map<Entity, Integer> result = new HashMap<Entity, Integer>();

			if (cursor.moveToFirst()) {
				while (!cursor.isAfterLast()) {
					final int unreadMessagesCount = cursor.getInt(3);
					if (unreadMessagesCount > 0) {
						result.put(EntityMapper.newInstanceFor(0).convert(cursor), unreadMessagesCount);
					}
					cursor.moveToNext();
				}
			}

			return result;
		}
	}

	private static final class ChatDaoMapper implements SqliteDaoEntityMapper<Chat> {

		@Nonnull
		private final ChatMapper chatMapper;

		private ChatDaoMapper(@Nonnull ChatDao dao) {
			chatMapper = new ChatMapper(dao);
		}

		@Nonnull
		@Override
		public ContentValues toContentValues(@Nonnull Chat chat) {
			return SqliteChatDao.toContentValues(chat);
		}

		@Nonnull
		@Override
		public Converter<Cursor, Chat> getCursorMapper() {
			return chatMapper;
		}

		@Nonnull
		@Override
		public String getId(@Nonnull Chat chat) {
			return chat.getId();
		}
	}
}

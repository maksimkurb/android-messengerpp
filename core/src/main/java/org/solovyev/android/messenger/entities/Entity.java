package org.solovyev.android.messenger.entities;

import android.os.Parcelable;

import javax.annotation.Nonnull;

/**
 * User: serso
 * Date: 2/24/13
 * Time: 4:10 PM
 */
public interface Entity extends Parcelable {

	/**
	 * @return unique ID of entity (user/chat/message) in application
	 */
	@Nonnull
	String getEntityId();

	/**
	 * @return account to which entity is belonged to
	 */
	@Nonnull
	String getAccountId();

	/**
	 * @return realm def id to which user is belonged to
	 */
	@Nonnull
	String getRealmDefId();

	/**
	 * @return user id in account
	 */
	@Nonnull
	String getAccountEntityId();

	/**
	 * @return id in realm generated by application
	 */
	@Nonnull
	String getAppRealmEntityId();

    /*
	**********************************************************************
    *
    *                           EQUALS/HASHCODE/CLONE
    *
    **********************************************************************
    */

	int hashCode();

	boolean equals(Object o);

	@Nonnull
	Entity clone();
}

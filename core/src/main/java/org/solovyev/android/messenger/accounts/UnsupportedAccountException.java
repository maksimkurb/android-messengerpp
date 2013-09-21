package org.solovyev.android.messenger.accounts;

import javax.annotation.Nonnull;

/**
 * User: serso
 * Date: 7/22/12
 * Time: 1:12 AM
 */
public final class UnsupportedAccountException extends AccountException {

	public UnsupportedAccountException(@Nonnull String accountId) {
		super(accountId);
	}
}

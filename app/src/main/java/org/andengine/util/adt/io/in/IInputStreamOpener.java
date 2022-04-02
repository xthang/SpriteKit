package org.andengine.util.adt.io.in;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;

/**
 * (c) Zynga 2012
 *
 * @author Nicolas Gramlich <ngramlich@zynga.com>
 * @since 11:54:25 - 02.03.2012
 */
public interface IInputStreamOpener extends Cloneable {
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Methods
	// ===========================================================

	public InputStream open() throws IOException;

	// Thang.X
	@NonNull
	IInputStreamOpener clone() throws CloneNotSupportedException;
}


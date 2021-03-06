package org.andengine.util.adt.io.in;

import android.content.res.AssetManager;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;

/**
 * (c) Zynga 2012
 *
 * @author Nicolas Gramlich <ngramlich@zynga.com>
 * @since 12:05:38 - 02.03.2012
 */
public class AssetInputStreamOpener implements IInputStreamOpener {
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	private final AssetManager mAssetManager;
	private final String mAssetPath;

	// ===========================================================
	// Constructors
	// ===========================================================

	public AssetInputStreamOpener(final AssetManager pAssetManager, final String pAssetPath) {
		this.mAssetManager = pAssetManager;
		this.mAssetPath = pAssetPath;
	}

	// Thang.X
	@NonNull
	@Override
	public AssetInputStreamOpener clone() throws CloneNotSupportedException {
		return (AssetInputStreamOpener) super.clone();
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	@Override
	public InputStream open() throws IOException {
		return this.mAssetManager.open(this.mAssetPath);
	}

	// ===========================================================
	// Methods
	// ===========================================================

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}

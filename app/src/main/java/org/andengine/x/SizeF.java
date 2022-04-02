package org.andengine.x;

import static kotlin.jvm.internal.Intrinsics.checkNotNull;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public final class SizeF implements Parcelable {
    public SizeF(final float width, final float height) {
        mWidth = width;
        mHeight = height;
    }

    public float getWidth() {
        return mWidth;
    }

    public float getHeight() {
        return mHeight;
    }

    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (obj instanceof SizeF) {
            final SizeF other = (SizeF) obj;
            return mWidth == other.mWidth && mHeight == other.mHeight;
        }
        return false;
    }

    @Override
    public String toString() {
        return mWidth + "x" + mHeight;
    }

    private static NumberFormatException invalidSizeF(String s) {
        throw new NumberFormatException("Invalid SizeF: \"" + s + "\"");
    }

    public static SizeF parseSizeF(String string)
            throws NumberFormatException {
        checkNotNull(string, "string must not be null");

        int sep_ix = string.indexOf('*');
        if (sep_ix < 0) {
            sep_ix = string.indexOf('x');
        }
        if (sep_ix < 0) {
            throw invalidSizeF(string);
        }
        try {
            return new SizeF(Float.parseFloat(string.substring(0, sep_ix)),
                    Float.parseFloat(string.substring(sep_ix + 1)));
        } catch (NumberFormatException e) {
            throw invalidSizeF(string);
        } catch (IllegalArgumentException e) {
            throw invalidSizeF(string);
        }
    }

    @Override
    public int hashCode() {
        return Float.floatToIntBits(mWidth) ^ Float.floatToIntBits(mHeight);
    }

    private final float mWidth;
    private final float mHeight;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel out, int flags) {
        out.writeFloat(mWidth);
        out.writeFloat(mHeight);
    }

    public static final @NonNull
    Creator<SizeF> CREATOR = new Creator<SizeF>() {
        /**
         * Return a new size from the data in the specified parcel.
         */
        @Override
        public @NonNull
        SizeF createFromParcel(@NonNull Parcel in) {
            float width = in.readFloat();
            float height = in.readFloat();
            return new SizeF(width, height);
        }

        /**
         * Return an array of sizes of the specified size.
         */
        @Override
        public @NonNull
        SizeF[] newArray(int size) {
            return new SizeF[size];
        }
    };
}

package com.translator.messagingapp.mms.pdu;

import java.util.Arrays;

/**
 * Represents an encoded string value in MMS PDUs.
 */
public class EncodedStringValue {
    private final byte[] mData;
    private final int mCharacterSet;
    
    // Default character set (UTF-8)
    public static final int CHARSET_UTF8 = 106;

    /**
     * Creates a new encoded string value with UTF-8 encoding.
     *
     * @param data The string data
     */
    public EncodedStringValue(String data) {
        this(data.getBytes(), CHARSET_UTF8);
    }

    /**
     * Creates a new encoded string value.
     *
     * @param data The byte data
     * @param characterSet The character set
     */
    public EncodedStringValue(byte[] data, int characterSet) {
        mData = data != null ? Arrays.copyOf(data, data.length) : new byte[0];
        mCharacterSet = characterSet;
    }

    /**
     * Gets the byte data.
     *
     * @return The byte data
     */
    public byte[] getTextString() {
        return Arrays.copyOf(mData, mData.length);
    }

    /**
     * Gets the character set.
     *
     * @return The character set
     */
    public int getCharacterSet() {
        return mCharacterSet;
    }

    /**
     * Gets the string representation.
     *
     * @return The string value
     */
    public String getString() {
        return new String(mData);
    }

    @Override
    public String toString() {
        return getString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        EncodedStringValue that = (EncodedStringValue) obj;
        return mCharacterSet == that.mCharacterSet && Arrays.equals(mData, that.mData);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(mData);
        result = 31 * result + mCharacterSet;
        return result;
    }
}
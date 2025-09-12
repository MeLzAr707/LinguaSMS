package com.translator.messagingapp.mms.pdu;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the body of an MMS message containing multiple parts.
 */
public class PduBody {
    private final List<PduPart> mParts = new ArrayList<>();

    /**
     * Adds a part to the body.
     *
     * @param part The part to add
     */
    public void addPart(PduPart part) {
        if (part != null) {
            mParts.add(part);
        }
    }

    /**
     * Gets the part at the specified index.
     *
     * @param index The index
     * @return The part at the index
     */
    public PduPart getPart(int index) {
        if (index >= 0 && index < mParts.size()) {
            return mParts.get(index);
        }
        return null;
    }

    /**
     * Gets the number of parts.
     *
     * @return The number of parts
     */
    public int getPartsNum() {
        return mParts.size();
    }

    /**
     * Removes the part at the specified index.
     *
     * @param index The index
     * @return The removed part, or null if index is invalid
     */
    public PduPart removePart(int index) {
        if (index >= 0 && index < mParts.size()) {
            return mParts.remove(index);
        }
        return null;
    }

    /**
     * Gets all parts.
     *
     * @return List of all parts
     */
    public List<PduPart> getParts() {
        return new ArrayList<>(mParts);
    }

    /**
     * Clears all parts.
     */
    public void clear() {
        mParts.clear();
    }

    /**
     * Checks if the body is empty.
     *
     * @return True if the body has no parts
     */
    public boolean isEmpty() {
        return mParts.isEmpty();
    }

    @Override
    public String toString() {
        return "PduBody{" +
                "parts=" + mParts.size() +
                '}';
    }
}
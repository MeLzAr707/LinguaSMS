package com.translator.messagingapp.mms.pdu;

/**
 * Base class for all MMS PDU objects.
 */
public abstract class GenericPdu {
    
    /**
     * Gets the message type of this PDU.
     *
     * @return The message type
     */
    public abstract int getMessageType();

    /**
     * Gets a string representation of this PDU.
     *
     * @return String representation
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "messageType=" + PduHeaders.getMessageTypeName(getMessageType()) +
                '}';
    }
}
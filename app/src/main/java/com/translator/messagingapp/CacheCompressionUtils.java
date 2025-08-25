package com.translator.messagingapp;

import android.util.Log;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Utility class for compressing and decompressing cached message data
 * to reduce storage requirements while maintaining fast read/write speeds.
 */
public class CacheCompressionUtils {
    private static final String TAG = "CacheCompression";
    private static final int COMPRESSION_THRESHOLD = 1024; // Only compress data larger than 1KB
    
    /**
     * Compressed wrapper for message data
     */
    public static class CompressedMessageData implements Serializable {
        private static final long serialVersionUID = 1L;
        
        public final byte[] compressedData;
        public final boolean isCompressed;
        public final int originalSize;
        public final int compressedSize;
        public final long compressionTime;
        
        public CompressedMessageData(byte[] data, boolean compressed, int originalSize) {
            this.compressedData = data;
            this.isCompressed = compressed;
            this.originalSize = originalSize;
            this.compressedSize = data.length;
            this.compressionTime = System.currentTimeMillis();
        }
        
        /**
         * Gets the compression ratio as a percentage.
         */
        public double getCompressionRatio() {
            if (!isCompressed || originalSize == 0) {
                return 100.0;
            }
            return (double) compressedSize / originalSize * 100.0;
        }
        
        /**
         * Gets space saved in bytes.
         */
        public int getSpaceSaved() {
            return isCompressed ? originalSize - compressedSize : 0;
        }
    }
    
    /**
     * Serializable wrapper for a list of messages to enable compression.
     */
    private static class SerializableMessageList implements Serializable {
        private static final long serialVersionUID = 1L;
        
        public final List<SerializableMessage> messages;
        
        public SerializableMessageList(List<Message> messages) {
            this.messages = new ArrayList<>();
            for (Message msg : messages) {
                this.messages.add(new SerializableMessage(msg));
            }
        }
        
        public List<Message> toMessageList() {
            List<Message> result = new ArrayList<>();
            for (SerializableMessage sMsg : messages) {
                result.add(sMsg.toMessage());
            }
            return result;
        }
    }
    
    /**
     * Serializable representation of a Message object.
     */
    private static class SerializableMessage implements Serializable {
        private static final long serialVersionUID = 1L;
        
        public final long id;
        public final String body;
        public final long date;
        public final int type;
        public final boolean read;
        public final String address;
        public final long threadId;
        public final String contactName;
        public final int messageType;
        
        public SerializableMessage(Message msg) {
            this.id = msg.getId();
            this.body = msg.getBody();
            this.date = msg.getDate();
            this.type = msg.getType();
            this.read = msg.isRead();
            this.address = msg.getAddress();
            this.threadId = msg.getThreadId();
            this.contactName = msg.getContactName();
            this.messageType = msg.getMessageType();
        }
        
        public Message toMessage() {
            Message msg = new Message();
            msg.setId(id);
            msg.setBody(body);
            msg.setDate(date);
            msg.setType(type);
            msg.setRead(read);
            msg.setAddress(address);
            msg.setThreadId(threadId);
            msg.setContactName(contactName);
            msg.setMessageType(messageType);
            return msg;
        }
    }
    
    /**
     * Compresses a list of messages if beneficial.
     *
     * @param messages The list of messages to compress
     * @return CompressedMessageData containing the potentially compressed data
     */
    public static CompressedMessageData compressMessages(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return new CompressedMessageData(new byte[0], false, 0);
        }
        
        try {
            // Serialize the messages first
            SerializableMessageList serializableList = new SerializableMessageList(messages);
            byte[] serializedData = serialize(serializableList);
            
            // Only compress if data is larger than threshold
            if (serializedData.length < COMPRESSION_THRESHOLD) {
                Log.d(TAG, "Skipping compression for small data (" + serializedData.length + " bytes)");
                return new CompressedMessageData(serializedData, false, serializedData.length);
            }
            
            // Compress the serialized data
            long startTime = System.currentTimeMillis();
            byte[] compressedData = compressData(serializedData);
            long compressionTime = System.currentTimeMillis() - startTime;
            
            // Check if compression was beneficial
            double compressionRatio = (double) compressedData.length / serializedData.length;
            
            if (compressionRatio > 0.9) {
                // Less than 10% compression - not worth it
                Log.d(TAG, "Poor compression ratio (" + String.format("%.1f%%", compressionRatio * 100) + 
                      "), using uncompressed data");
                return new CompressedMessageData(serializedData, false, serializedData.length);
            }
            
            Log.d(TAG, "Compressed " + messages.size() + " messages from " + serializedData.length + 
                  " to " + compressedData.length + " bytes (" + 
                  String.format("%.1f%%", compressionRatio * 100) + ") in " + compressionTime + "ms");
            
            return new CompressedMessageData(compressedData, true, serializedData.length);
            
        } catch (Exception e) {
            Log.e(TAG, "Error compressing messages", e);
            // Return empty data on error to prevent cache corruption
            return new CompressedMessageData(new byte[0], false, 0);
        }
    }
    
    /**
     * Decompresses message data back to a list of messages.
     *
     * @param compressedData The compressed message data
     * @return List of messages, or null if decompression failed
     */
    public static List<Message> decompressMessages(CompressedMessageData compressedData) {
        if (compressedData == null || compressedData.compressedData.length == 0) {
            return new ArrayList<>();
        }
        
        try {
            byte[] data;
            
            if (compressedData.isCompressed) {
                // Decompress the data
                long startTime = System.currentTimeMillis();
                data = decompressData(compressedData.compressedData);
                long decompressionTime = System.currentTimeMillis() - startTime;
                
                Log.d(TAG, "Decompressed " + compressedData.compressedData.length + 
                      " to " + data.length + " bytes in " + decompressionTime + "ms");
            } else {
                // Data is not compressed
                data = compressedData.compressedData;
            }
            
            // Deserialize the data
            SerializableMessageList serializableList = (SerializableMessageList) deserialize(data);
            return serializableList.toMessageList();
            
        } catch (Exception e) {
            Log.e(TAG, "Error decompressing messages", e);
            return null;
        }
    }
    
    /**
     * Compresses byte array using GZIP compression.
     */
    private static byte[] compressData(byte[] data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
            gzos.write(data);
        }
        return baos.toByteArray();
    }
    
    /**
     * Decompresses GZIP compressed byte array.
     */
    private static byte[] decompressData(byte[] compressedData) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(compressedData);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        try (GZIPInputStream gzis = new GZIPInputStream(bais)) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzis.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
        }
        
        return baos.toByteArray();
    }
    
    /**
     * Serializes an object to byte array.
     */
    private static byte[] serialize(Serializable obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(obj);
        }
        return baos.toByteArray();
    }
    
    /**
     * Deserializes byte array to object.
     */
    private static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        try (ObjectInputStream ois = new ObjectInputStream(bais)) {
            return ois.readObject();
        }
    }
    
    /**
     * Gets compression statistics for a compressed data object.
     */
    public static String getCompressionStats(CompressedMessageData data) {
        if (data == null) {
            return "No data";
        }
        
        if (!data.isCompressed) {
            return String.format("Uncompressed: %d bytes", data.originalSize);
        }
        
        return String.format("Compressed: %d → %d bytes (%.1f%%, saved %d bytes)",
                data.originalSize, data.compressedSize, data.getCompressionRatio(), data.getSpaceSaved());
    }
    
    /**
     * Estimates whether compression would be beneficial for a given data size.
     */
    public static boolean shouldCompress(int dataSize) {
        return dataSize >= COMPRESSION_THRESHOLD;
    }
}
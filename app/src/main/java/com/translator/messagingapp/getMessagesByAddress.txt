/**
     * Gets messages by address (phone number).
     *
     * @param address The address to filter by
     * @return A list of messages for the specified address
     */
    public List<Message> getMessagesByAddress(String address) {
        if (address == null || address.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<Message> messages = new ArrayList<>();
        ContentResolver contentResolver = context.getContentResolver();
        
        // Get SMS messages for this address
        Uri uri = Uri.parse("content://sms");
        String selection = "address = ?";
        String[] selectionArgs = new String[] { address };
        
        try (Cursor cursor = contentResolver.query(uri, null, selection, selectionArgs, "date DESC")) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String id = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms._ID));
                    String body = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY));
                    long date = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE));
                    int type = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE));
                    boolean read = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms.READ)) == 1;
                    String threadId = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID));
                    
                    Message message = new Message();
                    message.setId(Long.parseLong(id));
                    message.setBody(body);
                    message.setDate(date);
                    message.setType(type);
                    message.setRead(read);
                    message.setAddress(address);
                    message.setThreadId(Long.parseLong(threadId));
                    message.setMessageType(Message.MESSAGE_TYPE_SMS);
                    
                    messages.add(message);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting SMS messages by address", e);
        }
        
        // Get MMS messages for this address (simplified implementation)
        try {
            String threadId = getThreadIdForAddress(address);
            if (threadId != null) {
                uri = Uri.parse("content://mms");
                selection = "thread_id = ?";
                selectionArgs = new String[] { threadId };
                
                try (Cursor cursor = contentResolver.query(uri, null, selection, selectionArgs, "date DESC")) {
                    if (cursor != null && cursor.moveToFirst()) {
                        do {
                            String id = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Mms._ID));
                            long date = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Mms.DATE)) * 1000; // Convert to milliseconds
                            int type = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Mms.MESSAGE_BOX));
                            boolean read = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Mms.READ)) == 1;
                            
                            // Get MMS body
                            String body = getMmsText(contentResolver, id);
                            
                            MmsMessage message = new MmsMessage(id, body, date, type);
                            message.setRead(read);
                            message.setAddress(address);
                            message.setThreadId(Long.parseLong(threadId));
                            
                            messages.add(message);
                        } while (cursor.moveToNext());
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting MMS messages by address", e);
        }
        
        // Sort by date
        Collections.sort(messages, (m1, m2) -> Long.compare(m2.getDate(), m1.getDate()));
        
        return messages;
    }
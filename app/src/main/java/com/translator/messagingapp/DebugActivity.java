package com.translator.messagingapp;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class DebugActivity extends AppCompatActivity {
    private static final String TAG = "DebugActivity";

    private EditText addressInput;
    private Button checkButton;
    private Button openButton;
    private Button directOpenButton;
    private TextView resultText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Debug Tool");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize UI components
        addressInput = findViewById(R.id.debug_address_input);
        checkButton = findViewById(R.id.debug_check_button);
        openButton = findViewById(R.id.debug_open_button);
        directOpenButton = findViewById(R.id.debug_direct_open_button);
        resultText = findViewById(R.id.debug_result_text);

        // Set up click listeners
        checkButton.setOnClickListener(v -> checkAddress());
        openButton.setOnClickListener(v -> openConversation());
        directOpenButton.setOnClickListener(v -> openConversationDirect());

        // Check if we were launched with an address
        String launchedAddress = getIntent().getStringExtra("address");
        if (launchedAddress != null && !launchedAddress.isEmpty()) {
            addressInput.setText(launchedAddress);
            checkAddress(); // Automatically check the address
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void checkAddress() {
        String address = addressInput.getText().toString().trim();
        if (address.isEmpty()) {
            Toast.makeText(this, "Please enter an address", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder result = new StringBuilder();
        result.append("Address: ").append(address).append("\n\n");

        // Check different formats
        result.append("Normalized: ").append(PhoneNumberUtils.normalizeNumber(address)).append("\n");
        result.append("Stripped: ").append(PhoneNumberUtils.stripSeparators(address)).append("\n\n");

        // Check thread ID
        String threadId = getThreadIdForAddress(address);
        result.append("Thread ID: ").append(threadId != null ? threadId : "Not found").append("\n\n");

        // Check messages
        int messageCount = countMessagesForAddress(address);
        result.append("Message count: ").append(messageCount).append("\n\n");

        // Show sample messages
        result.append("Sample messages:\n");
        result.append(getSampleMessagesForAddress(address));

        // Show all threads that might match
        result.append("\nPotential matching threads:\n");
        result.append(findPotentialThreads(address));

        resultText.setText(result.toString());
    }

    private void openConversation() {
        String address = addressInput.getText().toString().trim();
        if (address.isEmpty()) {
            Toast.makeText(this, "Please enter an address", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Intent intent = new Intent(this, ConversationActivity.class);
            intent.putExtra("address", address);
            startActivity(intent);

            Log.d(TAG, "Started ConversationActivity with address: " + address);
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error opening conversation", e);
        }
    }

    private void openConversationDirect() {
        String address = addressInput.getText().toString().trim();
        if (address.isEmpty()) {
            Toast.makeText(this, "Please enter an address", Toast.LENGTH_SHORT).show();
            return;
        }

        String threadId = getThreadIdForAddress(address);

        try {
            Intent intent = new Intent(this, ConversationActivity.class);
            intent.putExtra("address", address);
            if (threadId != null) {
                intent.putExtra("thread_id", threadId);
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);

            Log.d(TAG, "Started ConversationActivity directly with address: " + address +
                    (threadId != null ? " and thread ID: " + threadId : ""));
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error opening conversation directly", e);
        }
    }

    private String getThreadIdForAddress(String address) {
        if (address == null) return null;

        // Try different formats
        String[] addressFormats = {
                address,
                PhoneNumberUtils.normalizeNumber(address),
                PhoneNumberUtils.stripSeparators(address)
        };

        for (String addr : addressFormats) {
            try (Cursor cursor = getContentResolver().query(
                    Uri.parse("content://sms/conversations"),
                    new String[]{"thread_id"},
                    null, null, null)) {

                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        int threadIdIndex = cursor.getColumnIndex("thread_id");
                        if (threadIdIndex >= 0) {
                            String threadId = cursor.getString(threadIdIndex);
                            if (threadContainsAddress(threadId, addr)) {
                                return threadId;
                            }
                        }
                    } while (cursor.moveToNext());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting thread ID", e);
            }
        }

        return null;
    }

    private boolean threadContainsAddress(String threadId, String address) {
        if (threadId == null || address == null) return false;

        try (Cursor cursor = getContentResolver().query(
                Telephony.Sms.CONTENT_URI,
                new String[]{Telephony.Sms.ADDRESS},
                Telephony.Sms.THREAD_ID + "=?",
                new String[]{threadId},
                null)) {

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int addressIndex = cursor.getColumnIndex(Telephony.Sms.ADDRESS);
                    if (addressIndex >= 0) {
                        String msgAddress = cursor.getString(addressIndex);
                        if (phoneNumbersMatch(msgAddress, address)) {
                            return true;
                        }
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking thread", e);
        }

        return false;
    }

    private boolean phoneNumbersMatch(String number1, String number2) {
        if (number1 == null || number2 == null) return false;

        // Try exact match
        if (number1.equals(number2)) return true;

        // Try normalized match
        String norm1 = PhoneNumberUtils.normalizeNumber(number1);
        String norm2 = PhoneNumberUtils.normalizeNumber(number2);
        if (norm1.equals(norm2)) return true;

        // Try stripped match
        String strip1 = PhoneNumberUtils.stripSeparators(number1);
        String strip2 = PhoneNumberUtils.stripSeparators(number2);
        if (strip1.equals(strip2)) return true;

        // Try PhoneNumberUtils.compare
        if (PhoneNumberUtils.compare(number1, number2)) return true;

        // Try last digits match
        String digits1 = getLastDigits(number1, 8);
        String digits2 = getLastDigits(number2, 8);
        return !digits1.isEmpty() && !digits2.isEmpty() && digits1.equals(digits2);
    }

    private String getLastDigits(String phoneNumber, int count) {
        if (phoneNumber == null) return "";

        // Remove all non-digit characters
        String digitsOnly = phoneNumber.replaceAll("\\D", "");

        // Return the last 'count' digits, or the whole string if shorter
        if (digitsOnly.length() <= count) {
            return digitsOnly;
        } else {
            return digitsOnly.substring(digitsOnly.length() - count);
        }
    }

    private int countMessagesForAddress(String address) {
        int count = 0;

        // Try different formats
        String[] addressFormats = {
                address,
                PhoneNumberUtils.normalizeNumber(address),
                PhoneNumberUtils.stripSeparators(address)
        };

        for (String addr : addressFormats) {
            try (Cursor cursor = getContentResolver().query(
                    Telephony.Sms.CONTENT_URI,
                    new String[]{"COUNT(*) as count"},
                    Telephony.Sms.ADDRESS + "=?",
                    new String[]{addr},
                    null)) {

                if (cursor != null && cursor.moveToFirst()) {
                    int countIndex = cursor.getColumnIndex("count");
                    if (countIndex >= 0) {
                        count += cursor.getInt(countIndex);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error counting messages", e);
            }
        }

        // Try one more approach - check all messages and compare numbers
        try (Cursor cursor = getContentResolver().query(
                Telephony.Sms.CONTENT_URI,
                new String[]{Telephony.Sms.ADDRESS},
                null, null, null)) {

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int addressIndex = cursor.getColumnIndex(Telephony.Sms.ADDRESS);
                    if (addressIndex >= 0) {
                        String msgAddress = cursor.getString(addressIndex);
                        if (phoneNumbersMatch(msgAddress, address)) {
                            count++;
                        }
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error counting all messages", e);
        }

        return count;
    }

    private String getSampleMessagesForAddress(String address) {
        StringBuilder result = new StringBuilder();

        // Try different formats
        String[] addressFormats = {
                address,
                PhoneNumberUtils.normalizeNumber(address),
                PhoneNumberUtils.stripSeparators(address)
        };

        for (String addr : addressFormats) {
            try (Cursor cursor = getContentResolver().query(
                    Telephony.Sms.CONTENT_URI,
                    new String[]{
                            Telephony.Sms._ID,
                            Telephony.Sms.ADDRESS,
                            Telephony.Sms.BODY,
                            Telephony.Sms.DATE,
                            Telephony.Sms.TYPE,
                            Telephony.Sms.THREAD_ID
                    },
                    Telephony.Sms.ADDRESS + "=?",
                    new String[]{addr},
                    Telephony.Sms.DATE + " DESC LIMIT 3")) {

                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        int addressIndex = cursor.getColumnIndex(Telephony.Sms.ADDRESS);
                        int bodyIndex = cursor.getColumnIndex(Telephony.Sms.BODY);
                        int typeIndex = cursor.getColumnIndex(Telephony.Sms.TYPE);
                        int threadIdIndex = cursor.getColumnIndex(Telephony.Sms.THREAD_ID);

                        if (addressIndex >= 0 && bodyIndex >= 0 && typeIndex >= 0 && threadIdIndex >= 0) {
                            String msgAddress = cursor.getString(addressIndex);
                            String body = cursor.getString(bodyIndex);
                            int type = cursor.getInt(typeIndex);
                            String threadId = cursor.getString(threadIdIndex);

                            result.append("- ")
                                    .append(type == Telephony.Sms.MESSAGE_TYPE_INBOX ? "Received: " : "Sent: ")
                                    .append(body.length() > 30 ? body.substring(0, 30) + "..." : body)
                                    .append(" (Address: ")
                                    .append(msgAddress)
                                    .append(", Thread: ")
                                    .append(threadId)
                                    .append(")\n");
                        }
                    } while (cursor.moveToNext());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting sample messages", e);
            }
        }

        // If we didn't find any messages with exact address match, try matching by phone number
        if (result.length() == 0) {
            try (Cursor cursor = getContentResolver().query(
                    Telephony.Sms.CONTENT_URI,
                    new String[]{
                            Telephony.Sms._ID,
                            Telephony.Sms.ADDRESS,
                            Telephony.Sms.BODY,
                            Telephony.Sms.DATE,
                            Telephony.Sms.TYPE,
                            Telephony.Sms.THREAD_ID
                    },
                    null, null,
                    Telephony.Sms.DATE + " DESC")) {

                if (cursor != null && cursor.moveToFirst()) {
                    int count = 0;
                    do {
                        int addressIndex = cursor.getColumnIndex(Telephony.Sms.ADDRESS);
                        int bodyIndex = cursor.getColumnIndex(Telephony.Sms.BODY);
                        int typeIndex = cursor.getColumnIndex(Telephony.Sms.TYPE);
                        int threadIdIndex = cursor.getColumnIndex(Telephony.Sms.THREAD_ID);

                        if (addressIndex >= 0 && bodyIndex >= 0 && typeIndex >= 0 && threadIdIndex >= 0) {
                            String msgAddress = cursor.getString(addressIndex);

                            if (phoneNumbersMatch(msgAddress, address)) {
                                String body = cursor.getString(bodyIndex);
                                int type = cursor.getInt(typeIndex);
                                String threadId = cursor.getString(threadIdIndex);

                                result.append("- ")
                                        .append(type == Telephony.Sms.MESSAGE_TYPE_INBOX ? "Received: " : "Sent: ")
                                        .append(body.length() > 30 ? body.substring(0, 30) + "..." : body)
                                        .append(" (Address: ")
                                        .append(msgAddress)
                                        .append(", Thread: ")
                                        .append(threadId)
                                        .append(")\n");

                                count++;
                                if (count >= 3) break; // Limit to 3 messages
                            }
                        }
                    } while (cursor.moveToNext());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting sample messages by number matching", e);
            }
        }

        return result.length() > 0 ? result.toString() : "No messages found for this address.\n";
    }

    private String findPotentialThreads(String address) {
        StringBuilder result = new StringBuilder();

        try (Cursor cursor = getContentResolver().query(
                Uri.parse("content://sms/conversations"),
                new String[]{"thread_id", "message_count"},
                null, null, null)) {

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int threadIdIndex = cursor.getColumnIndex("thread_id");
                    int messageCountIndex = cursor.getColumnIndex("message_count");

                    if (threadIdIndex >= 0) {
                        String threadId = cursor.getString(threadIdIndex);
                        int messageCount = messageCountIndex >= 0 ? cursor.getInt(messageCountIndex) : -1;

                        // Get a sample message from this thread
                        String sampleAddress = getSampleAddressFromThread(threadId);

                        result.append("Thread ID: ").append(threadId)
                                .append(", Messages: ").append(messageCount)
                                .append(", Sample Address: ").append(sampleAddress);

                        // Check if this thread might match our address
                        if (phoneNumbersMatch(sampleAddress, address)) {
                            result.append(" (MATCH)");
                        }

                        result.append("\n");
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error finding potential threads", e);
        }

        return result.length() > 0 ? result.toString() : "No threads found.\n";
    }

    private String getSampleAddressFromThread(String threadId) {
        if (threadId == null) return null;

        try (Cursor cursor = getContentResolver().query(
                Telephony.Sms.CONTENT_URI,
                new String[]{Telephony.Sms.ADDRESS},
                Telephony.Sms.THREAD_ID + "=?",
                new String[]{threadId},
                Telephony.Sms.DATE + " DESC LIMIT 1")) {

            if (cursor != null && cursor.moveToFirst()) {
                int addressIndex = cursor.getColumnIndex(Telephony.Sms.ADDRESS);
                if (addressIndex >= 0) {
                    return cursor.getString(addressIndex);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting sample address from thread", e);
        }

        return null;
    }
}


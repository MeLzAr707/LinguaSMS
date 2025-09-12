package demo;

import java.util.Date;

// Simple demonstration that would reproduce the original build errors
// if the fix wasn't implemented correctly

public class SmsMessageUsageDemo {
    
    public static void main(String[] args) {
        demonstrateOriginalErrorPatterns();
        demonstrateFixedImplementation();
    }
    
    /**
     * This demonstrates the patterns that were causing build errors.
     * These would fail if we had import conflicts.
     */
    public static void demonstrateOriginalErrorPatterns() {
        System.out.println("=== Demonstrating Original Error Patterns (Now Fixed) ===");
        
        // Pattern 1: Constructor with parameters that was failing
        String senderAddress = "+1234567890";
        String messageBody = "Hello, World!";
        long timestamp = System.currentTimeMillis();
        
        try {
            // This was the exact pattern causing the error:
            // "constructor SmsMessage in class SmsMessage cannot be applied to given types"
            com.translator.messagingapp.sms.SmsMessage smsMessage = 
                new com.translator.messagingapp.sms.SmsMessage(senderAddress, messageBody, new Date(timestamp));
            
            // Pattern 2: setIncoming method that was missing
            smsMessage.setIncoming(true);
            
            // Pattern 3: setRead method that was missing  
            smsMessage.setRead(false);
            
            System.out.println("✅ Constructor with timestamp: SUCCESS");
            System.out.println("✅ setIncoming method: SUCCESS");
            System.out.println("✅ setRead method: SUCCESS");
            System.out.println("   - Address: " + smsMessage.getAddress());
            System.out.println("   - Message: " + smsMessage.getOriginalText());
            System.out.println("   - Incoming: " + smsMessage.isIncoming());
            System.out.println("   - Read: " + smsMessage.isRead());
            
        } catch (Exception e) {
            System.out.println("❌ ERROR: " + e.getMessage());
        }
    }
    
    /**
     * This demonstrates how the fix allows both classes to coexist.
     */
    public static void demonstrateFixedImplementation() {
        System.out.println("\n=== Demonstrating Fixed Implementation ===");
        
        // Custom SmsMessage can now be used without conflicts
        com.translator.messagingapp.sms.SmsMessage customSms = 
            new com.translator.messagingapp.sms.SmsMessage("+1111111111", "Custom message");
        customSms.setIncoming(false);
        customSms.setRead(true);
        
        System.out.println("✅ Custom SmsMessage works correctly");
        System.out.println("   - Type: " + customSms.getClass().getSimpleName());
        System.out.println("   - Package: " + customSms.getClass().getPackage().getName());
        System.out.println("   - Has setIncoming: " + hasMethod(customSms.getClass(), "setIncoming"));
        System.out.println("   - Has setRead: " + hasMethod(customSms.getClass(), "setRead"));
        
        // Android SmsMessage class can still be referenced (though not easily instantiated in demo)
        try {
            Class<?> androidSmsClass = Class.forName("android.telephony.SmsMessage");
            System.out.println("✅ Android SmsMessage still accessible");
            System.out.println("   - Type: " + androidSmsClass.getSimpleName());
            System.out.println("   - Package: " + androidSmsClass.getPackage().getName());
            System.out.println("   - Has setIncoming: " + hasMethod(androidSmsClass, "setIncoming"));
            System.out.println("   - Has setRead: " + hasMethod(androidSmsClass, "setRead"));
        } catch (ClassNotFoundException e) {
            System.out.println("ℹ️  Android SmsMessage not available in this environment (expected)");
        }
        
        System.out.println("\n🎉 Fix verification complete!");
    }
    
    private static boolean hasMethod(Class<?> clazz, String methodName) {
        try {
            clazz.getMethod(methodName, boolean.class);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}
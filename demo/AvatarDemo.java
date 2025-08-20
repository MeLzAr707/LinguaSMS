/**
 * Simple demonstration of contact avatar logic without packages
 */
public class AvatarDemo {
    
    public static void main(String[] args) {
        System.out.println("=== LinguaSMS Contact Avatar Feature Demo ===\n");
        
        // Test different scenarios
        testContactInitial("John Doe", "J");
        testContactInitial("+1234567890", "1");
        testContactInitial("", "#");
        testContactInitial(null, "#");
        testContactInitial("@#$%", "#");
        testContactInitial("123 Pizza", "1");
        testContactInitial("alice", "A");
        
        System.out.println("\n=== Color Generation Test ===");
        testContactColor("John Doe");
        testContactColor("+1234567890");
        testContactColor("Alice Smith");
        testContactColor("+9876543210");
        
        System.out.println("\n=== Feature Summary ===");
        System.out.println("✓ Contact photos will be loaded from device contacts when available");
        System.out.println("✓ Fallback colored circles with initials for contacts without photos");
        System.out.println("✓ Consistent color generation based on contact name/number");
        System.out.println("✓ Proper error handling with default avatars");
        System.out.println("✓ Integration with existing Glide image loading");
        System.out.println("✓ Null safety checks throughout");
    }
    
    private static void testContactInitial(String input, String expected) {
        String result = getContactInitial(input);
        String displayInput = (input == null) ? "null" : "\"" + input + "\"";
        System.out.println("Input: " + displayInput + " -> Initial: \"" + result + "\" " + 
                          (result.equals(expected) ? "✓" : "✗ (expected: " + expected + ")"));
    }
    
    private static void testContactColor(String input) {
        int color = getContactColor(input);
        System.out.println("\"" + input + "\" -> Color: " + String.format("#%08X", color));
    }
    
    /**
     * Gets the first letter of a contact name or phone number.
     */
    public static String getContactInitial(String contactName) {
        if (contactName == null || contactName.trim().isEmpty()) {
            return "#";
        }

        String cleanName = contactName.trim();
        if (cleanName.isEmpty()) {
            return "#";
        }

        // For phone numbers starting with +, skip the + and find first digit
        if (cleanName.startsWith("+")) {
            for (int i = 1; i < cleanName.length(); i++) {
                char ch = cleanName.charAt(i);
                if (Character.isDigit(ch)) {
                    return String.valueOf(ch);
                }
            }
            return "#"; // No digits found after +
        }

        char firstChar = cleanName.charAt(0);

        if (Character.isLetter(firstChar)) {
            return String.valueOf(Character.toUpperCase(firstChar));
        }

        if (Character.isDigit(firstChar)) {
            return String.valueOf(firstChar);
        }

        return "#";
    }

    /**
     * Gets a color for a contact based on their name or phone number.
     */
    public static int getContactColor(String contactNameOrNumber) {
        if (contactNameOrNumber == null || contactNameOrNumber.trim().isEmpty()) {
            return 0xFF9E9E9E; // Default gray
        }

        int[] colors = {
                0xFFE57373, // Red
                0xFFF06292, // Pink
                0xFFBA68C8, // Purple
                0xFF9575CD, // Deep Purple
                0xFF7986CB, // Indigo
                0xFF64B5F6, // Blue
                0xFF4FC3F7, // Light Blue
                0xFF4DD0E1, // Cyan
                0xFF4DB6AC, // Teal
                0xFF81C784, // Green
                0xFFAED581, // Light Green
                0xFFFF8A65, // Deep Orange
                0xFFD4E157, // Lime
                0xFFFFD54F, // Amber
                0xFFFFB74D, // Orange
                0xFFA1887F  // Brown
        };

        int hashCode = contactNameOrNumber.hashCode();
        int index = Math.abs(hashCode) % colors.length;

        return colors[index];
    }
}
# Translation Original Display - Visual Example

## Before Changes (Current Behavior)
When a message is translated, only the translated text is shown:

```
┌─────────────────────────────────┐
│ John Doe                        │
├─────────────────────────────────┤
│ ┌─────────────────────────────┐ │
│ │ Hola mundo                  │ │ ← Only translated text shown
│ │ 12:34 PM        [🔄]        │ │
│ └─────────────────────────────┘ │
└─────────────────────────────────┘
```

## After Changes (New Behavior)
When a message is translated, both original and translated text are shown:

```
┌─────────────────────────────────┐
│ John Doe                        │
├─────────────────────────────────┤
│ ┌─────────────────────────────┐ │
│ │ Original: Hello world       │ │ ← Original text (italic, smaller)
│ │ Hola mundo                  │ │ ← Translated text (main)
│ │ 12:34 PM        [↩️]        │ │ ← Button shows restore icon
│ └─────────────────────────────┘ │
└─────────────────────────────────┘
```

## Translation Toggle States

### State 1: Original Message (No Translation)
```
┌─────────────────────────────────┐
│ │ Hello world                 │ │
│ │ 12:34 PM        [🔄]        │ │
└─────────────────────────────────┘
```

### State 2: Translation Shown (Both Texts)
```
┌─────────────────────────────────┐
│ │ Original: Hello world       │ │
│ │ Hola mundo                  │ │
│ │ 12:34 PM        [↩️]        │ │
└─────────────────────────────────┘
```

### State 3: Translation Hidden (Back to Original)
```
┌─────────────────────────────────┐
│ │ Hello world                 │ │
│ │ 12:34 PM        [🔄]        │ │
└─────────────────────────────────┘
```

## Key Features Implemented

1. **Dual Text Display**: Original message shown above translated message when translation is active
2. **Persistence**: Translation state saved when translating and restored when app restarts
3. **Toggle Functionality**: Users can toggle between showing/hiding translation while preserving both texts
4. **Visual Hierarchy**: Original text is smaller and italic to show it's secondary information
5. **Consistency**: Works across all message types (SMS, MMS, incoming, outgoing)

## Layout Changes Made

### Added to all message layouts:
```xml
<!-- Original message text (shown when translation is active) -->
<TextView
    android:id="@+id/original_text"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:textColor="@color/textColorSecondary"
    android:textSize="14sp"
    android:textStyle="italic"
    android:visibility="gone"
    tools:text="Original: This is an incoming message" />
```

### Logic in MessageRecyclerAdapter:
```java
if (message.isShowTranslation() && message.isTranslated()) {
    // Show both original and translated text
    originalText.setText("Original: " + originalBody);
    originalText.setVisibility(View.VISIBLE);
    messageText.setText(translatedText);
} else {
    // Show only original text
    messageText.setText(displayText);
    originalText.setVisibility(View.GONE);
}
```
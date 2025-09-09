# Custom Notification Tones - User Flow Diagram

## Visual Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                    LinguaSMS Conversation Screen                │
├─────────────────────────────────────────────────────────────────┤
│ ← John Smith                                    ☰ ⋮            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  John: Hey, are you coming to the party?      [10:30 AM]      │
│                                                                 │
│      You: Yes, I'll be there in 20 minutes    [10:32 AM]      │
│                                                                 │
│  John: Great! See you soon                     [10:33 AM]      │
│                                                                 │
├─────────────────────────────────────────────────────────────────┤
│ Type a message...                               📎  😊  ➤      │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ User taps menu (⋮)
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                        Menu Options                             │
├─────────────────────────────────────────────────────────────────┤
│ 📞  Call                                                        │
│ 🔄  Translate                                                   │
│ ⚙️   Contact Settings              ← NEW FEATURE                │
│ 🔄  Translate All Messages                                     │
│ 🗑️  Delete Conversation                                         │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ User selects "Contact Settings"
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                Contact Settings - John Smith                    │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│ 🔔  Notification Tone                                          │
│                                                                 │
│                          [Cancel]                               │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ User selects "Notification Tone"  
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                Select Notification Tone                         │
├─────────────────────────────────────────────────────────────────┤
│ Current: Default Tone                                           │
│                                                                 │
│ 🎵  Select Custom Tone                                         │
│ 🔕  Default Tone                                               │
│                                                                 │
│                          [Cancel]                               │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ User selects "Select Custom Tone"
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   Android Ringtone Picker                      │
├─────────────────────────────────────────────────────────────────┤
│ Select Notification Tone                                        │
│                                                                 │
│ ○ Default notification sound                                    │
│ ○ Chime                                                         │
│ ○ Bell                                                          │
│ ● Cheerful notification        ← User selects                   │
│ ○ Digital notification                                          │
│ ○ Elegant notification                                          │
│                                                                 │
│                    [Cancel]  [OK]                               │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ User taps "OK"
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                         Toast Message                           │
├─────────────────────────────────────────────────────────────────┤
│              ✅ Custom notification tone set                   │
└─────────────────────────────────────────────────────────────────┘
```

## When a Message Arrives

```
┌─────────────────────────────────────────────────────────────────┐
│                    Incoming Message Flow                        │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ 1. SMS/MMS Received from John Smith (+1234567890)              │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ 2. NotificationHelper.showSmsReceivedNotification()            │
│    - Normalize phone number: +1234567890 → 1234567890          │
│    - Check UserPreferences for custom tone                     │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ 3. Custom tone found: "content://settings/system/cheerful"     │
│    - Play custom notification sound                            │
│    - Show notification with John's message                     │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                     User Experience                             │
├─────────────────────────────────────────────────────────────────┤
│ 🔊 "Cheerful notification" sound plays                         │
│ 📱 Phone shows: "John Smith: Hey, I'm here!"                  │
│                                                                 │
│ User immediately recognizes it's from John due to the          │
│ distinctive notification sound they selected for him!           │
└─────────────────────────────────────────────────────────────────┘
```

## Data Flow Architecture

```
┌─────────────────┐    stores/retrieves    ┌──────────────────────┐
│  UserPreferences│◄─────────────────────►│   SharedPreferences  │
│                 │                        │                      │
│ - setContact... │                        │ Key: contact_tone_   │
│ - getContact... │                        │      1234567890      │
│ - hasContact... │                        │ Value: content://... │
└─────────────────┘                        └──────────────────────┘
         ▲
         │ uses
         ▼
┌─────────────────┐    creates/shows       ┌──────────────────────┐
│NotificationHelper│◄─────────────────────►│   Android Notification│
│                 │                        │                      │
│ - getNotification│                        │ - Custom tone played │
│   SoundForContact│                        │ - Message displayed  │
└─────────────────┘                        └──────────────────────┘
         ▲
         │ called by
         ▼
┌─────────────────┐    manages            ┌──────────────────────┐
│ ConversationAct │◄──────────────────────►│ ContactSettingsDialog│
│                 │                        │                      │
│ - Menu handling │                        │ - UI for tone setup │
│ - Activity result│                        │ - Ringtone picker   │
└─────────────────┘                        └──────────────────────┘
```

## Key Benefits

### For Users
- **🎵 Personalization**: Each contact can have a unique sound
- **⚡ Quick Recognition**: Instantly know who's texting without looking
- **🎯 Priority Management**: Important contacts can have distinct tones
- **🔄 Easy Management**: Simple menu-driven interface

### For Developers  
- **🔒 Robust Storage**: Phone number normalization ensures consistency
- **🛡️ Error Handling**: Graceful fallbacks when custom tones aren't available
- **🧪 Well Tested**: Comprehensive unit test coverage
- **📱 Platform Native**: Uses Android's built-in ringtone picker

## Usage Examples

1. **Family Priority**: Set your spouse's texts to play a heart sound
2. **Work Separation**: Boss gets a serious tone, colleagues get standard
3. **Emergency Contacts**: Critical contacts get loud, attention-grabbing tones  
4. **Friend Groups**: Different friend circles get different musical tones
5. **Service Notifications**: Banks, delivery services get subtle, distinct sounds
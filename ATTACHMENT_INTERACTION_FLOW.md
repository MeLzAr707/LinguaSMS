# Attachment Interaction Flow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                    ATTACHMENT INTERACTION FLOW                  │
└─────────────────────────────────────────────────────────────────┘

┌──────────────────┐
│  User sees MMS   │
│  with attachment │
│  in conversation │
└────────┬─────────┘
         │
         v
┌──────────────────┐
│   Image/Media    │
│   displayed as   │
│   thumbnail      │
└──────┬───────────┘
       │
       │ ┌─── Single Tap ────┐     │ ┌─── Long Press ────┐
       v │                   │     v │                   │
┌─────────────────┐           │ ┌─────────────────┐       │
│ onAttachmentClick│           │ │onAttachmentLong │       │
│    triggered     │           │ │  Click triggered│       │
└─────────┬───────┘           │ └─────────┬───────┘       │
          │                   │           │               │
          v                   │           v               │
┌─────────────────┐           │ ┌─────────────────┐       │
│ openAttachment() │           │ │showAttachmentOp │       │
│     method       │           │ │ tionsDialog()   │       │
└─────────┬───────┘           │ └─────────┬───────┘       │
          │                   │           │               │
          v                   │           v               │
┌─────────────────┐           │ ┌─────────────────┐       │
│ Intent.ACTION_  │           │ │ Dialog with 3   │       │
│ VIEW created    │           │ │ options shown   │       │
└─────────┬───────┘           │ └─────────┬───────┘       │
          │                   │           │               │
          v                   │           │               │
┌─────────────────┐           │           ├──── View ─────┤
│ System app      │           │           │               │
│ opens (Gallery, │           │           v               │
│ Video Player)   │           │ ┌─────────────────┐       │
└─────────────────┘           │ │ Same as single  │       │
                              │ │ tap behavior    │       │
                              │ └─────────────────┘       │
                              │                           │
                              │           ├──── Save ─────┤
                              │           │               │
                              │           v               │
                              │ ┌─────────────────┐       │
                              │ │ saveAttachment()│       │
                              │ │ method called   │       │
                              │ └─────────┬───────┘       │
                              │           │               │
                              │           v               │
                              │ ┌─────────────────┐       │
                              │ │ Android 10+:    │       │
                              │ │ MediaStore API  │       │
                              │ │ Used to save to │       │
                              │ │ appropriate     │       │
                              │ │ folder          │       │
                              │ └─────────┬───────┘       │
                              │           │               │
                              │           v               │
                              │ ┌─────────────────┐       │
                              │ │ Success toast   │       │
                              │ │ shown to user   │       │
                              │ └─────────────────┘       │
                              │                           │
                              │           ├─── Share ─────┤
                              │           │               │
                              │           v               │
                              │ ┌─────────────────┐       │
                              │ │shareAttachment()│       │
                              │ │ method called   │       │
                              │ └─────────┬───────┘       │
                              │           │               │
                              │           v               │
                              │ ┌─────────────────┐       │
                              │ │ Intent.ACTION_  │       │
                              │ │ SEND created    │       │
                              │ └─────────┬───────┘       │
                              │           │               │
                              │           v               │
                              │ ┌─────────────────┐       │
                              │ │ Android share   │       │
                              │ │ dialog opens    │       │
                              │ │ (WhatsApp,      │       │
                              │ │ Email, etc.)    │       │
                              │ └─────────────────┘       │
                              └───────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                        BEFORE vs AFTER                          │
├─────────────────────────────────────────────────────────────────┤
│ BEFORE: Click → Toast "Attachment clicked"                      │
│ AFTER:  Click → Opens in appropriate system app                 │
│         Long Press → Context menu with View/Save/Share options  │
└─────────────────────────────────────────────────────────────────┘
```

## Key Implementation Details

### File Organization
```
Pictures/LinguaSMS/          ← Image attachments  
Movies/LinguaSMS/            ← Video attachments
Downloads/LinguaSMS/         ← Other file types
```

### Error Handling
- No compatible app → Toast notification
- Save failure → Error toast with details  
- Invalid URI → Silent skip (no crash)
- Permission issues → Graceful fallback

### Android Version Support
- **Android 10+ (API 29+)**: Full MediaStore implementation
- **Android 9 and below**: Share option for saving
- **All versions**: View and Share functionality works

### MIME Type Support
- Images: image/jpeg, image/png, image/gif, etc.
- Videos: video/mp4, video/mov, video/avi, etc.  
- Audio: audio/mp3, audio/wav, audio/aac, etc.
- Documents: application/pdf, text/*, etc.
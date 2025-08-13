# UI Changes for Offline Translation

## Settings Activity Enhancement

The Settings activity now includes a new section for offline translation management:

```xml
<!-- Offline Translation Section -->
<TextView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginTop="24dp"
    android:text="@string/pref_category_offline_models"
    android:textSize="18sp"
    android:textStyle="bold" />

<TextView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginTop="8dp"
    android:text="@string/offline_models_description" />

<Button
    android:id="@+id/manage_offline_models_button"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_marginTop="8dp"
    android:text="@string/pref_manage_offline_models"
    android:backgroundTint="@color/colorPrimary"
    android:textColor="@android:color/white" />
```

## Offline Models Activity Layout

The offline models management screen provides:

1. **Enable/Disable Switch**: Toggle offline translation on/off
2. **Model List**: Shows all available language models
3. **Download Progress**: Real-time download progress for each model
4. **Model Status**: Clear indication of downloaded vs available models

### Key Components:

- **Toolbar**: With back navigation to Settings
- **Switch**: Enable/disable offline translation
- **RecyclerView**: List of available language models
- **Progress Bars**: Download progress indication
- **Action Buttons**: Download/Delete for each model

## User Experience Flow

```
Settings → Offline Translation → Manage Models → Download Languages → Translation Ready
    ↓              ↓                    ↓               ↓                  ↓
Main Menu    New Section         Model Management   Progress Tracking   Offline Ready
```

## Visual Layout Structure

```
┌─────────────────────────────────────┐
│ Settings Activity                   │
├─────────────────────────────────────┤
│ ┌─ Translation API Key ─────────────┐ │
│ │ [API Key Input]                  │ │
│ │ [Test API Key]                   │ │
│ └─────────────────────────────────────┘ │
│                                     │
│ ┌─ Language Settings ───────────────┐ │
│ │ [Select Incoming Language]       │ │
│ │ [Select Outgoing Language]       │ │
│ └─────────────────────────────────────┘ │
│                                     │
│ ┌─ Auto-Translate ──────────────────┐ │
│ │ Auto-translate SMS [Toggle]      │ │
│ └─────────────────────────────────────┘ │
│                                     │
│ ┌─ Theme ───────────────────────────┐ │
│ │ ○ Light ○ Dark ○ Black ○ System  │ │
│ └─────────────────────────────────────┘ │
│                                     │
│ ┌─ Offline Translation ─────────────┐ │ ← NEW SECTION
│ │ Download language models to      │ │
│ │ enable offline translation       │ │
│ │ [Manage Offline Models]          │ │ ← NEW BUTTON
│ └─────────────────────────────────────┘ │
│                                     │
│ [Save Settings]                     │
└─────────────────────────────────────┘
```

```
┌─────────────────────────────────────┐
│ Offline Models Activity             │
├─────────────────────────────────────┤
│ Enable Offline Translation [Toggle] │
├─────────────────────────────────────┤
│ Available Offline Models            │
├─────────────────────────────────────┤
│ ┌─ English (en) ──── 25MB [Download]┐ │
│ ┌─ Spanish (es) ──── 28MB [Download]┐ │
│ ┌─ French (fr) ───── 27MB [Delete] ┐ │
│ │ ████████████████░░░░ 80%         │ │ ← Download Progress
│ ┌─ German (de) ──── 30MB [Download]┐ │
│ ┌─ Italian (it) ──── 26MB [Download]┐ │
│ └─────────────────────────────────────┘ │
└─────────────────────────────────────┘
```

## String Resources

All necessary string resources have been added:

- `offline_models_description`: "Download language models to enable offline translation when internet is unavailable"
- `pref_category_offline_models`: "Offline Translation"
- `pref_manage_offline_models`: "Manage Offline Models"
- `model_download_success`: "Model downloaded successfully"
- `model_download_error`: "Error downloading model: %1$s"
- `model_delete_success`: "Model deleted successfully"

## Integration Points

1. **Settings Integration**: New section in existing settings layout
2. **Navigation**: Intent-based navigation to offline models activity
3. **Theme Support**: Uses existing app theming system
4. **Permission Handling**: No additional permissions required
5. **Manifest Registration**: Proper activity registration with parent relationship

The UI changes provide a seamless and intuitive way for users to manage offline translation capabilities within the existing app structure.
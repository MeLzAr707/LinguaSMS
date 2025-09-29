# LinguaSMS 🌍📱

**LinguaSMS** is an advanced Android SMS/MMS messaging application that breaks down language barriers through real-time message translation. Built with modern Android technologies, it provides a seamless messaging experience while offering powerful translation capabilities both online and offline.

## 📋 Features

### 🌐 Translation Features
- **Real-time Message Translation**: Automatically translate incoming and outgoing messages
- **Offline Translation**: Download language models for offline translation using ML Kit
- **Language Detection**: Smart language detection for incoming messages
- **60+ Languages Supported**: Support for major world languages through Google ML Kit
- **Manual Translation Control**: Choose when to translate specific messages

### 💬 Messaging Features
- **Full SMS/MMS Support**: Send and receive text messages and multimedia messages
- **Default SMS App**: Can be set as your device's default SMS application
- **Contact Integration**: Seamless contact management and avatar display
- **Conversation Threading**: Organized conversation view with message history
- **Message Search**: Find messages quickly with built-in search functionality

### 🎨 User Interface
- **Modern Material Design**: Clean, intuitive interface following Material Design principles
- **Dark Mode Support**: Full dark theme support for better viewing in low light
- **Custom Themes**: Customize app appearance with color themes
- **Attachment Support**: Send and receive images, videos, and other media files
- **Pinch-to-Zoom**: Zoom in on images and media within conversations

### 🔧 Advanced Features
- **Deep Sleep Message Handling**: Reliable message delivery even when device is in deep sleep
- **Custom Notification Tones**: Set custom notification sounds for different contacts
- **Message Backup**: Secure message storage and management
- **P2P Connectivity**: Direct device-to-device communication capabilities
- **Secret Messages**: Enhanced privacy features for sensitive communications

## 🚀 Installation

### Prerequisites
- Android 10 (API level 29) or higher
- Minimum 100MB storage space
- Internet connection for online translation and initial setup
- SMS/Phone permissions

### Installation Steps

1. **Download and Install**
   ```bash
   git clone https://github.com/MeLzAr707/LinguaSMS.git
   cd LinguaSMS
   ./gradlew assembleDebug
   ```

2. **Set Permissions**
   - Grant SMS read/write permissions
   - Allow phone and contacts access
   - Enable notification permissions
   - Optionally set as default SMS app

3. **Initial Setup**
   - Open the app and complete the setup wizard
   - Configure your translation preferences
   - Download offline language models (optional)

## 📖 Usage

### Basic Messaging
1. **Start a Conversation**: Tap the + button to start a new message
2. **Send Messages**: Type your message and tap send
3. **View Conversations**: All conversations appear on the main screen

### Translation Setup
1. **Access Settings**: Tap the menu button → Settings
2. **Configure Translation**: 
   - Enable auto-translate for incoming messages
   - Set your default language
   - Configure translation providers
3. **Offline Models**: Go to Settings → Manage Offline Models to download language packs

### Advanced Features
- **Long-press Messages**: Access translation, copy, and other options
- **Attachment Menu**: Tap the attachment button to send media
- **Search Messages**: Use the search icon to find specific messages
- **Contact Settings**: Long-press contacts for individual settings

## 🔧 Configuration

### Translation Settings
- **Auto-translate Incoming**: Automatically translate received messages
- **Default Language**: Set your primary language
- **Translation Confidence**: Adjust translation accuracy thresholds
- **Offline Priority**: Prefer offline translation when available

### Notification Settings
- **Custom Tones**: Set unique notification sounds per contact
- **Notification Styles**: Choose notification appearance and behavior
- **Privacy Settings**: Control what shows in notifications

### Theme Settings
- **Dark Mode**: Enable/disable dark theme
- **Custom Colors**: Choose accent colors and themes
- **Font Sizes**: Adjust text sizing for accessibility

## 🛠️ Technical Details

### Architecture
- **MVVM Pattern**: Clean architecture with ViewModel and LiveData
- **Android Jetpack**: Uses modern Android components
- **ML Kit Integration**: Google ML Kit for translation services
- **SQLite Database**: Local message and contact storage
- **Content Providers**: Standard Android SMS/MMS providers

### Key Components
- **MainActivity**: Main conversation list
- **ConversationActivity**: Individual conversation view
- **MessageService**: Background message handling
- **TranslationService**: Translation processing
- **OfflineModelManager**: Offline translation model management

### Dependencies
- **ML Kit Translate**: Offline translation capabilities
- **ML Kit Language ID**: Language detection
- **AndroidX Libraries**: Modern Android development
- **Material Components**: UI components
- **Glide**: Image loading and caching

## 🧪 Testing

### Manual Testing
See [MANUAL_TESTING_GUIDE.md](MANUAL_TESTING_GUIDE.md) for comprehensive testing procedures.

### Test Coverage
- Unit tests for core functionality
- Integration tests for translation services
- UI tests for critical user flows

## 📚 Documentation

### Implementation Guides
- [MMS Integration](MMS_INTEGRATION_README.md) - MMS handling implementation
- [ML Kit Offline](MLKIT_OFFLINE_IMPLEMENTATION.md) - Offline translation setup
- [Android 10+ MMS](ANDROID_10_MMS_IMPLEMENTATION_GUIDE.md) - Modern MMS compatibility
- [UI Changes](UI_CHANGES_DOCUMENTATION.md) - User interface documentation

### Feature Documentation
- [Attachment Features](ATTACHMENT_MENU_IMPLEMENTATION.md) - File attachment handling
- [Notification System](CUSTOM_NOTIFICATION_TONES_FEATURE.md) - Custom notifications
- [Manual Translation](MANUAL_DOWNLOAD_MODE_IMPLEMENTATION.md) - Manual translation controls

## 🤝 Contributing

### Development Setup
1. Fork the repository
2. Clone your fork locally
3. Create a feature branch
4. Make your changes with appropriate tests
5. Submit a pull request

### Code Style
- Follow Android development best practices
- Use meaningful variable and method names
- Add JavaDoc comments for public methods
- Maintain consistent indentation and formatting

### Testing Requirements
- Add unit tests for new functionality
- Update integration tests as needed
- Test on multiple Android versions
- Verify translation accuracy

## 📱 Compatibility

### Android Versions
- **Minimum**: Android 10 (API 29)
- **Target**: Android 14 (API 34)
- **Recommended**: Android 11+ for full feature support

### Device Requirements
- **RAM**: Minimum 2GB recommended
- **Storage**: 100MB for app + space for offline models
- **Network**: WiFi or mobile data for online translation
- **Hardware**: Camera for photo attachments (optional)

## 🔒 Privacy & Security

### Data Protection
- Messages stored locally on device
- No cloud storage of personal messages
- Encrypted local database
- Secure file handling for attachments

### Translation Privacy
- Offline models ensure privacy
- Online translation uses secure connections
- No message content stored by translation services
- User controls all translation settings

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙋‍♂️ Support

### Getting Help
- Check the [documentation](docs/) for detailed guides
- Review [troubleshooting guides](MMS_TROUBLESHOOTING_GUIDE.md)
- Search existing GitHub issues
- Create a new issue for bugs or feature requests

### Contact
- **GitHub Issues**: For bugs and feature requests
- **Discussions**: For questions and community support

## 🚀 Roadmap

### Upcoming Features
- Enhanced P2P messaging
- Voice message translation
- Group translation management
- Advanced message scheduling
- Cross-platform synchronization

### Performance Improvements
- Faster message loading
- Reduced battery usage
- Improved offline translation speed
- Better memory management

---

**LinguaSMS** - Breaking language barriers, one message at a time. 🌍✉️
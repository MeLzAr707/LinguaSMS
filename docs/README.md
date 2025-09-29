# LinguaSMS Documentation

This directory contains technical documentation for the LinguaSMS application.

## Overview

LinguaSMS is an Android SMS/MMS messaging application with real-time translation capabilities. The app uses modern Android development practices and provides both online and offline translation features.

## Documentation Index

### Implementation Guides
- [Contact Avatar Implementation](ContactAvatarImplementation.md) - Contact photo and avatar handling
- [Deep Sleep Message Handling](deep_sleep_message_handling.md) - Reliable message delivery during device sleep
- [Incoming Message Fix](incoming_message_fix.md) - Incoming SMS/MMS message processing
- [Notification Implementation](notification_implementation.md) - Push notification system
- [P2P Connection Implementation](p2p_connection_implementation.md) - Peer-to-peer messaging features

### Performance Optimizations

The app includes several performance optimizations for better user experience:

#### 1. Enhanced Caching
- Implemented `OptimizedMessageCache` with LRU caching for better memory management
- Added proper cache invalidation strategies
- Contact caching with intelligent expiration

#### 2. Batch Contact Lookup
- Created `OptimizedContactUtils` for efficient batch processing of contact lookups
- Reduced database queries by processing contacts in batches
- Asynchronous avatar loading to prevent UI blocking

#### 3. Pagination Support
- Added `PaginationUtils` for implementing pagination in RecyclerViews
- Created paginated message loading in `OptimizedMessageService`
- Improved user experience by loading messages in smaller batches

#### 4. RecyclerView Optimizations
- Implemented `MessageDiffCallback` for efficient RecyclerView updates using DiffUtil
- Reduced unnecessary view rebinding and layout passes
- Smooth infinite scrolling with memory-efficient pagination

#### 5. Background Processing
- Improved background processing with dedicated executors
- Reduced UI thread blocking
- Heavy operations moved off main thread

### Architecture Notes

#### Translation Architecture
- ML Kit integration for offline translation
- Language detection using ML Kit Language ID
- Fallback to online translation services
- Intelligent model management for offline capabilities

#### Database Optimization
- Indexed queries for faster message retrieval
- Optimized conversation loading with batch processing
- Efficient MMS content handling

## Development Guidelines

### Code Organization
- **ui/**: User interface components and activities
- **message/**: Core messaging functionality
- **translation/**: Translation services and language handling
- **contact/**: Contact management and avatar system
- **mms/**: MMS handling and media processing
- **system/**: System-level services and utilities

### Testing Strategy
- Unit tests for core business logic
- Integration tests for translation services
- Manual testing guides for complex user flows
- Performance testing for optimization validation

### Best Practices
- Use AndroidX components for modern Android development
- Implement proper lifecycle management in activities
- Handle background processing with WorkManager
- Use content providers for secure SMS/MMS access
- Follow Material Design guidelines for UI consistency

## Future Enhancements

### Planned Features
1. Enhanced group messaging support
2. Voice message translation
3. Advanced message scheduling
4. Cross-device synchronization
5. Improved P2P connectivity

### Performance Improvements
1. Kotlin migration for better concurrency
2. Room database adoption
3. Background message prefetching
4. Advanced caching strategies
5. UI optimization with Compose

## Contributing

When contributing to the documentation:
- Keep technical details accurate and up-to-date
- Include code examples where appropriate
- Update architecture diagrams when making structural changes
- Test documented procedures before publishing
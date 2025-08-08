# LinguaSMS Performance Optimization

This branch contains performance optimizations for the LinguaSMS app, focusing on improving the loading speed of conversations and messages.

## Key Optimizations

### 1. Enhanced Caching
- Implemented `OptimizedMessageCache` with LRU caching for better memory management
- Added proper cache invalidation strategies

### 2. Batch Contact Lookup
- Created `OptimizedContactUtils` for efficient batch processing of contact lookups
- Reduced database queries by processing contacts in batches

### 3. Pagination Support
- Added `PaginationUtils` for implementing pagination in RecyclerViews
- Created paginated message loading in `OptimizedMessageService`
- Improved user experience by loading messages in smaller batches

### 4. RecyclerView Optimizations
- Implemented `MessageDiffCallback` for efficient RecyclerView updates using DiffUtil
- Reduced unnecessary view rebinding and layout passes

### 5. Background Processing
- Improved background processing with dedicated executors
- Reduced UI thread blocking

## Implementation Details

### OptimizedMessageCache
- Uses Android's LruCache for efficient memory management
- Stores a copy of messages to prevent modification of cached data
- Provides methods for cache invalidation

### OptimizedContactUtils
- Implements batch contact lookup to reduce database queries
- Maintains a contact cache to avoid repeated lookups
- Processes contacts in smaller batches to avoid query size limitations

### PaginationUtils
- Provides a reusable pagination implementation for RecyclerViews
- Includes loading indicators and callbacks for loading more items
- Handles edge cases like loading state and empty results

### MessageDiffCallback
- Efficiently calculates the minimum number of changes needed to update the UI
- Compares messages by ID and content
- Handles special cases for MMS messages with attachments

### OptimizedMessageService
- Implements paginated message loading
- Uses a dedicated executor for database queries
- Provides callbacks for asynchronous message loading

## How to Use

### Pagination
```java
// In your activity or fragment
private static final int PAGE_SIZE = 50;
private int currentPage = 0;

private void setupPagination() {
    PaginationUtils.setupPagination(
        messagesRecyclerView,
        onLoadingComplete -> {
            // Load next page
            currentPage++;
            loadMessages(currentPage * PAGE_SIZE, PAGE_SIZE, onLoadingComplete);
        },
        10, // threshold
        loadingIndicator
    );
}

private void loadMessages(int offset, int limit, PaginationUtils.OnLoadingCompleteCallback callback) {
    optimizedMessageService.getMessagesByThreadIdPaginated(
        threadId,
        offset,
        limit,
        messages -> {
            // Update UI with new messages
            this.messages.addAll(messages);
            adapter.notifyItemRangeInserted(this.messages.size() - messages.size(), messages.size());
            
            // Signal that loading is complete
            callback.onLoadingComplete();
        }
    );
}
```

### Efficient RecyclerView Updates
```java
public void updateMessages(List<Message> newMessages) {
    DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
            new MessageDiffCallback(messages, newMessages));
    
    messages.clear();
    messages.addAll(newMessages);
    
    diffResult.dispatchUpdatesTo(this);
}
```

### Batch Contact Lookup
```java
// Get contact names for multiple phone numbers in one batch
Map<String, String> contactNames = OptimizedContactUtils.getContactNamesForNumbers(context, phoneNumbers);

// Apply contact names to messages
for (Message message : messages) {
    String address = message.getAddress();
    String contactName = contactNames.get(address);
    if (contactName != null) {
        message.setContactName(contactName);
    }
}
```

## Performance Impact

These optimizations should significantly improve the app's performance, especially for users with large message histories. Key improvements include:

1. Faster initial loading of conversations
2. Smoother scrolling in message lists
3. Reduced memory usage
4. Better battery efficiency
5. More responsive UI during data loading

## Next Steps

1. Convert the codebase to Kotlin for even better performance and conciseness
2. Implement Kotlin Coroutines for structured concurrency
3. Add background prefetching of conversations
4. Implement a more sophisticated caching strategy with disk caching
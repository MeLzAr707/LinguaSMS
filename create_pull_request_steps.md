# Steps to Create a Pull Request for Performance Optimizations

## 1. Fork the Repository (if you don't have write access)
1. Go to https://github.com/MeLzAr707/LinguaSMS
2. Click the "Fork" button in the top-right corner
3. Wait for the repository to be forked to your account

## 2. Clone the Repository
```bash
# If you forked the repository:
git clone https://github.com/YOUR_USERNAME/LinguaSMS.git

# If you have write access to the original repository:
git clone https://github.com/MeLzAr707/LinguaSMS.git
```

## 3. Create a New Branch
```bash
cd LinguaSMS
git checkout -b performance-optimization
```

## 4. Add the Optimized Files
Copy all the optimized files we've created to their respective locations in the repository:

### Core Classes
```bash
# Create directories if they don't exist
mkdir -p app/src/main/java/com/translator/messagingapp
mkdir -p app/src/main/res/layout
mkdir -p docs
```

Copy these files to their respective locations:
- `OptimizedMessageCache.java` → `app/src/main/java/com/translator/messagingapp/`
- `OptimizedContactUtils.java` → `app/src/main/java/com/translator/messagingapp/`
- `PaginationUtils.java` → `app/src/main/java/com/translator/messagingapp/`
- `MessageDiffCallback.java` → `app/src/main/java/com/translator/messagingapp/`
- `OptimizedMessageService.java` → `app/src/main/java/com/translator/messagingapp/`
- `OptimizedMainActivity.java` → `app/src/main/java/com/translator/messagingapp/`
- `OptimizedConversationActivity.java` → `app/src/main/java/com/translator/messagingapp/`
- `OptimizedTranslatorApp.java` → `app/src/main/java/com/translator/messagingapp/`
- `OptimizedMessageRecyclerAdapter.java` → `app/src/main/java/com/translator/messagingapp/`
- `PerformanceBenchmark.java` → `app/src/main/java/com/translator/messagingapp/`
- `item_loading.xml` → `app/src/main/res/layout/`
- `AndroidManifest.xml.optimized` → `app/src/main/`
- `performance_optimization_report.md` → `docs/`
- `optimization_summary.md` → `docs/`

## 5. Fix the build.gradle.kts File
Open the `app/build.gradle.kts` file and make sure it doesn't have the following line:
```kotlin
annotationProcessor(libs.compiler)
```

If it does, remove it and keep only:
```kotlin
annotationProcessor(libs.glide.compiler)
```

This will fix the "unresolved reference compiler" error.

## 6. Fix String Resources
Make sure the `item_loading.xml` file uses a string resource instead of a hardcoded string:

```xml
<!-- Change this: -->
<TextView
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_marginTop="4dp"
    android:text="Loading more messages..."
    android:textSize="12sp" />

<!-- To this: -->
<TextView
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_marginTop="4dp"
    android:text="@string/loading_messages"
    android:textSize="12sp" />
```

The string resource `loading_messages` is already defined in the strings.xml file as "Loading messages…".

## 7. Fix OptimizedConversationActivity
Update the `OptimizedConversationActivity.java` file to fix the MessageClickListener implementation:

1. Remove the `implements MessageRecyclerAdapter.MessageClickListener` from the class declaration:
```java
// Change this:
public class OptimizedConversationActivity extends BaseActivity implements MessageRecyclerAdapter.MessageClickListener {

// To this:
public class OptimizedConversationActivity extends BaseActivity {
```

2. Create an anonymous implementation of the interface when creating the adapter:
```java
adapter = new MessageRecyclerAdapter(this, messages, new MessageRecyclerAdapter.MessageClickListener() {
    @Override
    public void onMessageClick(Message message) {
        // Handle message click
        Toast.makeText(OptimizedConversationActivity.this, "Message clicked", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onMessageLongClick(Message message) {
        // Handle message long click
        showMessageOptions(message);
    }
});
```

3. Add a simple implementation for the `showMessageOptions` method:
```java
private void showMessageOptions(Message message) {
    // Implementation for showing message options
    Toast.makeText(this, "Message options", Toast.LENGTH_SHORT).show();
}
```

This will fix the "cannot find symbol MessageClickListener" compiler error.

## 8. Commit the Changes
```bash
git add .
git commit -m "Add comprehensive performance optimizations"
```

## 9. Push the Branch
```bash
git push origin performance-optimization
```

## 10. Create the Pull Request
1. Go to the repository on GitHub (either your fork or the original)
2. You should see a prompt to create a pull request from your recently pushed branch
3. Click on "Compare & pull request"
4. Use the content from `pull_request.md` as the description
5. Click "Create pull request"

## 11. Additional Information for the Pull Request
- **Title**: "Add comprehensive performance optimizations"
- **Description**: Copy the content from `pull_request.md`
- **Reviewers**: Add appropriate team members if known
- **Labels**: Add "enhancement" and "performance" if available

## 12. After Creating the Pull Request
- Respond to any feedback or questions from reviewers
- Make additional commits to the same branch if changes are requested
- These commits will automatically be added to the pull request
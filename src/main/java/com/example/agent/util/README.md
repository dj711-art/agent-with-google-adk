# GenAI Retry Helper

## Overview

The `GenAiRetryHelper` class provides automatic retry logic for handling transient errors when interacting with Google's GenAI API.

## Supported Error Codes

The helper automatically retries on:
- **429 (Too Many Requests)**: Rate limiting errors
- **503 (Service Unavailable)**: Model overloaded errors

## Features

- Exponential backoff with jitter for retries
- Respects `Retry-After` headers when provided by the API
- Configurable retry attempts and base delay
- Detailed logging of retry attempts

## Usage

### Basic Example

```java
import com.example.agent.util.GenAiRetryHelper;
import java.util.concurrent.Callable;

// Wrap your GenAI API call in a Callable
Callable<String> task = () -> {
    // Your GenAI API call here
    return someGenAiApiCall();
};

// Call with retry logic
// Parameters: task, maxRetries (3), baseDelayMillis (1000ms = 1 second)
String result = GenAiRetryHelper.callWithRetry(task, 3, 1000L);
```

### Configuration Parameters

- `maxRetries`: Maximum number of retry attempts (excluding the initial try)
- `baseDelayMillis`: Base delay in milliseconds for exponential backoff

### Retry Behavior

1. **Initial attempt**: The task is executed immediately
2. **On 429 or 503 error**: The helper checks if retries are available
3. **Delay calculation**:
   - If a `Retry-After` header is present, it's honored
   - Otherwise, exponential backoff with jitter is used: `delay = baseDelay * 2^(attempt-1) + jitter`
4. **Retry**: After the delay, the task is retried
5. **Success or failure**: Returns the result on success, or throws the exception if max retries exceeded

### Example with Custom Configuration

```java
// Retry up to 5 times with a base delay of 2 seconds
String result = GenAiRetryHelper.callWithRetry(() -> {
    return genAiClient.generateContent(prompt);
}, 5, 2000L);
```

## Error Handling

- Non-retriable errors (other than 429 and 503) are immediately thrown
- When max retries are exhausted, the last exception is thrown
- All retry attempts are logged at INFO level

## Notes

- The ADK framework may handle some retries internally
- This helper is useful for direct GenAI API calls or as an additional safety layer
- Jitter is added to prevent thundering herd problems when multiple clients retry simultaneously

# Prerequisites
* Java 17 or above
* Maven 3.9 or above
# Configuration
* Configuratoins
```declarative
GOOGLE_GENAI_USE_VERTEXAI=FALSE
GOOGLE_API_KEY="Your API Key"
FOOTBALL_API_KEY="Your football-data.org API Key"
```
FOOTBALL_API_KEY can be obtained from https://www.football-data.org/

# How to run it?
* Put your API key in .env file
```declarative
source .env
```
* execute run.sh script
```declarative
./run.sh
```
* open http://localhost:8000 in your browser
Then select football_agent from the list of agents at the left corner.
![English Premier League Standings](/docs/EPL.png)
![Get latest result of a team](/docs/LatestResultForATeam.png)
![Get latest result from the beginning](/docs/EPLLatestResultsFromBeginning.png)
![Get latest result most recent](/docs/EPl_latest_result.png)
![Get next match of a team](/docs/GetFixtureLFC.png)
![Analysis of a team performance](/docs/Analysis.png)
* Get Team squad
![Get Team Squad](/docs/TeamSquad.png)
* Match analysis
![Match Analysis](/docs/MatchAnalysis.png)
# Sample prompts
* "EPL"
* "Get latest result"
* "Get latest result of Manchester United"
* "Get next match of Liverpool"

# Improvements
* Add more leagues
* Improve prompt engineering for better results
* More analysis

# Error Handling

## Model Overload (503 Errors)

The Google GenAI API may occasionally return 503 errors when the model is overloaded. This project includes a retry helper utility (`GenAiRetryHelper`) that can handle these transient errors with exponential backoff.

### Using the Retry Helper

For direct GenAI API calls, you can wrap them with the retry logic:

```java
import com.example.agent.util.GenAiRetryHelper;

String result = GenAiRetryHelper.callWithRetry(() -> {
    // Your GenAI API call
    return genAiClient.generateContent(prompt);
}, 3, 1000L); // 3 retries, 1 second base delay
```

See [GenAiRetryHelper README](src/main/java/com/example/agent/util/README.md) for more details.

### ADK Framework Limitations

The ADK framework (InMemoryRunner) handles LLM calls internally and may not automatically retry on 503 errors. If you encounter frequent 503 errors:
- Consider upgrading to a higher-tier API plan with better quota
- Use the retry helper for custom GenAI interactions
- Implement error handling at the application level to catch and retry failed agent runs



 
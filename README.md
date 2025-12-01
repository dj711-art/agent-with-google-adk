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
Then select football_agent from the list of agents at the left corner.
![English Premier League Standings](/docs/EPL.png)
![Get latest result of a team](/docs/LatestResultForATeam.png)
![Get latest result from the beginning](/docs/EPLLatestResultsFromBeginning.png)
![Get latest result most recent](/docs/EPl_latest_result.png)
![Get next match of a team](/docs/GetFixtureLFC.png)
![Analysis of a team performance](/docs/Analysis.png)
# Sample prompts
* "EPL"
* "Get latest result"
* "Get latest result of Manchester United"
* "Get next match of Liverpool"

# Improvements
* Add more leagues
* Improve prompt engineering for better results
* More analysis



 
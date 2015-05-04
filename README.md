slack-youtrack
==============

Integration of slack and you track.

Main functionality here is change management:

* show ticket changes
* linkify references to issues
* map the changes to the appropriate channel

Of course, to do this we have an agent being woken up regularly that:

1. goes to the RSS feed
1. for each item found there, calls into the REST interface to get changes
1. formats the change info
1. posts it to the channel

Installation and configuration
------------
1. List of required properties
    * YOUTRACK_USERNAME - YouTrack username
    * YOUTRACK_PASSWORD - YouTrack password
    * SLACK_AUTH_TOKEN - token for authentication to Slack REST services
    * APP_DATA_DIR - directory where app will store it's data-files (configuration)
    * YOUTRACK_HOST - YouTrack server host
    * YOUTRACK_PORT - YouTrack server port
    * ISSUE_HISTORY_WINDOW - Time In minutes - how deep should we look for issues in the past. If set to 10, it means that issues and changes that happened not longer than 10 minutes will be posted to chat server
2. Run "mvn -DYOUTRACK_USERNAME=user -DYOUTRACK_PASSWORD=pwd ... package" to build war file
3. Drop war file into servlet container "webapps" directory

That's it.

Troubleshooting
------------

If you experience any problems, e.g. YouTrack updates are not posted to Slack channel, rebuild the project setting http client log level to DEBUG (so that all requests and responses are logged), redeploy and feel free to file an issue with information from the log. To set the log level to DEBUG, edit [src/main/resources/logback.xml](https://github.com/ontometrics/slack-youtrack/blob/master/src/main/resources/logback.xml) and uncomment lines

```xml
<logger name="org.apache.http" level="DEBUG" />
```

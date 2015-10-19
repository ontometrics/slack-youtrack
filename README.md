slack-youtrack
==============

Integration of slack and you track.

Main functionality here is change management:

* show ticket changes
* linkify references to issues
* map the changes to the appropriate channel

Of course, to do this we have an agent being woken up regularly that:

1. Request list of updated issues for each project
1. for each item found there, calls into the REST interface to get changes
1. formats the change info
1. posts it to the channel
1. load list of youtrack projects sometimes
1. Retrieve AccessToken from HUB (for "hub-oauth2" type of authentication)

Installation and configuration
------------
1. List of required properties
    * SLACK_WEBHOOK_PATH - path for Slack webhook excluding first slash. e.g. "services/T00000000/B00000000/XXXXXXXXXXXXXXXXXXXXXXXX" (see https://api.slack.com/incoming-webhooks)
    * YOUTRACK_URL - YouTrack server url. e.g. "https://mycompany.com/youtrack". This can be inner URL (for example local IP-based)
    * YOUTRACK_EXTERNAL_URL - YouTrack server external URL. It should be accessible for Slack Users, so consider it to be public DNS-based URL. This can be the same as YOUTRACK_URL however.
    * YOUTRACK_TO_SLACK_CHANNELS - mappings between YouTrack projects and Slack channels. For example "APL->#apple;SUP->#support" (please note that "#" or "@" should be included in the slack-channel name
    * DEFAULT_SLACK_CHANNEL - Default slack channel to post in. e.g. "#general". Projects without mappings will be posted here
    * AUTH_TYPE - authentication / authorization method. Available values: {"credentials", "hub-oauth2"}
    
    For "credentials"  authentication type such properties have to be created
    
    * YOUTRACK_USERNAME - YouTrack username
    * YOUTRACK_PASSWORD - YouTrack password
    * SLACK_WEBHOOK_PATH - path for Slack webhook excluding first slash. e.g. "services/T00000000/B00000000/XXXXXXXXXXXXXXXXXXXXXXXX"

    For "hub-oauth2" authentication type (Hub authentication OAuth 2.0) following properties are required. 
    See https://www.jetbrains.com/hub/help/1.0/OAuth-2.0-Authorization.html
    
    * HUB_URL - JetBrains Hub url e.g. "https://mycompany.com/hub"
    * HUB_OAUTH_RESOURCE_SERVER_SERVICE_ID - Hub OAuth resource server service id
    * HUB_OAUTH_CLIENT_SERVICE_ID - Hub OAuth clientServiceId
    * HUB_OAUTH_CLIENT_SERVICE_SECRET - Hub OAuth clientServiceSecret    DEFAULT_SLACK_CHANNEL - Default slack channel to post in. e.g. "#general"

    Generic attributes
    * ISSUE_HISTORY_WINDOW - Time in minutes - how deep should we look for issues in the past. If set to 10, it means that issues and changes that happened not longer than 10 minutes will be posted to chat server
    * APP_DATA_DIR - directory where app will store it's data-files (configuration). e.g. "/opt/slack-youtrack"
    * SLACKBOT_ICON - URL of icon used for the posts in the YouTrack channel

2. Create maven profile with described properties or directly define them like below

Run "mvn -DYOUTRACK_USERNAME=usr -DYOUTRACK_PASSWORD=pwd -DSLACK_WEBHOOK_PATH=services/T00000000/B00000000/XXXXXXXXXXXXXXXXXXXXXXXX -DAPP_DATA_DIR=/opt/slack-youtrack -DYOUTRACK_URL=http://company.myjetbrains.com/youtrack -DISSUE_HISTORY_WINDOW=10 -DDEFAULT_SLACK_CHANNEL=#general package" to build war file

3. Drop war file into servlet container "webapps" directory

That's it.

Troubleshooting
------------

If you experience any problems, e.g. YouTrack updates are not posted to Slack channel, rebuild the project setting http client log level to DEBUG (so that all requests and responses are logged), redeploy and feel free to file an issue with information from the log. To set the log level to DEBUG, edit [src/main/resources/logback.xml](https://github.com/ontometrics/slack-youtrack/blob/master/src/main/resources/logback.xml) and uncomment lines

```xml
<logger name="org.apache.http" level="DEBUG" />
<logger name="com.ontometrics.integration.youtrack.response" level="DEBUG" />
```

slack-youtrack
==============

Integration of slack and you track.

Main functionality here is change management:

* show ticket changes
* linkify references to issues
* map the changes to the appropriate channel

Of course, to do this we have an agent being woken up regularly that:

# goes to the RSS feed
# for each item found there, calls into the REST interface to get changes
# formats the change info
# posts it to the channel

That's it.

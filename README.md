# AODN Status Page
Script and files to produce a user-facing status page for the AODN's main services (currently proof-of-concept)

### Proof of Concept
- [&#10003;] Can access stausJson from the wide world?
- [&#10003;] Generate regularly
- [&#10003;] Sample page output
- [&#10003;] Javascript modal when page needs refreshing
- [&#10003;] Display age of information
- [&#10003;] Upcoming outages information
- [&nbsp;&nbsp;] Harvest current service data from Nagios
- [&nbsp;&nbsp;] Harvest planned maintenance info from Nagios

### First prod release
- [&nbsp;&nbsp;] Choose which services to display (and descriptions)
- [&nbsp;&nbsp;] Branding / legal words
- [&nbsp;&nbsp;] Where to host? (Probably Amazon)

### Future work
- [&nbsp;&nbsp;] Favicon represents status (green icon when all good, yellow or red when some services are having issues)
- [&nbsp;&nbsp;] Combine output fron *n* Nagios instances
- [&nbsp;&nbsp;] How to handle discrepancies between those instances?

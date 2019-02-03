# User actions collector

### Setup
MacOS

```
brew install influxdb
influxd
influx

run query 
CREATE DATABASE analytics
```

Our data is classic example of time-series: 95% of data points with 95% of reads
for data for the latest hour, thus it would be ideal to use database specifically build for such workloads.

There are several options on the market including OpenTSDB, TimescaleDB, influxDB, Cassandra. Taking into consideration 
context of the assignment I was looking for solution with following characteristics:


* Built-in clustering mechanism as volume of data potentially can be huge 
* Fast inserts and data retrieval
* Tested in production


It seems Cassandra checks all the marks. It is distributes by design, it is being successfully used
by Netflix. It has great integration with JVM based languages, since it is written in Java. However there is 
a drawback which considering context of the project (namely time constraint) makes it not a great fit. It is well known
that Cassandra takes some effort to configure it properly. Therefore influxDB was chosen. It is easy to setup and get
running, though unfortunately clustering mechanism is locked behind paid version.


At the core of the application are two actors talking to underlying storage: one actor writes data,
another aggregates it and presents in our business-defined format. Both actors wrapped into RoundRobinPool which is
configurable via application.conf. There is another actor - Supervisor. It watches pool of writer actors and shutdowns 
ActorSystem once all messages are processed. Full mechanism works like this 

`Broadcast PoisonPill -> Writer Actors stop recieving new messages  but continue processing messages remaining in their mailboxes
until PosionPill is encountered -> Once all wrtier actors in a poll stop itself pool terminates and send Terminated message
to supervisor -> Supervisor actror shutdowns system` 

 
Since no part of our application has state and we ensure that all outstanding work will be completed before shutdown it is easy to scale number of running instances without affecting 
data consistency
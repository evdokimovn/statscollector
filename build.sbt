name := "Tapad"

version := "0.1"

scalaVersion := "2.12.8"

resolvers += "Spark Packages Repo" at "https://dl.bintray.com/spark-packages/maven"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.5.20"
libraryDependencies += "datastax" % "spark-cassandra-connector" % "2.4.0-s_2.11"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % "10.1.7",
  "com.typesafe.akka" %% "akka-http-testkit" % "10.1.7" % Test
)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % "2.5.20",
  "com.typesafe.akka" %% "akka-stream-testkit" % "2.5.20" % Test
)

libraryDependencies += "com.paulgoldbaum" %% "scala-influxdb-client" % "0.6.1"

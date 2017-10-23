
import com.typesafe.sbt.packager.docker._

organization := "lightbend"
name := "akka-customer"
version := "0.0.1"

scalaVersion := "2.12.2"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-persistence-cassandra" % "0.56" exclude("com.google.guava", "guava"),
  "org.projectlombok" % "lombok" % "1.16.10" % "compile",
  "com.typesafe.akka" %% "akka-cluster" % "2.5.6",
  "com.typesafe.akka" %% "akka-cluster-tools" % "2.5.6",
  "com.typesafe.akka" %% "akka-cluster-sharding" % "2.5.6",
  "com.typesafe.akka" %% "akka-cluster-metrics" % "2.5.6",
  "com.typesafe.akka" %% "akka-http" % "10.0.10",
  "com.typesafe.akka" %% "akka-http-jackson" % "10.0.10",
  "com.google.guava"  % "guava" % "23.0",
  "com.github.romix.akka" %% "akka-kryo-serialization" % "0.5.1",
  "junit" % "junit" % "4.12" % "test",
  "com.typesafe.akka" %% "akka-testkit" % "2.5.6" % "test",
  "com.typesafe.akka" %% "akka-http-testkit" % "10.0.10" % "test",
  "com.typesafe.akka" %% "akka-persistence-cassandra-launcher" % "0.58" % "test"
)

licenses := Seq(("CC0", url("http://creativecommons.org/publicdomain/zero/1.0")))

resolvers += "krasserm at bintray" at "http://dl.bintray.com/krasserm/maven"

enablePlugins(JavaAppPackaging)

dockerEntrypoint ++= Seq(
  """-Dcassandra-journal.contact-points.0="$CASSANDRA_SERVICE_NAME"""",
  """-Dhttp.address="$CUSTOMERSERVICE_BIND_IP"""",
  """-Dhttp.port="$CUSTOMERSERVICE_BIND_PORT"""",
  """-Dakka.actor.provider=cluster""",
  """-Dakka.remote.netty.tcp.hostname="$(eval "echo $AKKA_REMOTING_BIND_HOST")"""",
  """-Dakka.remote.netty.tcp.port="$AKKA_REMOTING_BIND_PORT"""",
  """$(IFS=','; I=0; for NODE in $AKKA_SEED_NODES; do echo "-Dakka.cluster.seed-nodes.$I=akka.tcp://$AKKA_ACTOR_SYSTEM_NAME@$NODE"; I=$(expr $I + 1); done)""",
  "-Dakka.io.dns.resolver=async-dns",
  "-Dakka.io.dns.async-dns.resolve-srv=true",
  "-Dakka.io.dns.async-dns.resolv-conf=on "
)

dockerCommands :=
  dockerCommands.value.flatMap {
    case ExecCmd("ENTRYPOINT", args @ _*) => Seq(Cmd("ENTRYPOINT", args.mkString(" ")))
    case v => Seq(v)
  }

dockerRepository := Some("lightbend")

dockerExposedPorts := Seq(9000, 2551)

maintainer := "Eric Murphy <eric.murphy@lightbend.com>"
// dockerBaseImage := "openjdk:8-jre-alpine" // TODO: Bash not installed Alpine
dockerUpdateLatest := true
name := """scalike-sample"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws,
  "org.scalikejdbc" %% "scalikejdbc"       % "2.0.5",
  "org.scalikejdbc" %% "scalikejdbc-play-plugin"  % "2.3.0",
  "com.h2database"  %  "h2"                % "1.4.180"
)

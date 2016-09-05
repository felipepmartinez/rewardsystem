name := "desafionubank"

version := "1.0"

scalaVersion := "2.11.8"

lazy val versions = new {
  val finatra = "2.2.0"
  val guice = "4.0"
  val logback = "1.1.7"
  val slick = "3.1.1"
  val h2 = "1.4.191"
  val hikaricp = "2.3.3"
  val async = "0.9.6-RC2"
}

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  "Twitter Maven" at "https://maven.twttr.com"
)

libraryDependencies ++= Seq(
  "com.twitter" %% "finatra-http" % versions.finatra,
  "com.twitter" %% "finatra-httpclient" % versions.finatra,
  "ch.qos.logback" % "logback-classic" % versions.logback,

  "org.scala-lang.modules" % "scala-async_2.11" % versions.async,

  "com.zaxxer" % "HikariCP" % versions.hikaricp,

  "com.typesafe.slick" %% "slick-hikaricp" % versions.slick,
  "com.typesafe.slick" %% "slick" % versions.slick,
  "com.h2database" % "h2" % versions.h2,

  "org.json4s" %% "json4s-native" % "3.4.0",

  "com.twitter" %% "finatra-http" % versions.finatra % "test",
  "com.twitter" %% "finatra-jackson" % versions.finatra % "test",
  "com.twitter" %% "inject-server" % versions.finatra % "test",
  "com.twitter" %% "inject-app" % versions.finatra % "test",
  "com.twitter" %% "inject-core" % versions.finatra % "test",
  "com.twitter" %% "inject-modules" % versions.finatra % "test",
  "com.google.inject.extensions" % "guice-testlib" % versions.guice % "test",

  "com.twitter" %% "finatra-http" % versions.finatra % "test" classifier "tests",
  "com.twitter" %% "finatra-jackson" % versions.finatra % "test" classifier "tests",
  "com.twitter" %% "inject-server" % versions.finatra % "test" classifier "tests",
  "com.twitter" %% "inject-app" % versions.finatra % "test" classifier "tests",
  "com.twitter" %% "inject-core" % versions.finatra % "test" classifier "tests",
  "com.twitter" %% "inject-modules" % versions.finatra % "test" classifier "tests",

  "org.mockito" % "mockito-core" % "1.9.5" % "test",
  "org.scalatest" %% "scalatest" % "2.2.3" % "test",
  "org.specs2" %% "specs2" % "2.3.12" % "test",
  "com.novocode" % "junit-interface" % "0.11" % Test)

val scala3Version = "3.7.0"

lazy val root = project
  .in(file("."))
  .settings(
    name := "zio-graphql-example",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,

    libraryDependencies += "org.scalameta" %% "munit" % "1.1.1" % Test,
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % "2.1.18",
      "dev.zio" %% "zio-streams" % "2.1.18",
      "dev.zio" %% "zio-http" % "3.3.3",
      "com.github.ghostdogpr" %% "caliban" % "2.10.0",
      "com.github.ghostdogpr" %% "caliban-zio-http" % "2.10.0",
      "com.github.ghostdogpr" %% "caliban-quick" % "2.10.0",
      "com.github.jwt-scala" %% "jwt-circe" % "10.0.0",
      "io.circe" %% "circe-core" % "0.14.6",
      "io.circe" %% "circe-parser" % "0.14.6",
      "io.circe" %% "circe-generic" % "0.14.6"
    )
  )

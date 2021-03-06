import com.scalapenos.sbt.prompt.SbtPrompt.autoImport._
import com.scalapenos.sbt.prompt._
import Dependencies._
import microsites.ExtraMdFileConfig

name := """redis4cats-root"""

organization in ThisBuild := "dev.profunktor"

scalaVersion in ThisBuild := "2.13.1"
crossScalaVersions in ThisBuild := Seq(scalaVersion.value, "2.12.10")

sonatypeProfileName := "dev.profunktor"

// Needed to not run out of memory (Metaspace) when running test suite
ThisBuild / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.ScalaLibrary

promptTheme := PromptTheme(
  List(
    text("[sbt] ", fg(105)),
    text(_ => "redis4cats", fg(15)).padRight(" λ ")
  )
)

def pred[A](p: Boolean, t: => Seq[A], f: => Seq[A]): Seq[A] =
  if (p) t else f

def version(strVersion: String): Option[(Long, Long)] = CrossVersion.partialVersion(strVersion)

val commonSettings = Seq(
  organizationName := "Redis client for Cats Effect & Fs2",
  startYear := Some(2018),
  licenses += ("Apache-2.0", new URL("https://www.apache.org/licenses/LICENSE-2.0.txt")),
  homepage := Some(url("https://redis4cats.profunktor.dev/")),
  headerLicense := Some(HeaderLicense.ALv2("2018-2020", "ProfunKtor")),
  libraryDependencies ++= Seq(
        CompilerPlugins.betterMonadicFor,
        CompilerPlugins.contextApplied,
        CompilerPlugins.kindProjector,
        Libraries.catsEffect,
        Libraries.redisClient,
        Libraries.scalaCheck    % Test,
        Libraries.scalaTest     % Test,
        Libraries.catsLaws      % Test,
        Libraries.catsTestKit   % Test,
        Libraries.catsTestKitST % Test
      ),
  resolvers += "Apache public" at "https://repository.apache.org/content/groups/public/",
  scalacOptions ++= pred(
        version(scalaVersion.value) == Some(2, 12),
        t = Seq("-Xmax-classfile-name", "80"),
        f = Seq.empty
      ),
  scalafmtOnCompile := true,
  scmInfo := Some(ScmInfo(url("https://github.com/profunktor/redis4cats"), "scm:git:git@github.com:profunktor/redis4cats.git")),
  publishTo := {
    val sonatype = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at sonatype + "content/repositories/snapshots")
    else
      Some("releases" at sonatype + "service/local/staging/deploy/maven2")
  },
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ =>
    false
  },
  pomExtra :=
      <developers>
        <developer>
          <id>gvolpe</id>
          <name>Gabriel Volpe</name>
          <url>https://github.com/gvolpe</url>
        </developer>
      </developers>
)

lazy val noPublish = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false,
  skip in publish := true
)

lazy val `redis4cats-root` = project
  .in(file("."))
  .aggregate(
    `redis4cats-core`,
    `redis4cats-effects`,
    `redis4cats-streams`,
    `redis4cats-log4cats`,
    examples,
    tests,
    microsite
  )
  .settings(noPublish)

lazy val `redis4cats-core` = project
  .in(file("modules/core"))
  .settings(commonSettings: _*)
  .settings(parallelExecution in Test := false)
  .enablePlugins(AutomateHeaderPlugin)

lazy val `redis4cats-log4cats` = project
  .in(file("modules/log4cats"))
  .settings(commonSettings: _*)
  .settings(libraryDependencies += Libraries.log4CatsCore)
  .settings(parallelExecution in Test := false)
  .enablePlugins(AutomateHeaderPlugin)
  .dependsOn(`redis4cats-core`)

lazy val `redis4cats-effects` = project
  .in(file("modules/effects"))
  .settings(commonSettings: _*)
  .settings(parallelExecution in Test := false)
  .enablePlugins(AutomateHeaderPlugin)
  .dependsOn(`redis4cats-core`)

lazy val `redis4cats-streams` = project
  .in(file("modules/streams"))
  .settings(commonSettings: _*)
  .settings(libraryDependencies += Libraries.fs2Core)
  .settings(parallelExecution in Test := false)
  .enablePlugins(AutomateHeaderPlugin)
  .dependsOn(`redis4cats-core`)

lazy val examples = project
  .in(file("modules/examples"))
  .settings(commonSettings: _*)
  .settings(noPublish)
  .settings(libraryDependencies += Libraries.log4CatsSlf4j)
  .settings(libraryDependencies += Libraries.logback % "runtime")
  .enablePlugins(AutomateHeaderPlugin)
  .dependsOn(`redis4cats-log4cats`)
  .dependsOn(`redis4cats-effects`)
  .dependsOn(`redis4cats-streams`)

lazy val tests = project
  .in(file("modules/tests"))
  .settings(commonSettings: _*)
  .settings(noPublish)
  .enablePlugins(AutomateHeaderPlugin)
  .dependsOn(`redis4cats-core`)
  .dependsOn(`redis4cats-effects`)
  .dependsOn(`redis4cats-streams`)

lazy val microsite = project
  .in(file("site"))
  .enablePlugins(MicrositesPlugin)
  .settings(commonSettings: _*)
  .settings(noPublish)
  .settings(
    micrositeName := "Redis4Cats",
    micrositeDescription := "Redis client for Cats Effect & Fs2",
    micrositeAuthor := "ProfunKtor",
    micrositeGithubOwner := "profunktor",
    micrositeGithubRepo := "redis4cats",
    micrositeBaseUrl := "",
    micrositeExtraMdFiles := Map(
          file("README.md") -> ExtraMdFileConfig(
                "index.md",
                "home",
                Map("title" -> "Home", "position" -> "0")
              ),
          file("CODE_OF_CONDUCT.md") -> ExtraMdFileConfig(
                "CODE_OF_CONDUCT.md",
                "page",
                Map("title" -> "Code of Conduct")
              )
        ),
    micrositeExtraMdFilesOutput := (resourceManaged in Compile).value / "jekyll",
    micrositeGitterChannel := true,
    micrositeGitterChannelUrl := "profunktor-dev/redis4cats",
    micrositePushSiteWith := GitHub4s,
    micrositeGithubToken := sys.env.get("GITHUB_TOKEN"),
    scalacOptions --= Seq(
          "-Werror",
          "-Xfatal-warnings",
          "-Ywarn-unused-import",
          "-Ywarn-numeric-widen",
          "-Ywarn-dead-code",
          "-deprecation",
          "-Xlint:-missing-interpolator,_"
        )
  )
  .dependsOn(`redis4cats-effects`, `redis4cats-streams`, `examples`)

// CI build
addCommandAlias("buildRedis4Cats", ";clean;+test;mdoc")

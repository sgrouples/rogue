// Copyright 2012 Foursquare Labs Inc. All Rights Reserved.
import sbt._
import Keys._

object RogueBuild extends Build {
  override lazy val projects =
    Seq(all, index, core, lift)

  lazy val all: Project = Project("all", file(".")) aggregate(
    index, core, lift)

  lazy val index = Project("rogue-index", file("rogue-index/")) dependsOn()
  lazy val core = Project("rogue-core", file("rogue-core/")) dependsOn(index % "compile;test->test;runtime->runtime")
  lazy val lift = Project("rogue-lift", file("rogue-lift/")) dependsOn(core % "compile;test->test;runtime->runtime")
  lazy val IvyDefaultConfiguration = config("default") extend(Compile)

  lazy val defaultSettings: Seq[Setting[_]] = Seq(
    version := "2.5.1-MongoAsync-10",
    organization := "com.foursquare",
    scalaVersion := "2.11.8",
    publishMavenStyle := true,
    publishArtifact in Test := false,
    pomIncludeRepository := { _ => false },
    publishTo <<= (version) { v =>
      val nexus = "https://nexus.groupl.es/"
      if (v.endsWith("-SNAPSHOT"))
        Some("snapshots" at nexus+"repository/maven-snapshots/")
      else
        Some("releases" at nexus+"repository/maven-releases/")
    },
    //publishTo := Some(Resolver.file("mvn-repo", new File(Path.userHome + "/git/mvn-repo/"))),
    pomExtra := (
      <url>http://github.com/foursquare/rogue</url>
      <licenses>
        <license>
          <name>Apache</name>
          <url>http://www.opensource.org/licenses/Apache-2.0</url>
          <distribution>repo</distribution>
        </license>
      </licenses>
      <scm>
        <url>git@github.com:foursquare/rogue.git</url>
        <connection>scm:git:git@github.com:foursquare/rogue.git</connection>
      </scm>
      <developers>
        <developer>
          <id>jliszka</id>
          <name>Jason Liszka</name>
          <url>http://github.com/jliszka</url>
        </developer>
      </developers>),
    resolvers ++= Seq(
        "Bryan J Swift Repository" at "http://repos.bryanjswift.com/maven2/",
        "Releases" at "http://oss.sonatype.org/content/repositories/releases",
        "Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots"),
    retrieveManaged := true,
    ivyConfigurations += IvyDefaultConfiguration,
    scalacOptions ++= Seq("-deprecation", "-unchecked"),
    scalacOptions <++= scalaVersion map { scalaVersion =>
        scalaVersion.split('.') match {
            case Array(major, minor, _*) if major.toInt >= 2 && minor.toInt >= 10 => Seq("-feature", "-language:_")
            case _ => Seq()
        }
    },

    // Hack to work around SBT bug generating scaladoc for projects with no dependencies.
    // https://github.com/harrah/xsbt/issues/85
    unmanagedClasspath in Compile += Attributed.blank(new java.io.File("doesnotexist")),

    testFrameworks += new TestFramework("com.novocode.junit.JUnitFrameworkNoMarker"),
    credentials += Credentials(Path.userHome / ".ivy2" / ".meweCredentials")
    )
}

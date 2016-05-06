libraryDependencies <++= (scalaVersion) { scalaVersion =>
  val specsVersion = "2.4.2"
  val liftVersion = "2.6.2-MongoAsync"
  Seq(
    "com.foursquare"           %% "rogue-field"     % "2.4.0"      % "compile",
    "net.liftweb"              %% "lift-mongodb"    % liftVersion  % "compile" intransitive(),
    "net.liftweb"              %% "lift-common"     % liftVersion  % "compile",
    "net.liftweb"              %% "lift-json"       % liftVersion  % "compile",
    "net.liftweb"              %% "lift-util"       % liftVersion  % "compile",
    "joda-time"                % "joda-time"           % "2.9.1"        % "provided",
    "org.joda"                 % "joda-convert"        % "1.8.1"        % "provided",
    "org.mongodb"              % "mongodb-driver"      % "3.2.2"     % "compile",
    "org.mongodb"              % "mongodb-driver-async"% "3.2.2"     % "compile",
    "junit"                    % "junit"               % "4.5"        % "test",
    "com.novocode"             % "junit-interface"     % "0.6"        % "test",
    "ch.qos.logback"           % "logback-classic"     % "1.0.9"     % "provided",
    "org.specs2"              %% "specs2"              % specsVersion % "test",
    "org.scala-lang"           % "scala-compiler"      % scalaVersion % "test"
  )
}

Seq(RogueBuild.defaultSettings: _*)

resolvers += "Local mvnrepo" at "file:///Users/mar/git/mvn-repo/"

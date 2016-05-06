libraryDependencies <++= (scalaVersion) { scalaVersion =>
  val liftVersion = "2.6.2-MongoAsync-6"
  Seq(
    "net.liftweb"              %% "lift-util"           % liftVersion  % "compile" intransitive(),
    "net.liftweb"              %% "lift-common"         % liftVersion  % "compile" intransitive(),
    "net.liftweb"              %% "lift-record"         % liftVersion  % "compile" intransitive(),
    "net.liftweb"              %% "lift-mongodb-record" % liftVersion  % "compile" intransitive(),
    "net.liftweb"              %% "lift-mongodb"        % liftVersion  % "compile" intransitive(),
    "net.liftweb"              %% "lift-webkit"         % liftVersion  % "compile" intransitive(),
    "net.liftweb"              %% "lift-json"          % liftVersion  % "compile",
    "joda-time"                % "joda-time"               % "2.9.1"        % "compile",
    "org.joda"                 % "joda-convert"            % "1.8.1"        % "compile",
    "org.mongodb"              % "mongodb-driver"          % "3.2.2"     % "compile",
    "org.mongodb"              % "mongodb-driver-async"       % "3.2.2"     % "compile",
    "org.slf4j" % "slf4j-nop" % "1.7.14" % "test")
}

Seq(RogueBuild.defaultSettings: _*)

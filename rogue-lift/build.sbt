libraryDependencies <++= (scalaVersion) { scalaVersion =>
  val liftVersion = "2.6.2-Mongo"
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
    "org.mongodb"              % "mongo-java-driver"       % "2.12.5"     % "compile")
}

Seq(RogueBuild.defaultSettings: _*)

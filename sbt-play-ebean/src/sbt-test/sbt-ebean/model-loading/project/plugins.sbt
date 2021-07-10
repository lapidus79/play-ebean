resolvers ++= DefaultOptions.resolvers(snapshot = true)
addSbtPlugin("io.github.lapidus79" % "sbt-play-ebean" % sys.props("project.version"))
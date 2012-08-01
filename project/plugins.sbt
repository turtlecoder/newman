resolvers += "StackMob Nexus" at "http://nexus/nexus/content/groups/public"

resolvers += Resolver.url("sbt-plugin-releases",
  new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns)

addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.1.0-SNAPSHOT")

addSbtPlugin("me.lessis" % "sbt-growl-plugin" % "0.1.3")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.7.3")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.6.0")

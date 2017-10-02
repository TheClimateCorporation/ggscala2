val Dependencies = Seq(
	"org.clapper" %% "grizzled-slf4j" % "1.3.1",
	"org.slf4j" % "slf4j-api" % "1.7.25",
	"ch.qos.logback" % "logback-classic" % "1.1.7",
	"com.typesafe" % "config" % "1.3.1",
	"org.scalatest" %% "scalatest" % "3.0.1" % Test,
	"org.scalanlp" %% "breeze" % "0.13",
	"org.scalanlp" %% "breeze-natives" % "0.13",
	"org.ddahl" % "rscala_2.11" % "2.3.1",
	"joda-time" % "joda-time" % "2.9.3",
	"org.joda" % "joda-convert" % "1.8.1"
)

val Resolvers = Seq(
	"central" at "https://repo1.maven.org/maven",
	"artifactory" at "https://bin.tcc.li/libs-release")

val PublishTo = Some(Resolver.file("file", new File("target/artifactory-releases")))

lazy val root = (project in file(".")).
	settings(
		name := "ggscala2",
		organization := "com.climate",
		scalaVersion := "2.11.8",
		version := "0.0.3",
		libraryDependencies ++= Dependencies,
		resolvers := Resolvers,
		publishTo := PublishTo,
		scalacOptions := Seq(
			"-feature",
			"-target:jvm-1.8",
			"-deprecation",
			"-unchecked",
			"-encoding", "utf-8",
			"-Xlint",
			"-Xfatal-warnings",
			"-Ywarn-dead-code",
			"-Yno-adapted-args",
			"-Ywarn-unused-import",
			"-Ywarn-numeric-widen",
			"-Ywarn-value-discard",
			"-Ywarn-unused"
		)
	)
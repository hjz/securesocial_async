import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "ssdemo-scala"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
	    "securesocial" % "securesocial_2.10" % "2.1-SNAPSHOT"
    )
    val main = play.Project(appName, appVersion, appDependencies).settings(
    )

}

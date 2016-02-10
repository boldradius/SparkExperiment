name := "SparkExperiment"


scalaVersion := "2.10.5"

//libraryDependencies += "org.apache.spark" %% "spark-core" % "1.6.0"

//Comment out above and uncomment the following to sbt assembly
libraryDependencies += "org.apache.spark" %% "spark-core" % "1.6.0" % "provided"
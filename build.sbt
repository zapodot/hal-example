name := "hal-example"

organization := "org.zapodot"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.10.2"

libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.5"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.0.13"

libraryDependencies += "com.sparkjava" % "spark-core" % "1.1"

libraryDependencies += "com.theoryinpractise" % "halbuilder-standard" % "3.0.1"

com.typesafe.sbt.SbtNativePackager.packageArchetype.java_application

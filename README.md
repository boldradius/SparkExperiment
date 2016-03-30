Overview
=========

This repository contains code from experimentation done to compare different mechanisms for deploying Apache Spark in a clustered environment. Note that these are prototype (not high-availability / production grade) scripts, and I make no commitment to keep them up to date.

Ansible Scripts
---------------

The Ansible folder contains the meat from this work - roles for deploying Spark, HDFS, Yarn and Mesos on a small cluster of servers. Note that these are prototype clusters for experimentation. There are plenty of examples of Ansible scripts for deploying more robust - production secure instances of each of these services available.

To re-use these scripts for your cluster, you will need to update the hosts file with your servers, and potentially the different spark-on*.yml scripts if you change the server group names. The scripts deployed to servers running Ubuntu 14.4, some modifications may be necessary for a different OS. 

Running Test Spark Jobs
----------------------

The easiest way to test the Spark cluster is by running the Spark shell from the Master node (or any node with a spark installation) inside the cluster. Once launched, you can run a basic command like the following:

spark> sc.parallelize(1 to 10000).filter(_<10).collect()

Once completed, visit the appropriate web gui(s) to view the results:
  * For Standalone: http://masterhost:8080
  * For Yarn: http://historyserverhost:18080 and http://yarnmasterhost:8088
  * For Mesos: http://historyserverhost:18080 and http://yarnmasterhost:5050 

The src/... folder contains a basic Spark application used with spark-submit for testing each of the different configurations. The HDFS address is hardcoded into MainHDFS - it will need to be updated for different configurations. 

To run the MainHDFS Demo application (not scripted):
  1. Upload the data folder to hdfs as /user/sparkexp/data. (you may have to zip it and push it inside your HDFS cluster, then unzip)
  2. Run 'sbt assembly' to build a JAR file of the application
  3. Upload the application JAR to the Spark Master server (or HDFS) - from target/scala-2.10/SparkExperiment-...jar
  4. In the folder with the Jar, run the application JAR using the appropriate command depending on the deployment mechanism:

### For Standalone: 

> spark-submit --name "SparkExp" --master spark://<master ip>:7077  --class MainHDFS  SparkExperiment-assembly-0.1-SNAPSHOT.jar

### For Yarn:

> spark-submit --name "SparkExp"  --master yarn-client  --class MainHDFS SparkExperiment-assembly-0.1-SNAPSHOT.jar

### For Mesos:

>  spark-submit --name "SparkExp" --class MainHDFS --master mesos://<mesos master ip>:7079 --deploy-mode client --executor-memory 512m --total-executor-cores 3 SparkExperiment-assembly-0.1-SNAPSHOT.jar

Once completed, visit the appropriate web GUI(s) to view results (same as above for spark-shell).

Note 1: The output from this job will be in a folder also in HDFS under /user/sparkexp/output

Note 2: The additional memory/core settings for Mesos submit command were due to the small size of my Slave boxes. Without these settings, Spark would not accept any of the resources offers from Mesos and you would see the message - “Initial job has not accepted any resources; check your cluster UI to ensure that workers are registered and have sufficient resources”.

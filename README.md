Log Event Application
------------
This is an application written in Java to find log events in server logs using technology stack such as Gradle, HSQL and Spring Boot.

***

Requirements and OS
------------
* [Java Platform (JDK / JRE) 8+])
* Windows / Linux 
* Gradle




How to run locally
-----------
1. Clone project
2. Change directory of the root folder
3. Run in windows`gradlew.bat bootJar`, `java -jar build/libs/application-1.0-SNAPSHOT.jar src/test/resources/test.txt` 
OR 
`export testfile=src/test/resources/test.txt`
Run in Linux  `./gradlew bootJar`, `java -jar build/libs/application-1.0-SNAPSHOT.jar ${testfile}` 
4. Check `eventdb` and `eventdb.log` to verify results are as expected



Summary of task
--------------------

- Our custom-build server logs different events to a file. Every event has  entries in a log - one entry when the event was started and another when 2 the event was finished. The entries in a log file have no specific order (it can occur that a specific event is logged before the event starts)

- Every line in the file is a JSON object containing event data:
	- id - the unique event identifier 
	- state - whether the event was started or finished (can have values "STARTED" or "FINISHED" 
	- timestamp - the timestamp of the event in milliseconds
- Application Server logs also have the additional attributes:
	- type - type of log 
	- host - hostname

Example:
```
{"id":"scsmbstgra", "state":"STARTED", "type":"APPLICATION_LOG",
"host":"12345", "timestamp":1491377495212}
{"id":"scsmbstgrb", "state":"STARTED", "timestamp":1491377495213}
{"id":"scsmbstgrc", "state":"FINISHED", "timestamp":1491377495218}
{"id":"scsmbstgra", "state":"FINISHED", "type":"APPLICATION_LOG",
"host":"12345", "timestamp":1491377495217}
{"id":"scsmbstgrc", "state":"STARTED", "timestamp":1491377495210}
{"id":"scsmbstgrb", "state":"FINISHED", "timestamp":1491377495216}
```

In the example above, the event  duration is 1401377495216 - 1491377495213 = 3ms scsmbstgrb
The longest event is  (1491377495218 - 1491377495210 = 8ms) scsmbstgrc

The program should:

- Take the input file path as input argument 
- Flag any long events that take longer than 4ms with a column in the database called "alert" 
- Write the found event details to file-based HSQLDB ( in the working folder http://hsqldb.org/) 
	- The application should a new table if necessary and enter the following values: 
			# Event id 
			# Event duration 
			# Type and Host if applicable 
			# "alert" true is applicable
- Additional points will be granted for:
			# Proper use of info and debug logging 
			# Proper use of Object Oriented programming Unit test coverage
			# Multi-threaded solution 
			# Program that can handle very large files (gigabytes) As stated above, submissions should be loaded onto 			Github.





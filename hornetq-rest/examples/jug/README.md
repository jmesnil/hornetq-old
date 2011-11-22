### Build HornetQ
* cd ~/Git/hornetq
* git co jug
* ./build.sh jar
* ./build.sh -f build-maven.xml install

## Run the demo
* cd hornetq-rest/examples/jug
* mvn jetty:run

# Run the JMS application
* mvn compile
* mvn  exec:java -Dexec.mainClass="org.hornetq.example.GeolocationMonitorApp"

# Run the Web app
* open http://0.0.0.0:8080/

# Use the official Tomcat image as the base image
FROM tomcat:10.1-jdk17

# Set the working directory in the container
WORKDIR /usr/local/tomcat/webapps

# Copy the WAR file into the Tomcat webapps directory
COPY target/proxyserver-0.0.1-SNAPSHOT.war proxyserver.war

# Expose the port the proxy server listens on
EXPOSE 65432

# Start Tomcat
CMD ["catalina.sh", "run"]
$env:JAVA_HOME = "D:\java\microsoft-jdk-21.0.9-windows-x64\jdk-21.0.9+10"
$env:MAVEN_HOME = "D:\java\apache-maven-3.9.12-bin\apache-maven-3.9.12"
$env:PATH = "$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;$env:PATH"
Write-Host "Java and Maven environment configured."
java -version
mvn -version

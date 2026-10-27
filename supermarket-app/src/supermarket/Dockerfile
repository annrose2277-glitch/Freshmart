FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY . .
RUN javac -encoding UTF-8 -d out src/supermarket/*.java src/supermarket/model/*.java
CMD ["java", "-cp", "out", "supermarket.WebServer"]
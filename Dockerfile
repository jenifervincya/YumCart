FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY src ./src
RUN find src -name "*.java" > sources.txt && mkdir -p out && javac -d out @sources.txt
CMD ["java", "-cp", "out", "com.yumcart.ApiServerMain"]

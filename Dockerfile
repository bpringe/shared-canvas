FROM clojure

RUN mkdir -p /usr/src/app

WORKDIR /usr/src/app

COPY project.clj .

RUN lein deps

COPY . .

RUN mv "$(lein uberjar | sed -n 's/^Created \(.*standalone\.jar\)/\1/p')" app-standalone.jar

EXPOSE 8000

CMD ["java", "-jar", "app-standalone.jar"]

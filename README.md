# zenlectricity
[![Build Status](https://travis-ci.org/meucaa/zenlectricity.svg?branch=master)](https://travis-ci.org/meucaa/zenlectricity)

## Prerequisites

- sbt
- a running MySQL instance

## Database setup

Create a MySQL database and run this [SQL script](https://github.com/meucaa/zenlectricity/blob/master/resources/zenlectricity_tables.sql) script.
This script will create the database tables needed to use the API.

Fill the configuration file `application.conf` with your environment settings:

```java
slick.dbs.default.db.url = "jdbc:mysql://<db_host>/<db_name>?useLegacyDatetimeCode=false&serverTimezone=UTC&useSSL=false"
slick.dbs.default.db.user = "<db_user>"
slick.dbs.default.db.password = "<db_password>"
```

## Testing

```bash
sbt test
```
NB: Database setup is not needed to run the tests (database access are mocked).

## Run in dev mode

```bash
sbt run
```
will run the Play application in development mode on port 9000.

## API usage

API usage is shown in this [Postman collection](https://github.com/meucaa/zenlectricity/blob/master/resources/zenlectricity.postman_collection.json).
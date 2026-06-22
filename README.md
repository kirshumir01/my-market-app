# My Market App

## Описание проекта

**My Market App** — реактивное веб-приложение интернет-магазина, разработанное на Java с использованием Spring Boot
и Spring WebFlux.

Приложение позволяет просматривать каталог товаров, управлять корзиной покупок и оформлять заказы. Доступ к данным
осуществляется через PostgreSQL, а интеграционные тесты выполняются с использованием TestContainers.

### Функционал приложения:

* просмотр каталога товаров;
* поиск товаров по названию и описанию;
* сортировку списка товаров;
* пагинацию результатов;
* просмотр страницы отдельного товара;
* добавление товаров в корзину;
* уменьшение количества товаров в корзине;
* удаление товаров из корзины;
* просмотр содержимого корзины;
* оформление заказов;
* просмотр списка заказов;
* хранение данных в PostgreSQL;
* запуск приложения в Docker-контейнерах;
* выполнение интеграционных тестов в изолированных Docker-контейнерах через TestContainers.

### Используемые технологии

- язык разработки: **Java 21**;
- JDK: **Eclipse Temurin 21.0.7**;
- система сборки и управления зависимостями: **Gradle**;
- фреймворк **Spring Boot Framework 3.5.14**;
- фреймворк **Spring Web Flux JPA**;
- хранение данных (рабочий/тестовый режим): **PostgreSQL**;
- фреймворк для тестирования приложения: **JUnit 5.10.2**;
- библиотека для тестирования: **Testcontainers**;
- html-шаблоны: **Thymeleaf**;
- контейнеризация: **Docker**, **Docker Compose**.

### Схема базы данных приложения
___

![DB-diagram](https://github.com/kirshumir01/my-market-app/blob/main/db/my-market-app-db-diagram.png)

### Структура API

#### Каталог товаров

* GET /
* GET /items?search=&sort=&pageNumber=&pageSize=
* POST /items?id=&action=&search=&sort=&pageNumber=&pageSize=
* GET /items/{itemId}
* POST /items/{itemId}?action=

#### Корзина

* GET /cart/items
* POST /cart/items?id=&action=

#### Заказы

* GET /orders
* GET /orders/{orderId}?newOrder=
* POST /buy

### Профили и конфигурация приложения

Приложение поддерживает несколько профилей Spring Boot для различных сценариев запуска.

#### default

Используется при локальном запуске.

Содержит:
* настройки подключения к PostgreSQL;
* параметры логирования;
* общие настройки приложения.

#### dev

Профиль локальной разработки.

Активируется:

`-Dspring.profiles.active=dev`

или

`./gradlew bootRun --args='--spring.profiles.active=dev`

#### docker

Профиль для запуска внутри Docker.

Активируется через переменную окружения:

`SPRING_PROFILES_ACTIVE=docker`

Внутри Docker-сети приложение подключается к PostgreSQL:

`spring.r2dbc.url=r2dbc:postgresql://postgres:5432/my-market-app`,

где postgres — имя сервиса базы данных в docker-compose.yaml.

#### Проверка активного профиля

При запуске приложения в логах Spring Boot отображается активный профиль:
```text
The following 1 profile is active: "docker"
```
или
```text
The following 1 profile is active: "dev"
```

## Запуск приложения

### Вариант 1. Локальный запуск

1. Сборка исполняемого jar-файла:

- перейти в корневую папку проекта `my-market-app`;
- выполнить команду в терминале:
    - для Unix-систем: ```./gradlew clean build```;
    - для Windows: ```gradlew.bat clean build```;
- после успешной сборки JAR-файл будет находиться в директории: `build/libs/`.

2. Запуск тестов:
- перейти в корневую папку проекта `my-market-app`;
- выполнить команду в терминале:
    - для Unix-систем: ```./gradlew clean test```;
    - для Windows: ```gradlew.bat clean test```.

Интеграционные тесты используют TestContainers. Для выполнения тестов Docker должен быть запущен.

3. Запуск PostgreSQL в Docker-контейнере: ```docker compose -f docker-compose.dev.yml up -d```

4. Запуск приложения с профилем `dev`:

- перейти в папку с собранным файлом: ```cd build/libs```;
- запустить приложение: ```java -Dspring.profiles.active=dev -jar my-market-app-0.0.1-SNAPSHOT.jar```;
- или запустить напрямую через Gradle:
    - для Unix-систем: ```./gradlew bootRun --args='--spring.profiles.active=dev'```;
    - для Windows: ```./gradlew.bat bootRun --args='--spring.profiles.active=dev'```.

5. После запуска приложение будет доступно по адресу: http://localhost:8080.

### Вариант 2. Запуск в Docker

1. Контейнеры приложения и PostgreSQL запускаются с помощью `docker-compose.yaml`:

- сборка образа приложения и запуск контейнеров: ```docker compose up -d --build```;
- повторный запуск без пересборки: ```docker compose up -d```;
- остановка контейнеров: ```docker compose down```;
- удаление контейнеров и томов БД: ```docker compose down -v```.

2. После запуска приложение будет доступно по адресу: http://localhost:8080.

## Наполнение базы данных

При запуске приложения выполняется инициализация тестовых данных через `CommandLineRunner` в классе `MyMarketAppApplication`.

В базу автоматически добавляются:
- товары;
- содержимое корзины;
- тестовые заказы.

Данные пересоздаются при каждом запуске приложения.

## Тестирование

Проект содержит:

#### Unit-тесты

Проверяют отдельные сервисы и контроллеры.

Используются:
* JUnit 5;
* Mockito;
* WebTestClient;
* Reactor Test.

#### Интеграционные тесты

Проверяют взаимодействие приложения с PostgreSQL.

Используются:
* TestContainers;
* PostgreSQLContainer;
* Spring Boot Test;
* WebFlux.

* Во время тестирования контейнер PostgreSQL создается автоматически и удаляется после завершения тестов.
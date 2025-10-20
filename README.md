Лабораторная работа 3: Создание веб-сервиса для SPA

## Цель работы

Создание веб-сервиса в бэкенде системы для использования его в Single Page Application (SPA).

## Порядок демонстрации

Через Insomnia или Postman продемонстрировать коллекцию из 21 запроса в следующем порядке:

 1. **GET** списка заявок (отфильтровать по дате формирования и статусу).
 2. **GET** иконки корзины (возвращает ID заявки-черновика и количество услуг).
 3. **DELETE** удаление введенной заявки (если она существует).
 4. **GET** списка услуг с фильтрацией.
 5. **POST** добавление новой услуги (без изображения).
 6. **POST** добавление изображения к услуге (через MinIO, старое изображение заменяется).
 7. **POST** добавление услуги в заявку-черновик.
 8. **POST** добавление другой услуги в ту же заявку.
 9. **GET** иконки корзины (проверка обновленного количества услуг).
10. **GET** просмотр заявки (содержит 2 услуги).
11. **PUT** изменение поля м-м (например, количество или приоритет).
12. **PUT** изменение полей заявки.
13. **PUT** попытка завершения заявки (показать ошибку из-за отсутствия обязательных полей).
14. **PUT** формирование заявки (установка даты формирования).
15. **PUT** завершение сформированной заявки (вычисление стоимости/даты доставки по формуле из Лабораторной 2).
16. **POST** регистрация нового пользователя.
17. **GET** полей пользователя после аутентификации (для личного кабинета).
18. **PUT** обновление данных пользователя (для личного кабинета).
19. **POST** аутентификация пользователя.
20. **POST** деавторизация пользователя.
21. **SELECT** демонстрация измененных данных через SQL-запрос (например, `SELECT * FROM ping_times WHERE status = 'COMPLETED';`).

### Объяснение моделей и сериализаторов

- **Модели**:
  - `User`: Пользователь (ID, username, password, isModerator).
  - `ServerComponent`: Услуга (ID, title, description, longDescription, time, image, status).
  - `PingTime`: Заявка (ID, status, createdAt, creator, formationDate, completionDate, moderator, totalTime, loadLevel).
  - `PingTimeComponent`: Связь заявки и услуги (pingTimeId, componentId, quantity, priority, componentGroup, subtotalTime, serverComponentTime).
  - `PingTimeComponentId`: Составной ключ для `PingTimeComponent` (pingTimeId, componentId).
- **Сериализаторы**:
  - `UserSerializer`: Для регистрации, аутентификации и обновления профиля.
  - `ServerComponentSerializer`: Для управления услугами и изображениями (MinIO).
  - `PingTimeSerializer`: Для заявок, включая вложенные компоненты.
  - `PingTimeComponentSerializer`: Для управления компонентами заявки.
- **Бизнес-логика**:
  - Системные поля (ID, статус, создатель, даты) вычисляются на бэкенде.
  - Ограничения на статусы: создатель формирует/удаляет черновик, модератор завершает/отклоняет заявку.
  - Создатель фиксирован как константа через функцию-singleton.

## Контрольные вопросы

1. **Веб-сервис**: Серверное приложение для предоставления API клиентам.
2. **REST**: Архитектурный стиль для работы с ресурсами через HTTP (stateless, client-server).
3. **RPC**: Протокол вызова удаленных процедур, ориентированный на действия.
4. **Заголовки и методы HTTP**: Заголовки (Content-Type, Authorization). Методы: GET, POST, PUT, DELETE.
5. **Версии HTTP**: HTTP/1.1 (постоянные соединения), HTTP/2 (мультиплексирование), HTTP/3 (UDP).
6. **HTTPS**: HTTP с TLS/SSL для безопасности.
7. **OSI ISO**: Модель сетевого взаимодействия (7 уровней: физический, канальный, сетевой, транспортный, сеансовый, представления, прикладной).

## Диаграмма классов и детали бэкенда

### Диаграмма классов

- **User**:
  - Поля: `id`, `username`, `password`, `isModerator`.
  - Связи: Один-ко-Многим с `PingTime` (creator, moderator).
- **ServerComponent**:
  - Поля: `id`, `title`, `description`, `longDescription`, `time`, `image`, `status`, `imageUrl` (transient).
  - Связи: Один-ко-Многим с `PingTimeComponent`.
- **PingTime**:
  - Поля: `id`, `status`, `createdAt`, `creator`, `formationDate`, `completionDate`, `moderator`, `totalTime`, `loadLevel`.
  - Связи: Один-ко-Многим с `PingTimeComponent`, Много-к-Одному с `User`.
- **PingTimeComponent**:
  - Поля: `pingTimeId`, `componentId`, `pingTime`, `component`, `quantity`, `priority`, `componentGroup`, `subtotalTime`, `serverComponentTime`.
  - Связи: Много-к-Одному с `PingTime` и `ServerComponent`, составной ключ через `PingTimeComponentId`.
- **PingTimeComponentId**:
  - Поля: `pingTimeId`, `componentId`.

### Домены и методы API

Все методы начинаются с `/api`.

#### Домен услуги (`ServerComponent`)

- **GET /api/services**: Список услуг с фильтрацией.
- **GET /api/services/{id}**: Получение одной услуги.
- **POST /api/services**: Создание услуги (без изображения).
- **PUT /api/services/{id}**: Изменение услуги.
- **DELETE /api/services/{id}**: Удаление услуги (включая изображение в MinIO).
- **POST /api/services/{id}/image**: Загрузка изображения услуги (MinIO, замена старого).
- **POST /api/services/{serviceId}/add-to-ping-time**: Добавление услуги в заявку-черновик.

#### Домен заявки (`PingTime`)

- **GET /api/ping-times/cart**: Иконка корзины (ID заявки-черновика и количество услуг).
- **GET /api/ping-times**: Список заявок (кроме удаленных и черновиков) с фильтрацией.
- **GET /api/ping-times/{id}**: Получение заявки с услугами.
- **PUT /api/ping-times/{id}**: Изменение полей заявки.
- **PUT /api/ping-times/{id}/form**: Формирование заявки (дата формирования, проверка полей).
- **PUT /api/ping-times/{id}/complete**: Завершение заявки (модератор, дата, вычисления).
- **PUT /api/ping-times/{id}/reject**: Отклонение заявки (модератор, дата).
- **DELETE /api/ping-times/{id}**: Удаление заявки.

#### Домен м-м (`PingTimeComponent`)

- **DELETE /api/ping-times/{pingTimeId}/components/{componentId}**: Удаление услуги из заявки.
- **PUT /api/ping-times/{pingTimeId}/components/{componentId}**: Изменение количества/приоритета.

#### Домен пользователь (`User`)

- **POST /api/users/register**: Регистрация.
- **GET /api/users/me**: Данные пользователя (личный кабинет).
- **PUT /api/users/me**: Обновление данных пользователя.
- **POST /api/users/login**: Аутентификация.
- **POST /api/users/logout**: Деавторизация.

### Таблицы базы данных

- **users**: `id`, `username`, `password`, `isModerator`.
- **server_components**: `id`, `title`, `description`, `longDescription`, `time`, `image`, `status`.
- **ping_times**: `id`, `status`, `createdAt`, `creator_id`, `formationDate`, `completionDate`, `moderator_id`, `totalTime`, `loadLevel`.
- **ping_time_components**: `ping_time_id`, `component_id`, `quantity`, `priority`, `componentGroup`, `subtotalTime`, `serverComponentTime`.

### Связи моделей

1. **Методы используют разные модели**: Например, `/api/ping-times/{id}` возвращает данные из `PingTime`, `PingTimeComponent`, `ServerComponent`.
2. **Модели используют другие модели**: `PingTime` ссылается на `User` (creator, moderator).
3. **Модели используют несколько таблиц**: `PingTimeComponent` связывает `ping_times` и `server_components`.

## Задание

Создать RESTful веб-сервис с полной бизнес-логикой (кроме авторизации), подключенный к базе данных через ORM и протестированный в Insomnia/Postman.

### Требования к веб-сервису

- **API**: Соответствие REST, все URL начинаются с `/api`.
- **Фильтрация**: На бэкенде для услуг (по статусу, названию) и заявок (по статусу, диапазону дат).
- **База данных**: Взаимодействие через ORM.
- **Исключения**: Записи со статусом "удален" не передаются клиенту. POST для создания заявок не используется.
- **Статусы**: Ограничения переходов (создатель: удаление/формирование, модератор: завершение/отклонение).
- **Создатель**: Фиксирован как константа через функцию-singleton.
- **Системные поля**: ID, статусы, даты, создатель/модератор вычисляются на бэкенде.
- **Изображения**: MinIO для загрузки/удаления изображений услуг (только в соответствующих методах).

### Методические указания

- **Golang**: Использовать для реализации бэкенда.
- **DRF**: Использовать Django REST Framework для упрощения создания API.

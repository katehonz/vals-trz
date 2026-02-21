# Инсталация и стартиране

## Системни изисквания

- **Java 21** или по-нова версия
- **Node.js 18+** и npm
- **Docker** (за ArangoDB)
- **Maven 3.9+** (за backend)

## Стъпка 1: Изтегляне на проекта

```bash
git clone <repository-url>
cd vals-trz
```

## Стъпка 2: Стартиране на ArangoDB

Базата данни се стартира чрез Docker Compose:

```bash
docker compose up -d
```

ArangoDB ще бъде достъпна на:
- **URL**: http://localhost:8529
- **Потребител**: root
- **Парола**: rootpassword

## Стъпка 3: Стартиране на Backend

```bash
cd backend
mvn spring-boot:run
```

Backend API ще бъде достъпно на: http://localhost:8080

### Конфигурация на Backend

Файл: `backend/src/main/resources/application.properties`

```properties
spring.data.arangodb.hosts=localhost:8529
spring.data.arangodb.user=root
spring.data.arangodb.password=rootpassword
spring.data.arangodb.database=vals_trz
```

## Стъпка 4: Стартиране на Frontend

```bash
cd frontend
npm install
npm run dev
```

Приложението ще бъде достъпно на: http://localhost:5173

### Proxy конфигурация

Frontend използва Vite proxy за комуникация с backend:

```typescript
// vite.config.ts
server: {
  proxy: {
    '/api': 'http://localhost:8080'
  }
}
```

## Бързо стартиране (Linux/Mac)

Използвайте предоставения скрипт:

```bash
./start.sh
```

За спиране на всички услуги:

```bash
./stop.sh
```

## Първоначална настройка

1. Отворете приложението в браузъра
2. Влезте с потребителско име и парола (по подразбиране: admin/admin)
3. Създайте фирма от меню "Фирмени данни"
4. Попълнете основните настройки:
   - БУЛСТАТ/ЕИК
   - Наименование на фирмата
   - Адрес
   - Данни за ръководител

## Проверка на инсталацията

### Проверка на ArangoDB

```bash
curl http://localhost:8529/_api/version
```

### Проверка на Backend

```bash
curl http://localhost:8080/actuator/health
```

### Проверка на Frontend

Отворете http://localhost:5173 в браузъра.

## Отстраняване на проблеми

### ArangoDB не стартира

Проверете дали порт 8529 е свободен:
```bash
lsof -i :8529
```

### Backend грешки при стартиране

1. Проверете версията на Java:
   ```bash
   java -version
   ```
   Трябва да е 21 или по-нова.

2. Проверете дали ArangoDB работи:
   ```bash
   docker ps | grep arangodb
   ```

### Frontend не се свързва с Backend

1. Проверете дали Backend работи на порт 8080
2. Проверете конфигурацията във `vite.config.ts`

### Логове

- Backend логове: `backend.log`
- Frontend логове: конзолата на браузъра (F12)

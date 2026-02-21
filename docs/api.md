# API документация

## Базов URL

```
http://localhost:8080/api
```

## Автентикация

Всички заявки (освен login) изискват JWT токен в header-а:

```
Authorization: Bearer <token>
```

### Логин

```
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin"
}

Response:
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "user": {
    "id": "user123",
    "username": "admin",
    "role": "ADMIN"
  }
}
```

## Фирми

### Списък фирми

```
GET /api/companies
```

### Получаване на фирма

```
GET /api/companies/{companyId}
```

### Създаване на фирма

```
POST /api/companies
Content-Type: application/json

{
  "name": "Фирма ЕООД",
  "bulstat": "123456789",
  "city": "София",
  "address": "ул. Примерна 1"
}
```

### Обновяване на фирма

```
PUT /api/companies/{companyId}
Content-Type: application/json

{
  "name": "Фирма АД",
  ...
}
```

## Служители

### Списък служители

```
GET /api/companies/{companyId}/employees
```

### Получаване на служител

```
GET /api/companies/{companyId}/employees/{employeeId}
```

### Създаване на служител

```
POST /api/companies/{companyId}/employees
Content-Type: application/json

{
  "egn": "8001010000",
  "firstName": "Иван",
  "middleName": "Иванов",
  "lastName": "Петров",
  "birthDate": "1980-01-01",
  "gender": "MALE",
  "phone": "+359888123456",
  "email": "ivan@example.com"
}
```

### Обновяване на служител

```
PUT /api/companies/{companyId}/employees/{employeeId}
```

### Изтриване на служител

```
DELETE /api/companies/{companyId}/employees/{employeeId}
```

## Отдели

### Списък отдели

```
GET /api/companies/{companyId}/departments
```

### Дървовидна структура

```
GET /api/companies/{companyId}/departments/tree
```

### Създаване на отдел

```
POST /api/companies/{companyId}/departments
Content-Type: application/json

{
  "name": "Счетоводство",
  "code": "SCH",
  "parentId": null
}
```

## Трудови правоотношения

### Списък за служител

```
GET /api/companies/{companyId}/employees/{employeeId}/employments
```

### Създаване на ТД

```
POST /api/companies/{companyId}/employees/{employeeId}/employments
Content-Type: application/json

{
  "contractNumber": "001",
  "contractDate": "2024-01-15",
  "startDate": "2024-02-01",
  "position": "Счетоводител",
  "nkpdCode": "2411",
  "baseSalary": 2000.00,
  "workScheduleId": "schedule123",
  "departmentId": "dept456"
}
```

## Пера за възнаграждение

### Списък пера

```
GET /api/companies/{companyId}/pay-items
```

### Зареждане на стандартни пера

```
POST /api/companies/{companyId}/pay-items/seed
```

Зарежда системните пера за начисления (ако няма съществуващи).

### Създаване на перо

```
POST /api/companies/{companyId}/pay-items
Content-Type: application/json

{
  "code": "221",
  "name": "Премии",
  "type": "FIXED",
  "system": false,
  "active": true
}
```

### Изтриване на перо

```
DELETE /api/companies/{companyId}/pay-items/{id}
```

Забележка: Системните пера не могат да се изтриват (връща 403).

## Пера за удръжки

### Списък пера

```
GET /api/companies/{companyId}/deduction-items
```

### Зареждане на стандартни пера

```
POST /api/companies/{companyId}/deduction-items/seed
```

### Създаване на перо

```
POST /api/companies/{companyId}/deduction-items
Content-Type: application/json

{
  "code": "451",
  "name": "Запори",
  "type": "FIXED",
  "system": false,
  "active": true
}
```

### Изтриване на перо

```
DELETE /api/companies/{companyId}/deduction-items/{id}
```

## Осигурителни прагове (МОД)

### Списък прагове

```
GET /api/companies/{companyId}/insurance-thresholds?year=2026
```

### Зареждане на стандартни прагове

```
POST /api/companies/{companyId}/insurance-thresholds/seed?year=2026
```

### Обновяване на праг

```
PUT /api/companies/{companyId}/insurance-thresholds/{id}
Content-Type: application/json

{
  "year": 2026,
  "nkidCode": "01",
  "personnelGroup": 1,
  "minInsurableIncome": 550.66
}
```

## Ведомости

### Списък ведомости

```
GET /api/companies/{companyId}/payrolls
```

### Получаване на ведомост

```
GET /api/companies/{companyId}/payrolls/{year}/{month}
```

### Подготвяне на нов месец

```
POST /api/companies/{companyId}/payrolls/{year}/{month}/start
```

Създава празна ведомост за периода.

### Изчисляване на заплати (всички служители)

```
POST /api/companies/{companyId}/payrolls/{year}/{month}/calculate
```

### Изчисляване на заплата (един служител)

```
POST /api/companies/{companyId}/payrolls/{year}/{month}/calculate/{employeeId}
```

Връща PayrollSnapshot за служителя.

### Получаване на snapshot на служител

```
GET /api/companies/{companyId}/payrolls/{year}/{month}/snapshot/{employeeId}
```

### Списък snapshots за месец

```
GET /api/companies/{companyId}/payrolls/{year}/{month}/snapshots
```

### Заключване на ведомост

```
POST /api/companies/{companyId}/payrolls/{year}/{month}/close
```

### Отключване на ведомост

```
POST /api/companies/{companyId}/payrolls/{year}/{month}/reopen
```

### Преизчисляване на затворен месец

```
POST /api/companies/{companyId}/payrolls/{year}/{month}/recalculate
```

### PDF експорт на фишове

```
GET /api/companies/{companyId}/export/payroll/{year}/{month}/pdf
```

Връща PDF файл с фишове за всички служители.

## Осигурителни вноски

### Списък ставки

```
GET /api/companies/{companyId}/insurance/rates
```

### Създаване/Обновяване на ставки

```
POST /api/companies/{companyId}/insurance/rates
Content-Type: application/json

{
  "year": 2024,
  "minWage": 933.00,
  "maxInsuranceIncome": 3400.00,
  "flatTaxRate": 10.0
}
```

## Работен календар

### Месечен календар

```
GET /api/companies/{companyId}/calendar/{year}/{month}
```

### Инициализиране на календар

```
POST /api/companies/{companyId}/calendar/{year}/{month}/init
```

## Декларации

### Декларация 1

```
GET /api/companies/{companyId}/declarations/1/{year}/{month}
```

### Декларация 6

```
GET /api/companies/{companyId}/declarations/6/{year}/{month}
```

### Уведомление чл. 62

```
GET /api/companies/{companyId}/declarations/article62/{employeeId}
```

### Експорт на декларация

```
GET /api/companies/{companyId}/declarations/{type}/{year}/{month}/export
```

## Документи

### Шаблони

```
GET /api/companies/{companyId}/document-templates
POST /api/companies/{companyId}/document-templates
PUT /api/companies/{companyId}/document-templates/{templateId}
DELETE /api/companies/{companyId}/document-templates/{templateId}
```

### Генериране на документ

```
POST /api/companies/{companyId}/documents/generate
Content-Type: application/json

{
  "templateId": "template123",
  "employeeId": "employee456",
  "variables": {}
}
```

## Банкови плащания

### Списък плащания

```
GET /api/companies/{companyId}/bank-payments
```

### Генериране на файл за банков превод

```
POST /api/companies/{companyId}/bank-payments/{year}/{month}/generate
```

## Потребители

### Списък потребители

```
GET /api/users
```

### Създаване на потребител

```
POST /api/users
Content-Type: application/json

{
  "username": "user1",
  "password": "password123",
  "role": "ACCOUNTANT",
  "companyId": "company123"
}
```

## Одит лог

### Списък записи

```
GET /api/companies/{companyId}/audit-log?from=2024-01-01&to=2024-12-31
```

## Импорт на данни

### Импорт на служители от CSV

```
POST /api/companies/{companyId}/import/employees
Content-Type: multipart/form-data

file: employees.csv
```

### Импорт на номенклатури от CSV

```
POST /api/companies/{companyId}/import/nomenclatures/{type}
Content-Type: multipart/form-data

file: nomenclature.csv
```

## Кодове на грешки

| Код | Описание |
|-----|----------|
| 200 | Успешна операция |
| 201 | Създаден ресурс |
| 400 | Невалидни данни |
| 401 | Неоторизиран достъп |
| 403 | Забранен достъп |
| 404 | Ресурсът не е намерен |
| 500 | Сървърна грешка |

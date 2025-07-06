# Excel SQL API

Excel SQL API 是一个允许用户通过 SQL 查询 Excel 文件的 Java Spring Boot 应用程序。它提供了类似数据库的操作体验，让用户可以使用熟悉的 SQL 语法来操作 Excel 文件。

## 功能特点

- 支持标准 SQL 查询（SELECT, INSERT, UPDATE, DELETE）
- 支持类似数据库的 DDL 操作（CREATE WORKBOOK, CREATE SHEET, USE WORKBOOK）
- 多级缓存机制，提高查询性能
- RESTful API 接口，方便集成
- 支持 Excel 文件上传和管理

## SQL 操作示例

### DDL 操作

#### 创建工作簿

```sql
CREATE WORKBOOK example_workbook
```

带选项的创建：

```sql
CREATE WORKBOOK example_workbook (overwrite=true)
```

#### 创建工作表

```sql
CREATE SHEET employees
```

带列定义的创建：

```sql
CREATE SHEET employees (columns=id:number,name:string,age:number,salary:number,department:string)
```

#### 使用工作簿

```sql
USE WORKBOOK example_workbook
```

#### 删除工作簿

```sql
DROP WORKBOOK example_workbook
```

#### 删除工作表

```sql
DROP SHEET employees
```

### DML 操作

#### SELECT 查询

```sql
SELECT * FROM employees
```

```sql
SELECT name, salary FROM employees WHERE department = 'IT' ORDER BY salary DESC
```

#### INSERT 操作

```sql
INSERT INTO employees (id, name, age, salary, department) VALUES (1, 'John Doe', 30, 5000, 'IT')
```

#### UPDATE 操作

```sql
UPDATE employees SET salary = 6000 WHERE id = 1
```

#### DELETE 操作

```sql
DELETE FROM employees WHERE id = 1
```

## API 接口

### SQL 查询接口

#### POST /api/sql/query

执行 SQL 查询，支持所有类型的 SQL 操作。

请求体：

```json
{
  "sql": "SELECT * FROM employees",
  "workbook": "example_workbook",
  "useCache": true,
  "maxRows": 1000
}
```

#### GET /api/sql/query

执行简单 SQL 查询，适用于简单查询和少量数据。

参数：
- `sql`：SQL 查询语句
- `workbook`：工作簿名称（可选，如果已使用 USE WORKBOOK）

### 文件管理接口

#### POST /api/files/upload

上传 Excel 文件。

#### GET /api/files

获取所有工作簿列表。

#### GET /api/files/{workbookName}

获取指定工作簿的详细信息。

#### DELETE /api/files/{workbookName}

删除指定的工作簿。

## 使用流程示例

1. 创建工作簿：
   ```sql
   CREATE WORKBOOK company
   ```

2. 创建工作表：
   ```sql
   CREATE SHEET employees (columns=id:number,name:string,age:number,salary:number,department:string)
   ```

3. 插入数据：
   ```sql
   INSERT INTO employees (id, name, age, salary, department) VALUES (1, 'John Doe', 30, 5000, 'IT')
   ```

4. 查询数据：
   ```sql
   SELECT * FROM employees WHERE department = 'IT'
   ```

5. 更新数据：
   ```sql
   UPDATE employees SET salary = 6000 WHERE id = 1
   ```

6. 删除数据：
   ```sql
   DELETE FROM employees WHERE id = 1
   ```

## 开发和部署

### 环境要求

- Java 17+
- Maven 3.6+
- MySQL 8.0+（用于元数据存储）
- Redis（可选，用于分布式缓存）

### 构建和运行

```bash
mvn clean package
java -jar target/excel-sql-api.jar
```

### 配置

主要配置位于 `application.yml` 文件中，可以根据需要进行调整。

## 许可证

本项目采用 MIT 许可证。 
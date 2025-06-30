# Excel-SQL Engine

A SpringBoot-based application that allows you to execute SQL-like queries on Excel files, treating workbooks as databases and sheets as tables.

## Features

### Core Capabilities
- **SQL-like syntax** for Excel operations
- **Multiple file format support** (.xlsx, .csv)
- **Advanced caching** with multi-level cache strategy
- **Streaming processing** for large files
- **Asynchronous execution** for complex queries
- **Function system** supporting MySQL-like functions
- **Index support** for improved query performance

### Supported Operations

#### DDL (Data Definition Language)
```sql
-- Create a new Excel workbook
CREATE WORKBOOK `sales_data.xlsx`;

-- Select active workbook
USE WORKBOOK `sales_data.xlsx`;

-- Create a new sheet with column definitions
CREATE SHEET `monthly_sales` (
    id INT PRIMARY KEY,
    product_name VARCHAR(100),
    sales_amount DECIMAL(10,2),
    sale_date DATE,
    region VARCHAR(50)
);

-- Show all workbooks
SHOW WORKBOOKS;

-- Show all sheets in current workbook
SHOW SHEETS;
```

#### DML (Data Manipulation Language)
```sql
-- Insert data into sheet
INSERT INTO SHEET `monthly_sales` 
VALUES (1, '苹果手机', 5999.00, '2024-01-15', '华东区');

-- Insert with column specification
INSERT INTO SHEET `monthly_sales` (id, product_name, sales_amount) 
VALUES (2, '华为手机', 4999.00);

-- Update data
UPDATE SHEET `monthly_sales` 
SET sales_amount = 6299.00 
WHERE id = 1;

-- Delete data
DELETE FROM SHEET `monthly_sales` 
WHERE sale_date < '2024-01-01';
```

#### DQL (Data Query Language)
```sql
-- Simple select
SELECT * FROM SHEET `monthly_sales`;

-- Select with conditions
SELECT product_name, sales_amount 
FROM SHEET `monthly_sales` 
WHERE region = '华东区';

-- Group by and aggregate functions
SELECT region, SUM(sales_amount) as total_sales 
FROM SHEET `monthly_sales` 
GROUP BY region 
ORDER BY total_sales DESC;

-- Pagination
SELECT * FROM SHEET `monthly_sales` 
LIMIT 10 OFFSET 20;
```

### Built-in Functions

#### Aggregate Functions
- `COUNT()` - Count records
- `SUM()` - Sum numeric values
- `AVG()` - Average of numeric values
- `MAX()` - Maximum value
- `MIN()` - Minimum value

#### String Functions
- `CONCAT()` - Concatenate strings
- `SUBSTRING()` - Extract substring
- `UPPER()` - Convert to uppercase
- `LOWER()` - Convert to lowercase
- `TRIM()` - Remove leading/trailing spaces

#### Date Functions
- `NOW()` - Current date and time
- `TODAY()` - Current date
- `DATE_FORMAT()` - Format date with pattern

## Quick Start

### 1. Prerequisites
- Java 11 or higher
- Maven 3.6+

### 2. Build and Run
```bash
# Clone the repository
git clone <repository-url>
cd excel-sql-engine

# Build the project
mvn clean package

# Run the application
java -jar target/excel-sql-engine-1.0.0.jar
```

### 3. API Usage

#### Execute SQL Query
```bash
curl -X POST http://localhost:8080/excel-sql/api/excel-sql/execute \
  -H "Content-Type: application/json" \
  -d '{
    "sql": "CREATE WORKBOOK `test.xlsx`"
  }'
```

#### Execute Async Query
```bash
curl -X POST http://localhost:8080/excel-sql/api/excel-sql/execute-async \
  -H "Content-Type: application/json" \
  -d '{
    "sql": "SELECT * FROM SHEET `large_dataset`"
  }'
```

#### Get Statistics
```bash
curl -X GET http://localhost:8080/excel-sql/api/excel-sql/statistics
```

#### Clear Cache
```bash
curl -X POST http://localhost:8080/excel-sql/api/excel-sql/cache/clear
```

## Configuration

### Application Properties
```yaml
excel-sql:
  cache:
    max-size: 1000              # Maximum cache entries
    expire-after-write: 30m     # Cache expiration time
  
  performance:
    streaming-threshold: 10MB   # File size threshold for streaming
    async-threshold: 1000       # Row count threshold for async processing
    max-memory-usage: 512MB     # Maximum memory usage
  
  storage:
    base-path: /data/excel-files     # Base directory for Excel files
    index-path: /data/excel-indexes  # Directory for index files
    temp-path: /tmp/excel-temp       # Temporary files directory
```

## Performance Optimization

### 1. Caching Strategy
- **Workbook Cache**: Caches frequently accessed workbooks in memory
- **Sheet Data Cache**: LRU cache for sheet data
- **Query Result Cache**: Caches complex query results

### 2. Memory Management
- **Streaming Processing**: Automatically uses streaming for large files
- **Object Pooling**: Reuses workbook objects to reduce GC pressure
- **Memory Monitoring**: Tracks memory usage and triggers cleanup

### 3. Indexing
- **Automatic Indexing**: Creates B+ tree indexes for frequently queried columns
- **Query Optimization**: Uses indexes to speed up WHERE clause processing

## Use Cases

### 1. Data Analysis Platform
```sql
-- Analyze sales data across regions
SELECT region, 
       AVG(sales_amount) as avg_sales,
       COUNT(*) as order_count
FROM SHEET `monthly_report` 
WHERE sale_date BETWEEN '2024-01-01' AND '2024-03-31'
GROUP BY region
ORDER BY avg_sales DESC;
```

### 2. Report Generation
```sql
-- Generate quarterly sales report
CREATE SHEET `q1_sales_report` AS
SELECT 
    product_name,
    SUM(sales_amount) as total_sales,
    COUNT(*) as order_count,
    AVG(sales_amount) as avg_order_value
FROM SHEET `raw_sales_data`
WHERE QUARTER(sale_date) = 1
GROUP BY product_name
ORDER BY total_sales DESC;
```

### 3. Data ETL Processing
```sql
-- Clean and transform data
UPDATE SHEET `customer_data` 
SET phone = REGEX_REPLACE(phone, '[^0-9]', '') 
WHERE phone IS NOT NULL;

-- Normalize data format
INSERT INTO SHEET `normalized_data`
SELECT 
    customer_id,
    UPPER(TRIM(customer_name)) as customer_name,
    DATE_FORMAT(STR_TO_DATE(birth_date, '%Y/%m/%d'), '%Y-%m-%d') as birth_date
FROM SHEET `raw_customer_data`;
```

## Architecture

### System Components
1. **API Layer**: REST controllers for HTTP requests
2. **Service Layer**: Business logic and query coordination
3. **Engine Layer**: SQL parsing and query execution
4. **Storage Layer**: File system operations and data access
5. **Cache Layer**: Multi-level caching for performance

### Key Design Patterns
- **Strategy Pattern**: Multiple storage implementations
- **Command Pattern**: Query execution pipeline
- **Observer Pattern**: Cache invalidation events
- **Factory Pattern**: SQL function creation

## Monitoring and Debugging

### Health Check
```bash
curl http://localhost:8080/excel-sql/actuator/health
```

### Performance Metrics
- Query execution time
- Cache hit rates
- Memory usage
- File I/O statistics

### Logging
- Configurable log levels
- Query execution tracing
- Performance profiling
- Error tracking

## Contributing

1. Fork the repository
2. Create a feature branch
3. Implement your changes
4. Add tests
5. Submit a pull request

## License

This project is licensed under the MIT License.

## Support

For issues and questions:
- Create an issue on GitHub
- Check the documentation
- Review the example queries
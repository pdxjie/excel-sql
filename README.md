<div align="center">
  <img src="/assets/logo.png" style="width:100px;height:100px;">
</div>
<p align="center">


<h2 align="center">👨‍💻 Excel-SQL</h2>
 <p align="center">
     假装是一款强大的 Excel 数据分析工具，用 SQL 的方式操作来处理数据
     <br/>
	<span align="center">🔥 Pretend to be a powerful Excel data analysis tool that uses SQL to process data 📜</span>
</p>



> 🥳 前后端全栈项目 Created By IT 派同学
>

# Excel-SQL-API

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/technologies/javase-jdk17-downloads.html)
[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)]()

🚀 **兼容 MySQL 语法的 Excel 文件 SQL 查询引擎** - 让你像操作数据库一样查询 Excel 文件

## 📖 项目介绍

Excel-SQL-API 是一个基于 Spring Boot 的轻量级 SQL 引擎，能够将 Excel 文件映射为数据库结构：

- 📊 **工作簿（WorkBook）** = 数据库（Database）
- 📋 **工作表（Sheet）** = 数据表（Table）
- 🏷️ **表头（Headers）** = 字段（Columns）

支持标准的 MySQL 语法，让您能够使用熟悉的 SQL 语句查询和操作 Excel 数据。

## ✨ 核心特性

### 🔍 完整的 SQL 支持
- ✅ **基础查询**: SELECT、INSERT、UPDATE、DELETE
- ✅ **聚合函数**: COUNT、SUM、AVG、MAX、MIN
- ✅ **分组查询**: GROUP BY、HAVING
- ✅ **排序分页**: ORDER BY、LIMIT、OFFSET
- ✅ **条件过滤**: WHERE 子句，支持各种比较运算符

### 📊 多格式支持
- 📄 **XLSX**: Excel 2007+ 格式
- 📄 **XLS**: Excel 97-2003 格式
- 📄 **CSV**: 逗号分隔值格式

### ⚡ 高性能架构
- 🚀 **双层缓存**: Caffeine (L1) + Redis (L2)
- 📈 **智能优化**: 查询计划优化
- 💾 **流式处理**: 内存高效的大文件处理
- 🔧 **自动类型检测**: 智能推断数据类型

### 🖥️ 多端支持
- 💻 **Electron 桌面端**: 跨平台桌面应用
- 🌐 **RESTful API**: 完整的 REST 接口
- 📚 **Swagger UI**: 交互式 API 文档

## 🏗️ 技术架构

```
┌─────────────────────────────────────────────────────────────┐
│                        展现层                                │
├─────────────────┬─────────────────┬─────────────────────────┤
│   Electron      │   RESTful API   │    Swagger UI           │
│   (桌面端)       │    (接口)       │     (文档)              │
└─────────────────┴─────────────────┴─────────────────────────┘
┌─────────────────────────────────────────────────────────────┐
│                        服务层                                │
├─────────────────┬─────────────────┬─────────────────────────┤
│   Excel服务     │    SQL服务      │    缓存服务              │
├─────────────────┼─────────────────┼─────────────────────────┤
│  SQL解析器      │   Excel读取器   │    查询执行器           │
│ (JSqlParser)    │ (Apache POI)    │                         │
└─────────────────┴─────────────────┴─────────────────────────┘
┌─────────────────────────────────────────────────────────────┐
│                        数据层                                │
├─────────────────┬─────────────────┬─────────────────────────┤
│    Redis        │   Excel文件     │    Caffeine             │
│   (分布式缓存)   │ (XLSX/XLS/CSV)  │   (内存缓存)            │
└─────────────────┴─────────────────┴─────────────────────────┘
```

## 🛠️ 技术栈

| 技术分类 | 技术选型 | 版本 | 说明 |
|---------|---------|------|------|
| 基础框架 | Spring Boot | 2.x | 核心应用框架 |
| Java版本 | JDK | 17 | 运行环境 |
| Excel处理 | Apache POI | 5.x | Excel文件读写 |
| CSV处理 | OpenCSV | 5.x | CSV文件处理 |
| SQL解析 | JSqlParser | 4.x | SQL语法解析 |
| 缓存技术 | Redis + Caffeine | - | 分布式+本地缓存 |
| 参数验证 | Hibernate Validator | 6.x | 输入验证 |
| API文档 | Swagger | 3.x | 接口文档 |
| 前端框架 | Electron | - | 桌面端应用 |

## 🚀 快速开始

### 环境要求

- Java 17+
- Maven 3.6+
- Redis 6.0+ (可选)
- Node.js 16+ (开发桌面端)

### 安装步骤

1. **克隆项目**
```bash
git clone https://github.com/yourusername/excel-sql-api.git
cd excel-sql-api
```

2. **配置应用**
```bash
# 复制配置文件
cp src/main/resources/application.yml.example src/main/resources/application.yml

# 编辑配置文件
vim src/main/resources/application.yml
```

3. **启动应用**
```bash
# 使用 Maven 启动
./mvnw spring-boot:run

# 或者打包运行
./mvnw clean package
java -jar target/excel-sql-api-1.0.0.jar
```

4. **访问应用**
- API接口: http://localhost:8080
- Swagger文档: http://localhost:8080/swagger-ui.html

### Docker 部署

```bash
# 构建镜像
docker build -t excel-sql-api:latest .

# 运行容器
docker run -p 8080:8080 excel-sql-api:latest
```

## 💡 使用示例

### 基础查询

```sql
-- 查询销售数据
SELECT region, product, amount 
FROM sales_data.xlsx 
WHERE amount > 1000;
```

### 聚合查询

```sql
-- 按地区统计销售额
SELECT region, SUM(amount) as total_sales, COUNT(*) as order_count
FROM sales_data.xlsx 
GROUP BY region 
HAVING total_sales > 50000
ORDER BY total_sales DESC;
```

## 🔧 配置说明

### 应用配置

```yaml
# application.yml
excel:
  sql:
    # 文件上传配置
    upload:
      max-file-size: 100MB
      allowed-types: xlsx,xls,csv
      
    # 缓存配置
    cache:
      caffeine:
        maximum-size: 10000
        expire-after-write: 30m
      redis:
        expire-time: 1h
        
    # 性能配置
    performance:
      max-rows-per-sheet: 1000000
      query-timeout: 60s
      
    # 日志配置
    logging:
      level: INFO
      enable-sql-log: true
```

### 环境变量

```bash
# Redis配置
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=your_password

# 应用配置
SERVER_PORT=8080
JAVA_OPTS=-Xmx2g -Xms1g
```

## 📊 性能指标

| 指标 | 目标值 | 说明 |
|------|--------|------|
| 查询响应时间 | < 2秒 | 标准查询操作 |
| 文件处理能力 | 100MB | 单文件最大支持 |
| 并发用户数 | 100+ | 同时在线用户 |
| 内存使用率 | < 80% | 正常运行状态 |
| 缓存命中率 | > 90% | 热点数据缓存 |

## 📚 API 文档

### 核心接口

#### 1. 执行 SQL 查询

```http
POST /api/sql/execute
Content-Type: application/json

{
    "sql": "SELECT * FROM users.xlsx WHERE age > 18",
    "parameters": {}
}
```

#### 2. 上传文件

```http
POST /api/files/upload
Content-Type: multipart/form-data

file: [Excel文件]
```

#### 3. 获取文件信息

```http
GET /api/files/{filename}/info
```

#### 4. 缓存管理

```http
# 清除缓存
DELETE /api/cache/clear

# 获取缓存统计
GET /api/cache/stats
```

更多API详情请查看: [Swagger UI](http://localhost:8080/swagger-ui.html)

## 🤝 贡献指南

我们欢迎各种形式的贡献！

### 如何贡献

1. **Fork** 本项目
2. **创建** 特性分支 (`git checkout -b feature/amazing-feature`)
3. **提交** 更改 (`git commit -m 'Add amazing feature'`)
4. **推送** 分支 (`git push origin feature/amazing-feature`)
5. **创建** Pull Request

### 开发规范

- 遵循 [Java Code Style](https://google.github.io/styleguide/javaguide.html)
- 编写单元测试，确保测试覆盖率
- 更新相关文档
- 提交前运行 `./mvnw test` 确保测试通过

### 问题报告

如果您发现了bug或有功能建议，请创建一个 [Issue](https://github.com/yourusername/excel-sql-api/issues)。

## 🙏 致谢

感谢以下开源项目的支持：

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Apache POI](https://poi.apache.org/)
- [JSqlParser](https://github.com/JSQLParser/JSqlParser)
- [OpenCSV](http://opencsv.sourceforge.net/)
- [Caffeine](https://github.com/ben-manes/caffeine)

---

⭐ **如果这个项目对您有帮助，请给个 Star！**

**[⬆ 回到顶部](#excel-sql-api)**
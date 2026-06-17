# campus-trade
校园二手交易平台

## 技术栈
- 后端：Java + SpringBoot + MyBatis-Plus
- 数据库：MySQL + Redis
- 认证：JWT
- 前端：原生 HTML/CSS/JS

## 功能模块
- 用户注册/登录（JWT认证）
- 商品浏览/发布/管理
- 购物车
- 订单管理
- 分类管理
- 后台管理（含数据统计）

## 本地运行

### 1. 环境要求
- JDK 8+
- MySQL 5.7+
- Redis

### 2. 数据库初始化
```sql
CREATE DATABASE campus_trade DEFAULT CHARSET utf8mb4;
```
然后导入 `db_schema.sql`

### 3. 配置
修改 `src/main/resources/application.yml` 中的数据库连接信息：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/campus_trade
    username: root
    password: your_password
  redis:
    host: localhost
    port: 6379
```

### 4. 启动
```bash
mvn clean package -DskipTests
java -jar target/campus-trade-1.0.0.jar
```
访问 http://localhost:8080

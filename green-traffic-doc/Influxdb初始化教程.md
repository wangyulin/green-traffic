在 macOS Monterey 上通过 Docker 运行 InfluxDB 2.7 后，创建 API Token 有以下几种方法：

## 方法一：通过 Web UI 创建（推荐）

### 1. 访问 InfluxDB UI
浏览器打开：`http://localhost:8086`

### 2. 首次初始化设置
如果是首次访问，需要先创建初始用户：
- **Username**: 输入用户名（如 `admin`）
- **Password**: 输入密码
- **Organization Name**: 输入组织名（如 `my-org`）
- **Bucket Name**: 输入桶名（如 `my-bucket`）

### 3. 创建 Token
初始化完成后：
1. 点击左侧菜单 **Data** → **Tokens**
2. 点击 **Generate Token** → 选择 **Read/Write Token**（或根据需要选择 All Access Token）
3. 选择 Bucket 权限（读写权限）
4. 输入 Token 描述
5. 点击 **Save**
6. **复制显示的 Token**（只显示一次，请妥善保存）

## 方法二：通过命令行创建

### 1. 进入 Docker 容器
```bash
docker exec -it influxdb /bin/bash
```

### 2. 使用 influx CLI 创建 Token
```bash
# 创建所有权限的 Token
influx auth create \
  --org my-org \
  --all-access \
  --description "Spring Boot Token"

# 或者创建指定 Bucket 的读写 Token
influx auth create \
  --org my-org \
  --write-bucket my-bucket \
  --read-bucket my-bucket \
  --description "Spring Boot Read/Write Token"
```

## 方法三：通过 HTTP API 创建

```bash
# 使用初始用户名密码获取 Token
curl -X POST http://localhost:8086/api/v2/query \
  -H "Content-Type: application/json" \
  -d '{
    "query": "buckets()",
    "type": "flux"
  }' \
  --user admin:password
```

或者使用 API 创建新 Token：
```bash
curl -X POST http://localhost:8086/api/v2/authorizations \
  -H "Authorization: Token YOUR_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "description": "Spring Boot Token",
    "orgID": "YOUR_ORG_ID",
    "permissions": [
      {
        "action": "read",
        "resource": {
          "type": "buckets"
        }
      },
      {
        "action": "write",
        "resource": {
          "type": "buckets"
        }
      }
    ]
  }'
```

## Spring Boot 配置

获得 Token 后，在 `application.yml` 中配置：

```yaml
spring:
  influxdb:
    url: http://localhost:8086
    token: YOUR_GENERATED_TOKEN
    org: my-org
    bucket: my-bucket
```

或者使用环境变量：
```properties
INFLUXDB_URL=http://localhost:8086
INFLUXDB_TOKEN=YOUR_GENERATED_TOKEN
INFLUXDB_ORG=my-org
INFLUXDB_BUCKET=my-bucket
```

## 注意事项

1. **Token 安全**：Token 只显示一次，务必妥善保存
2. **最小权限原则**：建议为 Spring Boot 应用创建专门的 Token，只授予必要的读写权限
3. **Token 格式**：通常是一串很长的字符串，形如 `xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx`
4. **验证 Token**：
   ```bash
   curl -X POST http://localhost:8086/api/v2/query \
     -H "Authorization: Token YOUR_TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"query": "buckets()", "type": "flux"}'
   ```

推荐使用 Web UI 方式创建 Token，最直观且不易出错。`
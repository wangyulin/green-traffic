下面是一份在 macOS Monterey 上通过 Docker 部署 VictoriaMetrics 的保姆级教程。整个过程不涉及复杂配置，只要按照步骤执行即可。

---

## 前提确认

- 已安装 Docker Desktop 并启动（菜单栏出现 Docker 图标，并且状态为 Running）
- 打开终端（Terminal），确认 Docker 可用：

```bash
docker --version
# 应输出版本号，如 Docker version 24.0.x
```

---

## 第一步：拉取 VictoriaMetrics 镜像

在终端执行：

```bash
docker pull victoriametrics/victoria-metrics:latest
```

**说明：**
- 官方镜像名为 `victoriametrics/victoria-metrics`
- 如果拉取速度较慢，可以考虑使用镜像加速器（这里不展开）

---

## 第二步：在 macOS 上创建数据目录

在 macOS 上创建一个文件夹，用于持久化存储 VictoriaMetrics 的数据。

```bash
mkdir -p ~/victoria-metrics-data
```

**说明：**
- `~` 表示当前用户的主目录，即 `/Users/你的用户名/`
- 这样做可以在容器删除或升级后数据不丢失

---

## 第三步：启动容器

执行以下命令运行 VictoriaMetrics：

```bash
docker run -d \
  --name victoria-metrics \
  -p 8428:8428 \
  -v ~/victoria-metrics-data:/victoria-metrics-data \
  victoriametrics/victoria-metrics:latest \
  -storageDataPath=/victoria-metrics-data \
  -retentionPeriod=12
```

### 参数说明（保姆级解释）

| 参数 | 含义 |
|------|------|
| `-d` | 后台运行容器 |
| `--name victoria-metrics` | 给容器起个名字，方便以后管理 |
| `-p 8428:8428` | 把容器的 8428 端口映射到 macOS 的 8428 端口，这样本机浏览器可以访问 |
| `-v ~/victoria-metrics-data:/victoria-metrics-data` | 把刚才创建的本地目录挂载到容器内，用于存储数据 |
| `-storageDataPath=/victoria-metrics-data` | 告诉 VictoriaMetrics 把数据写到挂载的目录 |
| `-retentionPeriod=12` | 数据保留 12 个月，可根据需要修改，单位是月 |

> 也可以设置成 `-retentionPeriod=3` 表示只保留 3 个月。

---

## 第四步：验证容器是否正常运行

### 1. 查看容器状态

```bash
docker ps
```

应看到类似输出：

```
CONTAINER ID   IMAGE                                      STATUS         PORTS                    NAMES
xxxxxxxxxxxx   victoriametrics/victoria-metrics:latest    Up 5 seconds   0.0.0.0:8428->8428/tcp   victoria-metrics
```

### 2. 浏览器访问

打开浏览器，访问：

```
http://localhost:8428
```

你会看到 VictoriaMetrics 自带的 Web UI（VMUI），页面显示当前实例状态、版本信息等。

### 3. 查看健康检查接口

在终端执行：

```bash
curl http://localhost:8428/health
```

应返回 `OK`。

---

## 第五步：写入第一条测试数据

VictoriaMetrics 支持多种写入协议，这里使用最简单的 **Prometheus Remote Write** 或 **InfluxDB Line Protocol** 来演示。

### 方式一：使用 InfluxDB Line Protocol（最简单）

在终端执行：

```bash
curl -i -XPOST 'http://localhost:8428/write' \
  --data-binary 'temperature,sensor=room1 value=23.5 1700000000000000000'
```

**解释：**
- 这条数据表示：在时间戳 `1700000000000000000`（纳秒），传感器 `room1` 的温度为 `23.5` 度

再插入几条：

```bash
curl -i -XPOST 'http://localhost:8428/write' \
  --data-binary 'temperature,sensor=room1 value=24.0 1700000060000000000'

curl -i -XPOST 'http://localhost:8428/write' \
  --data-binary 'temperature,sensor=room2 value=21.5 1700000060000000000'
```

---

## 第六步：查询刚才写入的数据

### 方式一：在浏览器 VMUI 中查询

1. 打开 `http://localhost:8428`
2. 在查询框中输入：

```
temperature
```

3. 点击 **Execute Query**
4. 可以看到返回的时序数据，包含三个时间点的值

### 方式二：使用 curl 查询

在终端执行：

```bash
curl 'http://localhost:8428/api/v1/query?query=temperature'
```

返回 JSON 格式数据，包含查询结果。

---

## 第七步：管理容器常用命令

### 查看日志

```bash
docker logs victoria-metrics
```

### 停止容器

```bash
docker stop victoria-metrics
```

### 启动已存在的容器（重启 Docker 后）

```bash
docker start victoria-metrics
```

### 删除容器（不删除数据）

```bash
docker rm victoria-metrics
```

### 设置开机自启动

启动时加 `--restart always`：

```bash
docker run -d \
  --name victoria-metrics \
  --restart always \
  -p 8428:8428 \
  -v ~/victoria-metrics-data:/victoria-metrics-data \
  victoriametrics/victoria-metrics:latest \
  -storageDataPath=/victoria-metrics-data \
  -retentionPeriod=12
```

这样即使重启 Mac，Docker 也会自动启动该容器。

---

## 第八步：清理数据（可选）

如果需要完全清空数据，直接删除本地数据目录即可：

```bash
rm -rf ~/victoria-metrics-data
```

之后重新创建目录并启动容器即可。

---

## 总结

至此你已经在 macOS Monterey 上完成了 VictoriaMetrics 的 Docker 部署，并成功完成了：

- 容器化安装
- 数据持久化配置
- 健康检查验证
- 写入测试数据
- 查询验证

整个过程资源开销极低，一台普通的 Mac 就能轻松运行。如果后续需要接入 Prometheus、Grafana 或 IoT 设备数据，只需使用相同的端口和协议即可无缝对接。
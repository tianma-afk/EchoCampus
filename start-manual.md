# EchoCampus 手动启动指南

> 适用场景：本地开发调试 / 局域网手机测试 / 答辩演示
> 前提：电脑连 WiFi，手机连同一个 WiFi

---

## 0. 查本机局域网 IP

```powershell
ipconfig
# 无线网卡 IPv4 地址 → 记下，下文用 <IP> 代替
# 示例: 10.195.111.243
```

---

## 1. 启动基础设施（Docker）

必须通过 **WSL** 执行（Windows 的 Docker 装在 WSL 里）：

```bash
# 在 WSL (Ubuntu) 中执行
cd /mnt/d/JavaLearn/EchoCampusProject/EchoCampus

# 启动全部容器
docker compose up -d 

# 验证
docker ps
# 应有 6 个容器在运行
```

---

## 2. 启动 Python 后端（端口 8000）

```powershell
# Windows PowerShell
cd D:\JavaLearn\EchoCampusProject\EchoCampus\backend_py
.venv\Scripts\Activate.ps1
cd app

$env:FORCE_CPU="true"
$env:XFORMERS_DISABLED="1"
uvicorn main:app --host 0.0.0.0 --port 8000
```

等待输出 `服务已启动，资源已加载`（约 10-15 秒）。

---

## 3. 启动 Java 后端（端口 8080）

**新开一个 PowerShell：**

```powershell
cd D:\JavaLearn\EchoCampusProject\EchoCampus\backend_java

$env:API_PUBLIC_BASE="http://192.168.48.6:8080"
$env:MINIO_PUBLIC_ENDPOINT="http://192.168.48.6:9000"
# 示例：
# $env:API_PUBLIC_BASE="http://10.195.103.116:8080"
# $env:MINIO_PUBLIC_ENDPOINT="http://10.195.103.116:9000"

mvn spring-boot:run
```

等待输出 `Started EchoCampusApplication`（约 20 秒）。

---

## 4. 启动移动端前端（端口 5174）

**新开一个 PowerShell：**

```powershell
cd D:\JavaLearn\EchoCampusProject\EchoCampus\frontend_app

$env:VITE_API_BASE="http://192.168.48.6:8080"
# 示例：
# $env:VITE_API_BASE="http://10.195.103.116:8080"

npm run dev
```

看到 `Network: http://<IP>:5174` 即可。

---

## 5. 启动管理后台（端口 5173）

**新开一个 PowerShell：**

```powershell
cd D:\JavaLearn\EchoCampusProject\EchoCampus\frontend_web

npm run dev
```

## 6. 关闭服务

```powershell
# 每个终端按 Ctrl+C 停止

# Docker 容器关闭（WSL 中执行）
docker compose down
```

---

> 注意：环境变量只在当前 PowerShell 窗口有效，关掉窗口后下次需要重新设置。

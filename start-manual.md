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

## 0.1 HTTPS 证书（仅首次 + 换 IP 后执行）

项目使用 mkcert 签发的受信证书，确保 HTTPS 下摄像头和定位均可用。

**首次设置（仅一次）：**

```powershell
# 1. 安装 mkcert（如已装跳过）
winget install mkcert

# 2. 安装本地 CA（弹出 UAC 点"是"）
mkcert -install

# 3. 查当前 IP 并签发证书
ipconfig
cd D:\JavaLearn\EchoCampusProject\EchoCampus\frontend_app
mkcert -key-file key.pem -cert-file cert.pem <你的IP> localhost 127.0.0.1 ::1

# 4. 手机安装 CA（仅首次）
mkcert -CAROOT
# 复制 rootCA.pem 到手机 → 设置 → 安全 → 安装证书 → CA 证书
```

**每次换 IP 后（30 秒）：**

```powershell
ipconfig  # 查看新 IP
cd D:\JavaLearn\EchoCampusProject\EchoCampus\frontend_app
mkcert -key-file key.pem -cert-file cert.pem <新IP> localhost 127.0.0.1 ::1
# 重启 Vite 即可，手机上无需任何操作
```
mkcert -key-file key.pem -cert-file cert.pem  192.168.48.6 localhost 127.0.0.1 ::1
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

```powershell，注意要把10.195.103.116换成自己的ip
cd D:\JavaLearn\EchoCampusProject\EchoCampus\backend_java

$env:API_PUBLIC_BASE="http://192.168.48.6:8080"
$env:MINIO_PUBLIC_ENDPOINT="http://192.168.48.6:9000"
# 示例：
# $env:API_PUBLIC_BASE="http://10.195.98.11:8080"
# $env:MINIO_PUBLIC_ENDPOINT="http://10.195.98.11:9000"

mvn spring-boot:run
```

等待输出 `Started EchoCampusApplication`（约 20 秒）。

> `API_PUBLIC_BASE` 和 `MINIO_PUBLIC_ENDPOINT` 必须填本机局域网 IP，供手机端访问。
> 移动端 API 调用和图片加载走 Vite 代理（`localhost:8080`），与这个 IP 无关。

---

## 4. 启动移动端前端（端口 5174）

**新开一个 PowerShell：**

```powershell
cd D:\JavaLearn\EchoCampusProject\EchoCampus\frontend_app

# VITE_API_BASE 设为空，API 走 Vite 代理，避免 HTTPS 页面加载 HTTP 资源的混合内容问题
$env:VITE_API_BASE=""

npx vite --host
```

看到 `Network: https://<IP>:5174` 即可。手机用 `https://<IP>:5174` 访问，首次需安装 CA 证书（见 0.1 节），安装后地址栏显示 🔒。

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

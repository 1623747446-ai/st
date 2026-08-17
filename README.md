# 协议签名实验室 V1

这个项目用于学习：

- Burp 抓包
- Repeater 原样重放
- 修改 Body 后为什么验签失败
- Comparer 比较请求
- JADX 静态分析
- 找到 `Authorization` 的生成位置
- 最后用 Python 复现签名

**它是完全自建的实验环境，不连接任何第三方平台。**

---

## 一、目录

- `app/`：Android 实验 App
- `server.py`：本地测试服务器
- `python_replay_examples.py`：完成逆向练习之后再看的 Python 答案示例

---

## 二、先启动服务器

电脑打开命令行，进入项目根目录：

```bash
python server.py
```

看到：

```text
Listening on http://0.0.0.0:8000
```

即可。

Windows 防火墙如果询问，允许“专用网络”。

---

## 三、Android Studio 打开项目

建议直接使用当前 Android Studio 的稳定版。

打开：

```text
ProtocolSignatureLabV1
```

等待 Gradle Sync 完成。

这个项目使用：

- Java
- compileSdk 36
- targetSdk 36
- minSdk 23
- 无第三方 Android 依赖

然后：

```text
Build
→ Build App Bundle(s) / APK(s)
→ Build APK(s)
```

或者直接 Run 到测试 Android 设备。

---

## 四、服务器地址

默认界面填写：

```text
http://192.168.2.100:8000
```

这是根据本次实验电脑局域网地址预填的。

如果电脑 IP 变化，在 Windows 中运行：

```bat
ipconfig
```

找到当前局域网 IPv4，再改 App 里的服务器地址。

如果使用 Android Studio 官方模拟器，通常也可尝试：

```text
http://10.0.2.2:8000
```

---

## 五、正常测试

App 中输入：

```text
当前用户：
519835849

目标 UID：
363330929

from_page：
Search
```

点击：

```text
发送关注请求
```

服务器应该返回：

```json
{
  "code": 1000,
  "message": "follow success"
}
```

---

## 六、用 Burp 做第一轮实验

让测试 Android 设备走你的 Burp 代理。

正常点击 App 的“发送关注请求”。

在 Burp HTTP history 找到：

```text
POST /api/follow/add
```

请求中会看到：

```text
Timestamp: ...
Authorization: LAB ...
```

Body：

```json
{
  "user_id":519835849,
  "follow_user_id":"363330929",
  "from_page":"Search",
  "timestamp":...
}
```

### 实验 A：原样重放

Send to Repeater。

一个字符不改。

再次 Send。

预期：

```json
{"code":1000,...}
```

### 实验 B：只修改 UID

把：

```text
363330929
```

改成：

```text
70390943
```

但是不要修改 Authorization。

Send。

预期：

```json
{
  "code":1002,
  "message":"signature invalid"
}
```

### 实验 C：只修改 from_page

把：

```text
Search
```

改成：

```text
Test
```

Authorization 不变。

预期仍然：

```text
code = 1002
```

这就是“请求内容参与签名”的直接现象。

---

## 七、JADX 第一课

构建 APK 后，用 JADX 打开 APK。

第一轮只搜索：

```text
Authorization
```

然后顺着调用寻找：

```text
hmacSha256Hex
```

最终你会看到签名输入由这些内容组成：

```text
POST
/api/follow/add
Timestamp
原始 JSON Body
```

以及 HMAC-SHA256。

V1 故意没有混淆，就是为了让你先学会“从网络请求追到签名函数”。

---

## 八、V1 签名规则（答案）

如果你还没做 JADX 练习，建议暂时不要看这里。

签名原文：

```text
POST
/api/follow/add
<Timestamp>
<原始 JSON Body>
```

算法：

```text
HMAC-SHA256
```

实验密钥：

```text
LAB_SECRET_V1_2026
```

Header：

```text
Authorization: LAB <hex签名>
```

---

## 九、为什么 V1 故意允许 HTTP

这是本地学习项目。

为了让：

```text
Android → Burp → 电脑 Python Server
```

配置尽量简单，项目的 `network_security_config.xml` 开启了明文 HTTP。

不要把这个配置直接照搬到真实生产 App。

---

## 十、下一关

V1 完成以后可以继续做：

- V2：Java 混淆 + 字符串拆分
- V3：Java → JNI → `.so`
- V4：method/path/body/token/device/timestamp 多参数签名

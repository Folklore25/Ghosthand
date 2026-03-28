# 51_Runtime-Verification-Workflow — Ghosthand 运行时验证工作流

> 用途：统一 Ghosthand 的设备侧运行时验证方法，减少“当前手机到底跑的是哪个 APK”“是否真的完成了 accessibility 重绑”“host 侧时序是否产生假阴性”这类误判  
> 性质：工程执行规范  
> 最后更新：2026-03-28

---

## 1. 目标

Ghosthand 的运行时验证应优先满足三件事：

1. **验证的是当前本地构建产物**
2. **验证发生在设备侧，而不是 host 侧时序猜测**
3. **验证步骤可重复执行**

因此当前推荐入口是：

`scripts/ghosthand-verify-runtime.sh`

---

## 2. 为什么要标准化

当前项目有几个实际风险：

- APK 多次重装后，ROM 可能需要重新绑定 AccessibilityService
- host 侧并发/时序容易让 `/wait`、前后台切换、通知读取出现假阴性
- “本地代码已改”不等于“手机上正在跑这个 build”

如果每次都手写 adb 命令，容易混入以下错误：

- 验证了旧 APK
- service 没启动
- accessibility service 已启用但未成功重绑
- 在 host 上等待 UI 变化，结果是 host 时序问题，不是 Ghosthand 问题

---

## 3. 推荐入口

### 3.1 安装当前构建

```bash
scripts/ghosthand-verify-runtime.sh install-current-build
```

用途：
- 把当前 `app/build/outputs/apk/debug/app-debug.apk` 流式安装到设备

适用：
- 代码有变化后
- 需要确保手机运行的就是当前构建时

---

### 3.2 恢复运行时

```bash
scripts/ghosthand-verify-runtime.sh restore-runtime
```

用途：
- 启动前台服务
- 重写 accessibility secure settings
- 恢复 Ghosthand 主界面

适用：
- 刚重装 APK 后
- service/AccessibilityService 状态不可信时

---

### 3.3 轻量 smoke

```bash
scripts/ghosthand-verify-runtime.sh smoke
```

当前覆盖：
- `/ping`
- `/foreground`
- `/commands`

适用：
- 只想快速判断本地 API 是否活着
- 不需要完整交互验证时

---

### 3.4 Wait/Home 验证

```bash
scripts/ghosthand-verify-runtime.sh wait-home
```

用途：
- 在设备 shell 内启动 `GET /wait`
- 随后触发 `/home`
- 读取 wait 结果

适用：
- `/wait` 语义是否仍然正确
- 避免 host 侧时序造成假阴性

---

### 3.5 完整当前标准流程

```bash
scripts/ghosthand-verify-runtime.sh all
```

当前含义：
1. 安装当前构建
2. 恢复运行时
3. 执行 smoke
4. 执行 wait-home

适用：
- 准备做一轮正式运行时验证时
- 需要先把设备拉回已知状态时

---

## 4. 什么时候用哪个

| 场景 | 推荐命令 |
|------|---------|
| 只想知道本地 API 是否在线 | `smoke` |
| 改了代码并准备验证 | `install-current-build` → `restore-runtime` |
| 验证 `/wait` 是否还正常 | `wait-home` |
| 做一轮完整基础健康检查 | `all` |

---

## 5. 当前最佳实践

### 规则 1

**改了代码之后，不要直接手写 adb 验证。先安装当前构建。**

最少应做：

```bash
./gradlew :app:assembleDebug
scripts/ghosthand-verify-runtime.sh install-current-build
scripts/ghosthand-verify-runtime.sh restore-runtime
```

### 规则 2

**状态敏感验证优先在 device shell 内完成。**

特别是：
- `/wait`
- 前后台切换
- 截图权限/系统弹窗
- 依赖 AccessibilityService 当前窗口状态的交互

### 规则 3

**把 root 视为测试恢复手段，不视为产品主线依赖。**

当前脚本使用 `su -c` 恢复运行时，是为了对抗 ROM/重装后的测试环境漂移。  
这不应被误读为 Ghosthand 产品主线需要 root。

### 规则 4

**把 `/commands` 当作运行时能力源，而不是只依赖文档。**

文档描述可以滞后，运行时能力不应该。  
本地 agent 应优先读取 `GET /commands`。

---

## 6. 已知环境 caveat

- 某些 ROM 在 APK 重装后需要重新写 `enabled_accessibility_services`
- host 侧并发请求会引入假阴性，因此已经把 Local API server 改为并发处理
- 截图路径必须优先验证 accessibility screenshot capability，再考虑其它 fallback

---

## 7. 建议的后续扩展

后续可把脚本继续扩展为：

- `focused-input`
  - 自动验证 `/focused` + `/input`
- `selector-click`
  - 自动验证 `/find` + `/click`
- `screenshot-check`
  - 自动验证 `/screenshot` 成功且返回 PNG 头
- `notify-check`
  - 自动发通知再读回

---

## 8. 结论

Ghosthand 当前的运行时验证不应再依赖一次性 adb 手工串命令。

应当：
- 先安装当前构建
- 恢复运行时
- 用设备侧脚本执行 smoke / wait / targeted checks

这就是当前项目的标准验证工作流。

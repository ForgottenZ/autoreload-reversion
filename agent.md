# agent.md — 给 Codex 的开发任务说明（Minecraft 1.20.1 Forge 服务端模组：重启还原世界）

你是一个资深 Minecraft Forge（1.20.1）模组工程师。请生成一个**仅服务端使用**的 Forge 模组：每次服务器启动/重启时，都将世界完整还原到一个“模板压缩包（zip）”的状态（包括方块、区块数据、实体、玩家数据等——本质上是还原整个存档目录内容）。

> 目标效果：玩家在服务器里做的任何改动（建造、破坏、生成/消灭实体等）都会在**下一次服务器重启后**消失，世界回到模板 zip 的初始状态。

---

## 1. 基本要求（必须满足）

### 1.1 版本与加载环境
- Minecraft：**1.20.1**
- Forge：对应 1.20.1 的 **47.x** 系列（用 MDK 默认推荐即可）
- Java：**17**
- 模组：**服务端可运行**（Dedicated Server）
- **不要写任何客户端渲染/GUI/按键等内容**。确保 Dedicated Server 无需客户端依赖即可启动。

### 1.2 还原机制（核心）
- 在服务器启动流程中、**世界真正加载前**，执行以下动作：
  1) 定位当前服务器使用的世界存档目录（世界根目录）。
  2) 从配置指定的 zip 路径读取模板压缩包。
  3) 解压到临时目录并做安全校验（防 Zip Slip 路径穿越）。
  4) 清空当前世界目录（注意 `session.lock` 可能被锁定，见后文），然后把模板内容复制/覆盖回世界目录。
  5) 记录日志：成功/失败原因、耗时等。

> 说明：只要把世界目录完整替换/覆盖，方块与实体（chunk NBT / entities 数据）都会自然回到模板状态。

### 1.3 模板压缩包来源
- 模板是一个**本地文件系统路径**的 zip（Windows/Linux 都要支持）。
- 该路径必须可在配置文件中设置（默认给一个合理路径，如服务器根目录下 `world_template.zip` 或 `config/worldrestore/world_template.zip`）。
- 支持两种 zip 结构（都要兼容）：
  - A) zip 内直接是世界根目录文件：`level.dat`、`region/`、`playerdata/` 等
  - B) zip 内第一层是一个文件夹（如 `world/`），其下才是 `level.dat` 等  
  你需要自动识别并选择正确的“解压根”。

---

## 2. 关键细节（避免踩坑）

### 2.1 启动时机（很重要）
- 使用 Forge 的服务器生命周期事件，在**尽可能早**、且世界加载前执行。
- 优先选择类似 `ServerAboutToStartEvent`（Forge 总线）这样的时机。
- 若发现该事件触发时世界目录已被部分访问，也要保证还原动作仍发生在世界正式加载前。

### 2.2 session.lock 文件锁（必须处理）
- MC 会对世界目录创建并持有 `session.lock`（尤其在 Windows 上删除会失败）。
- 你的实现必须做到：
  - 清空世界目录时**跳过** `session.lock`（以及其父目录本身不要强删导致失败）。
  - 解压/复制模板时，如模板内也包含 `session.lock`，应**忽略**该条目，不覆盖运行中的锁文件。
- 除 `session.lock` 外，默认应当尽可能完整替换其它内容，以保证“完全回档”。


- 如任何步骤失败，必须输出清晰日志，并根据配置决定：继续启动（skip）或直接阻止服务器启动（throw）。

## 3. 配置设计（必须提供）

### 3.1 配置文件类型与位置
- **不要使用会放在 world/serverconfig 下的 SERVER 配置**（因为世界会被你覆盖，配置会丢）。
- 使用 COMMON 配置（位于 `<server>/config`），保证配置不随世界重置丢失。

### 3.2 配置项（至少这些）
- `enabled`（boolean，默认 true）
- `templateZipPath`（string，默认相对路径如 `world_template.zip`，相对路径基于服务器运行目录/游戏目录解析）
- `failHardIfMissing`（boolean，默认 true：找不到模板 zip 就直接阻止服务器启动）
- `preserveServerConfigDir`（boolean，默认 false，可选：
  - 若为 true，则清理世界目录时保留 `serverconfig/`；否则一起回档
  - 注意：仍然必须保留 `session.lock`）

---

## 4. 额外功能

- 注册一个只读命令（避免运行中破坏世界）：
  - `/worldrestore status`：显示是否启用、模板路径、上次还原结果/耗时（可缓存到内存并打印）
- 提供一个“运行中强制还原世界”的命令，直接要求重新加载世界。
---

## 5. 工程与交付物（你需要生成完整项目）

请输出一个可直接构建的 Forge MDK 工程（或基于 MDK 的标准结构）：

- `build.gradle` / `gradle.properties` / `settings.gradle`（可用 MDK 默认）
- `src/main/resources/META-INF/mods.toml`
- `src/main/java/...` 主要代码（至少包含）：
  - Mod 主类（注册配置、注册事件）
  - 配置类（ForgeConfigSpec）
  - 世界还原服务类（文件删除/复制/解压/校验/日志）
- `README.md`（必须写清楚）：
  - 安装方法（把 jar 放进 mods）
  - 模板 zip 放哪里、配置怎么改
  - zip 结构要求（支持两种结构）
  - 重启后回档的说明
  - 常见问题：Windows 下 `session.lock` 无法删除（已处理）

---

## 6. 实现提示（你可以按此思路写）

### 6.1 世界目录获取（建议）
- 从 `MinecraftServer` 获取世界根目录 Path（例如使用类似 `server.getWorldPath(LevelResource.ROOT)` 的方式；若 API 差异，以 1.20.1 的可用方式为准）。
- 只要拿到“世界根目录”，后续复制/覆盖都围绕它做。

### 6.2 递归删除（跳过保留项）
- 使用 `Files.walkFileTree` 从深到浅删除。
- 跳过：
  - `session.lock`
- 注意：不要试图删除世界根目录本身（只清空内部内容）。

### 6.3 递归复制（覆盖）
- 同样用 `Files.walkFileTree` 实现从模板根复制到世界根：
  - 目录：`Files.createDirectories`
  - 文件：`Files.copy(..., REPLACE_EXISTING, COPY_ATTRIBUTES)`（属性复制可选）
- 如果遇到目标为 `session.lock`：跳过。

### 6.4 日志
- 使用 Forge/SLF4J logger：
  - 启动开始：模板路径、世界目录
  - 成功：耗时 ms、复制文件数量（可选）
  - 失败：异常堆栈、下一步行为（fail hard 或 skip）

---

## 7. 验收标准（你生成的项目必须满足）

1) Dedicated Server 放入该 mod 后启动：
   - 若模板 zip 存在：服务器正常启动，世界为模板状态。
   - 若模板 zip 不存在且 `failHardIfMissing=true`：服务器应在启动早期报错并停止（而不是继续生成新世界）。
2) 在游戏里破坏/放置方块、生成实体，然后重启服务器：
   - 重新进入后，世界完全回到模板状态（方块/实体/玩家数据等以模板为准）。
3) Windows 环境不会因为 `session.lock` 删除失败导致崩溃（已跳过该文件）。

确保代码能编译、结构清晰、注释适量、异常处理完善。

开始吧。

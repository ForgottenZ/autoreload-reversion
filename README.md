# World Restore (Minecraft 1.20.1 Forge Server Mod)

一个**仅服务端**的 Forge 1.20.1 模组：每次服务器启动/重启时，从本地模板 zip 还原整个世界存档（方块、实体、玩家数据等全部回档）。

## 功能概览

- 服务器启动时自动还原世界目录（世界尚未加载前执行）。
- 支持模板 zip 两种结构：
  - **A**：zip 内直接包含 `level.dat` / `region/` / `playerdata/` 等。
  - **B**：zip 第一层是一个文件夹（如 `world/`），其下包含 `level.dat`。
- 自动处理 `session.lock`（不会删除/覆盖）。
- `/worldrestore status` 显示当前配置与上次还原结果。
- `/worldrestore restore`：要求服务器关闭，**下次启动**会自动还原世界。

## 安装

1. 使用 Forge 1.20.1 (47.x) 启动服务器。
2. 将构建出的 `worldrestore-1.0.0.jar` 放入 `<server>/mods`。
3. 启动服务器一次生成配置后，编辑 `<server>/config/worldrestore-common.toml`。

## 配置说明

配置文件：`<server>/config/worldrestore-common.toml`

- `enabled`：是否启用（默认 `true`）。
- `templateZipPath`：模板 zip 路径（默认 `world_template.zip`，相对路径基于服务器运行目录）。
- `failHardIfMissing`：模板缺失时是否阻止服务器启动（默认 `true`）。
- `preserveServerConfigDir`：是否保留 `world/serverconfig`（默认 `false`）。

> ⚠️ **注意**：本模组使用 COMMON 配置，位于 `<server>/config`，不会被世界回档覆盖。

## 模板 zip 放置

将模板 zip 放到配置指定的位置。默认推荐放在服务器根目录：

```
<server>/world_template.zip
```

## 还原流程

- 服务器启动时（世界加载前）：
  1. 解压模板 zip 到临时目录并校验路径安全（防 Zip Slip）。
  2. 清空世界目录（跳过 `session.lock`，可选保留 `serverconfig/`）。
  3. 将模板内容复制回世界目录。

## 常见问题

**Q: Windows 下 `session.lock` 无法删除怎么办？**

A: 已在清理逻辑中跳过 `session.lock`，不会删除或覆盖它。

**Q: `/worldrestore restore` 为什么会关闭服务器？**

A: 运行中替换完整世界目录会导致崩溃/数据不一致。
该命令会要求服务器关闭，下一次启动时自动执行还原。

## 命令

- `/worldrestore status`：显示是否启用、模板路径、上次还原结果/耗时。
- `/worldrestore restore`：请求关闭服务器，下一次启动执行还原（需要权限等级 2）。

## 构建

```bash
./gradlew build
```

生成的 jar 在 `build/libs/` 目录。

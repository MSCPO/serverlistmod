# MSCPO Serverlist

在 Minecraft 原版**多人游戏界面**右侧加入一个 MSCPO 服务器列表面板，方便浏览、搜索并一键加入由 [MSCPO](https://github.com/MSCPO)（Minecraft 服务器集体宣传组织）收录的服务器。

- **Minecraft 版本**：1.21.1 / 1.21.4 / 1.21.8 / 1.21.11 / 26.1.2（26.1 系列）/ 26.2
- **加载器**：Fabric（Fabric Loader ≥ 0.19.3，Fabric API）与 NeoForge
- **运行环境**：客户端（Client）专用

## 功能特性

- **侧拉抽屉面板**：在原版多人游戏界面右侧增加 MSCPO 服务器列表。
  - **大屏（物理窗口 ≥ 1280px）**：关闭时右侧只有一条标签，点击标签向左滑开，形成「原版服务器列表 | 标签 | MSCPO 列表」三区并排；点击中间标签即可滑回关闭。
  - **小屏（物理窗口 < 1280px）**：打开后面板覆盖整个窗口宽度（原版列表宽度立即变为 0），最大化利用空间。
- **实时适配窗口大小**：拖动窗口跨越大小屏阈值时，面板与标签位置自动切换，无需重开。
- **搜索**：输入关键字（匹配名称 / 描述 / 地址），防抖后请求 API。
- **分类筛选**：主分类 + 子分类两个下拉框，数据来自 `/categories` 接口；切换分类/页签自动回到列表顶部。
- **三种页签**：全部 / 收藏 / 最近游玩。
- **收藏 & 最近游玩**：本地持久化到 `config/mscpo-serverlist.json`（含收藏、最近游玩记录、API 地址）。
- **加入服务器**：选中条目后点击「加入服务器」或双击条目，直接连接；加入后自动记入「最近游玩」。
- **真实服务器状态**：每个条目向服务器发起真实的服务器列表 Ping（异步多线程），显示：
  - 服务器图标（Favicon）
  - 格式化后的 MOTD 描述（最多两行）
  - 在线人数 / 最大人数
  - 延迟（Ping）与状态图标（连接中 / 在线 / 不可达 / 版本不兼容）
  - 悬浮提示：延迟毫秒数、在线玩家列表（与原版一致）
- **无限滚动**：列表滚到底部自动加载下一页。

## 安装

1. 安装对应加载器：
   - **Fabric**：[Fabric Loader](https://fabricmc.net/use/)（≥ 0.19.3）与对应版本的 [Fabric API](https://modrinth.com/mod/fabric-api)
   - **NeoForge**：[NeoForge](https://neoforged.net/)（对应版本见产物文件名）
2. 将构建产物 `MSCPO-serverlist-*.jar` 放入 `.minecraft/mods/` 目录。
3. 启动游戏，进入「多人游戏」界面，点击右侧的 MSCPO 标签即可。

## 使用说明

- **打开 / 关闭面板**：点击面板最左缘的「MSCPO ▸ / ◂」标签。
- **切换页签**：面板顶部「全部 / 收藏 / 最近」三个按钮。
- **搜索**：在搜索框输入关键字，稍候自动刷新结果。
- **筛选分类**：点击「全部分类」与「全部子分类」下拉框选择分类。
- **加入服务器**：单击选中条目 → 点击「加入服务器」；或直接双击条目。
- **收藏 / 取消收藏**：选中条目后点击「收藏 / 取消收藏」；条目左上角的 ★/☆ 标记当前收藏状态。

## 数据来源

- 默认 API：`https://api.mscpo.com/api/serverlist`
- 分类：`GET /api/serverlist/categories`
- 服务器列表：`GET /api/serverlist/servers?page=&pageSize=&q=&category=&subCategory=`
- 徽章：`GET /api/serverlist/badges`

如需更换 API 地址，可编辑 `config/mscpo-serverlist.json` 中的 `apiBaseUrl` 字段（首次运行后生成）。

## 配置文件

配置文件位于 `config/mscpo-serverlist.json`（游戏运行目录下）：

```json
{
  "apiBaseUrl": "https://api.mscpo.com/api/serverlist",
  "favorites": [ ... ],
  "recents": [ ... ]
}
```

## 构建

### 一键构建（推荐）

直接使用本机 `D:\java` 下的 Gradle 9.5.1 与本地 JDK（zulu21 / zulu25）构建全部 12 个产物：

```powershell
powershell -ExecutionPolicy Bypass -File .\build-all.ps1        # 构建全部版本
powershell -ExecutionPolicy Bypass -File .\build-all.ps1 1.21.11 # 只构建指定版本
powershell -ExecutionPolicy Bypass -File .\build-all.ps1 -Copy  # 构建并把 jar 复制到 .\dist
```

支持版本：`26.2`、`26.1.2`、`1.21.11`、`1.21.8`、`1.21.4`、`1.21.1`。

### 手动构建

需要本机 JDK（`D:\java\zulu21`，26.x 需要 `D:\java\zulu25`）与 Gradle：

```powershell
& 'D:\java\gradle-9.5.1\bin\gradle.bat' :fabric:build :neoforge:build -Pmc=1.21.11
```

产物位于 `fabric\build\libs\` 与 `neoforge\build\libs\`，文件名带 `+<mc版本>` 后缀。

## 项目结构

```
MSCPO-serverlist/
├── shared-java/        # 跨加载器/跨版本的纯 Java 代码（API 客户端、配置存储等）
├── shared-resources/   # 共享资源（语言文件、图标、Mixin 配置）
├── gui/<mc版本>/java/  # 各 MC 版本专用的 GUI 与 Mixin（统一使用 Mojang 官方映射命名）
├── fabric/             # Fabric 加载器模块
└── neoforge/           # NeoForge 加载器模块
```

- 两个加载器共享同一份 `gui/<版本>` 源码（Mojang 官方映射，Fabric 侧亦使用 `officialMojangMappings`）。
- 1.21.x（混淆版本）Fabric 使用常规 remap 插件；26.x（Mojang 已停止混淆的版本）Fabric 使用无 remap 插件 `net.fabricmc.fabric-loom`，加载器/API 以普通 `implementation` 依赖引入。

## 技术说明

- 通过 Mixin 修改 `JoinMultiplayerScreen`，在右侧叠加 MSCPO 面板，并实时调整原版服务器列表的宽度。
- 服务器状态请求使用原版 `ServerStatusPinger`，提交到独立的 8 线程守护线程池，避免阻塞渲染线程。
- Ping 结果有 5 分钟本地缓存，切换页签 / 刷新时不会重复请求。

## 许可证

MIT License（详见 [LICENSE.txt](LICENSE.txt)）。

> 本项目使用 MSCPO 的公开服务器列表 API。请遵守其[使用约定](https://github.com/MSCPO/ServerAPI)，仅供开源、非商用、小规模使用。

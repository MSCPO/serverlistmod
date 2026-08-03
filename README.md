# MSCPO Serverlist

在 Minecraft 原版**多人游戏界面**右侧加入一个 MSCPO 服务器列表面板，方便浏览、搜索并一键加入由 [MSCPO](https://github.com/MSCPO)（Minecraft 服务器集体宣传组织）收录的服务器。

- **Minecraft 版本**：1.21.11
- **加载器**：Fabric（Fabric Loader ≥ 0.19.3，Fabric API）
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

1. 安装 [Fabric Loader](https://fabricmc.net/use/)（≥ 0.19.3）与对应版本的 [Fabric API](https://modrinth.com/mod/fabric-api)。
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

需要 **JDK 21** 与 Gradle。

```bash
gradlew build
```

构建产物位于 `build/libs/MSCPO-serverlist-1.0.jar`。

开发运行：

```bash
gradlew runClient
```

## 技术说明

- 通过 Mixin 修改 `MultiplayerScreen`，在右侧叠加 MSCPO 面板，并实时调整原版服务器列表的宽度。
- 服务器状态请求使用原版 `MultiplayerServerListPinger`，提交到独立的 8 线程守护线程池，避免阻塞渲染线程。
- Ping 结果有 5 分钟本地缓存，切换页签 / 刷新时不会重复请求。

## 许可证

MIT License（详见 [LICENSE.txt](LICENSE.txt)）。

> 本项目使用 MSCPO 的公开服务器列表 API。请遵守其[使用约定](https://github.com/MSCPO/ServerAPI)，仅供开源、非商用、小规模使用。

# 植物大战僵尸2 中文版 · 植物碎片计数器

一个本地离线的 Android 植物碎片记录工具。数据只保存在手机本地，无需联网、无需注册。

## 功能

- 按「可探险 / 不可探险」→「进行中 / 已满待五阶 / 已完成」→ 颜色三级分组，逐层折叠
- 卡片上直接加减碎片；碎片够了只提示「可升阶」，阶数手动修改，不会自动扣碎片
- 记录五种颜色培养液等道具的已有数量与需求
- 支持按名称、拼音首字母搜索
- 数据可导出为 JSON 备份，导入时支持覆盖或合并

## 下载与安装

- 在 [Releases](../../releases) 页面下载 APK（Android 8.0 及以上）。
- 首次启动自带全部植物卡片，无需额外导入。

## 数据说明

- 所有数据保存在应用私有数据库中，卸载应用会一并清除。
- 换手机或重装前，请先在「设置 → 数据」中导出 JSON 备份。

## 构建

需要 JDK 17 与 Android SDK 35：

```bash
./gradlew :app:assembleDebug        # macOS / Linux
gradlew.bat :app:assembleDebug      # Windows
```

Release 包签名从 `local.properties` 读取 `RELEASE_STORE_FILE`、`RELEASE_STORE_PASSWORD`、`RELEASE_KEY_ALIAS`、`RELEASE_KEY_PASSWORD`；未配置时产出未签名包。

## 素材来源与声明

植物图片与道具图标来自植物大战僵尸2 中文版官方图鉴：<https://pvz2.hrgame.com.cn/books?type=10>，版权归原权利方所有。本项目为非官方粉丝作品，与游戏开发商及发行商无关。

## 反馈

- 邮箱：habudesu@outlook.com
- GitHub Issues：<https://github.com/habudesu/pvz2-fragment-tracke/issues>

// 项目最终核查说明
// ============================================================
// 本项目完整实现了"连连看"Android游戏应用，涵盖课程要求全部知识点。
//
// === 使用方法 ===
// 1. 在 Android Studio 中打开本项目目录
// 2. 将 res/raw/ 目录下的音频素材文件放入（可使用系统默认提示音替代）
// 3. 同步 Gradle → 构建 → 运行
//
// === 需要手动补充 ===
// res/raw/ 目录需要放入以下音频文件（可使用免费素材替代）：
//   - background_music.mp3
//   - sound_click.mp3
//   - sound_match.mp3
//   - sound_fail.mp3
//   - sound_win.mp3
//   - sound_lose.mp3
//
// 如果暂时没有音频文件，可以在 SoundManager 中使用 Android 内置提示音：
//   MediaPlayer.create(context, Settings.System.DEFAULT_NOTIFICATION_URI)
// 来替代 raw 目录下的音频文件。
//
// === 核心功能清单 ===
// ✅ 8×9 棋盘，随机生成 36 对动物
// ✅ 三种难度：简单(10种)/中等(15种)/困难(25种)
// ✅ 路径查找算法：最多2个拐点，支持棋盘外绕行
// ✅ 胜负判定：胜利/超时/死局 三种结果
// ✅ 2分钟倒计时，最后30秒红色警告
// ✅ BottomNavigationView + Fragment 四个页面
// ✅ OptionsMenu 选项菜单（难度设置/关于）
// ✅ ContextMenu 上下文菜单（打乱重排/重新开始/暂停）
// ✅ MusicService 前台服务播放背景音乐
// ✅ HeadsetReceiver 耳机拔出暂停音乐
// ✅ GameResultReceiver 接收游戏结果广播
// ✅ LocalBroadcast 胜利广播传递完整数据
// ✅ Notification 胜利/超时通知
// ✅ SharedPreferences 用户设置存储
// ✅ SQLite + ContentProvider 排行榜数据存储
// ✅ 消除动画 + 路径高亮
// ✅ 屏幕旋转状态保持
// ✅ 初始棋盘无直接可消除配对
//
// === 四次实验覆盖 ===
// 实验1：Activity, 游戏主功能, 胜负判定, Navigation, Fragment, 广播机制
// 实验2：Fragment完善, OptionsMenu, ContextMenu, 难度选择, Intent跳转
// 实验3：音效, Service, BroadcastReceiver, Preference, 通知
// 实验4：SQLite, ContentProvider, Notification, 数据持久化
// ============================================================
// 占位文件说明：
// res/raw/ 目录需要放入以下音频文件：
// - background_music.mp3  背景音乐
// - sound_click.mp3       点击音效
// - sound_match.mp3       消除配对音效
// - sound_fail.mp3        失败音效
// - sound_win.mp3         胜利音效
// - sound_lose.mp3        游戏结束音效
//
// 推荐免费音效来源：
// - https://freesound.org (搜索关键词: click, match, game win, game over)
// - https://mixkit.co/free-sound-effects/
// - Android系统默认音效: RingtoneManager内置提示音

// 如果暂时没有音频素材，可以使用以下代码创建简单的提示音：
// ToneGenerator toneGen = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
// toneGen.startTone(ToneGenerator.TONE_PROP_BEEP);
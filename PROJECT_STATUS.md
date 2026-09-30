# Golf Score 開発引継ぎ

## 現在の状態

- アプリ名: GOLF SCORE
- Android版: Version 1.14.0（versionCode 22）
- 開発環境: Android Studio / Kotlin / Jetpack Compose
- 対象端末: Galaxy S25
- データ保存: 端末内の SharedPreferences（JSON）
- オフラインでラウンドの作成・閲覧・編集・削除が可能
- 最新のDebug APK: `app/build/outputs/apk/debug/app-debug.apk`

## 主なファイル

- メイン実装: `app/src/main/java/jp/example/golfscore/MainActivity.kt`
- アプリ設定: `app/build.gradle.kts`
- Android設定: `app/src/main/AndroidManifest.xml`
- ホーム背景: `app/src/main/res/drawable/home_golf_course.png`

## 実装済み機能

- プレー日、ゴルフ場名、地域、天気、風、ティーグラウンド
- 最大4人のプレーヤーと、本人・仲間の登録
- レディースティーのプレーヤー別指定
- 18ホールのスコア、パット、OB、バンカー、1ペナ
- 合計スコア・パットの自動計算
- 1画面1ホール、4人表示、数字キー入力
- 左右スワイプによるホール移動とホール移動時の自動保存
- 1分ごとの自動保存
- 履歴一覧、スコアカード、編集、削除
- 縦横回転に対応した横スクロール式スコアカード
- 楽天GORAからゴルフ場とコース名を検索
- ドラコン・ニアピンの対象ホールと獲得者
- 前半・後半に分けたドラコン／ニアピン結果と個人別獲得数
- オリンピック（金・銀・銅・鉄）、得点設定、最終得点、清算
- たてよこの配置、判定方法、勝ち点、同点持越し、ローテーション
- たてよこの最終得点と清算
- オリンピック＋たてよこの合算清算
- ドラコン・ニアピン・オリンピックのゲーム別参加者選択（たてよこはルール上4人必須）
- 楽天GORAのWebコース情報からコース・グリーン・ティー別のPAR／ホール距離を取得してラウンド内に保持
- 右／左、A／B、ベント／コーライなどのグリーン候補と自由入力
- 選択した前後半コース・グリーン・ティーに一致する18ホールのPAR／距離を自動反映
- Webコース情報を取得できない場合は、同じ組み合わせで手入力したPAR／距離を端末内から再利用
- プレー年月日をタップするとカレンダーが開き、任意の日付を選択可能
- プレーヤー名は登録済み候補からの選択と、その場での直接入力の両方に対応
- ドラコン・ニアピンの対象ホールは、基本情報の前半／後半コース名ごとに1〜9番で指定（IN→OUTや3コース構成にも対応）

## 検証モード

ホーム画面中央の `GOLF SCORE` を素早く2回タップすると、18ホール分の検証ラウンドを履歴へ追加する。

- コース名: 検証コース
- プレーヤー: 4名
- 全ホールのスコア・パット・ペナルティ入力済み
- ドラコン、ニアピン、オリンピック、たてよこを有効化済み
- Par3=150Y、Par4=360Y、Par5=490Y

## 楽天GORA

- Application IDとAccess Keyはアプリ内の「メニュー → 楽天GORA設定」で入力する。
- 認証情報はソースコードへ直接保存せず、端末内の専用設定へ保存する。
- 楽天Webサービス側で楽天GORA APIの利用許可が必要。
- 公開ページ: `https://busan78.github.io/golf-score/`
- Access Keyのコピー時に先頭文字が欠けるとHTTP 403 `Invalid Access Key`になるため注意する。
- ゴルフ場選択時に楽天GORA公式Webのコース情報ページも取得する。Webページ構造が変更された場合は距離の自動取得に失敗する可能性がある。
- Webから取得した情報は選択したラウンドのJSON内へ保存し、認証情報は含めない。

## 清算方式

- 各ゲームの獲得点について、各プレーヤーがほかの全員と点差を清算する。
- 個人の清算点は `獲得点 × 参加人数 - 全員の獲得点合計`。
- 清算点の合計は0になる。
- 結果画面にはプラス／マイナスと、誰から誰へ何点渡すかを表示する。
- 現在は金額ではなく「点」で表示する。

## ビルド方法

Android Studioではプロジェクトを開いてGradle Sync後、接続したGalaxy S25を選択して実行する。

コマンドラインで確認する場合:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:GRADLE_USER_HOME='C:\Users\admin\Documents\Codex\2026-08-03\android-android-studio-kotlin-jetpack-compose\work\gradle-cache'
$env:JAVA_TOOL_OPTIONS='-Duser.home=C:\Users\admin\Documents\Codex\2026-08-03\android-android-studio-kotlin-jetpack-compose\work\android-studio-home'
& 'C:\Users\admin\Documents\Codex\2026-08-03\android-android-studio-kotlin-jetpack-compose\work\gradle-9.5.0\bin\gradle.bat' :app:assembleDebug --offline --no-daemon
```

作業ディレクトリは、この `PROJECT_STATUS.md` がある `GolfScoreManager` フォルダにする。

## 開発時の決まり

- 機能変更後はversionNameとversionCodeを更新する。
- 毎回Gradleビルドを行い、エラーがあれば修正する。
- 完了報告にはVersionを記載する。
- 楽天のApplication IDやAccess Keyを会話やソースコードへ掲載しない。
- 既存の端末内データを不用意に削除しない。
- 完成したAPKをPCのダウンロードフォルダへコピーする。
- Googleドライブの同期フォルダが利用できる場合はプロジェクトZIPを保存する。
- Galaxy S25がADB接続中なら完成したAPKをインストールする。
- 完成時はGitHubリポジトリ `busan78/golf-score` の既存公開ページを保持したまま、変更をコミットして `main` へ同期する。

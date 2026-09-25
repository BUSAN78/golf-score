# GolfScoreManager

Kotlin / Jetpack Compose製のオフライン・ゴルフスコア管理アプリです。

- 最大4人、18ホールの打数・パット・OB・バンカーを記録
- プレー日、ゴルフ場、地域、天気、ティーグラウンドを記録
- レディースティーをプレーヤーごとに設定
- 端末内（SharedPreferences内のJSON）に履歴を保存
- 履歴の表示、編集、削除と自動集計
- 楽天GORA公式APIを使ったゴルフ場名・所在地検索（APIキー設定時）

スコア履歴は端末内に保存され、ネット接続なしでも利用できます。インターネット通信は楽天GORAのゴルフ場検索を実行したときだけ使用します。

## 楽天GORAゴルフ場検索の設定

アプリのラウンド履歴画面で「メニュー」→「楽天GORA設定」→「楽天公式の取得手順を開く」を押すと、楽天ウェブサービスの公式ガイドがブラウザで開きます。ガイド内の「registering a new application」または「New App」から楽天へログインして新規アプリを登録し、発行されたApplication IDとAccess Keyをアプリへ戻って入力してください。設定は端末内だけに保存され、保存直後から検索に利用できます。登録ページを直接開くと楽天ログインでリダイレクトエラーになる場合があるため、必ず公式ガイドを経由してください。

開発時に初期値を設定したい場合のみ、`local.properties` の末尾へ次の値を追加することもできます。

```properties
rakuten.applicationId=取得したApplication ID
rakuten.accessKey=取得したAccess Key
```

`local.properties` を変更した場合は、Android Studioで「Sync Project with Gradle Files」を実行してください。未設定またはネット接続がない場合も、ゴルフ場名を手入力してスコア管理を利用できます。

楽天ウェブサービスの公開GORA APIには、楽天会員の過去ラウンド／スコア履歴を取得するAPIがありません。そのため本アプリは楽天のログイン情報を収集せず、履歴の自動取得も行いません。

## Android Studioで実行

1. Android Studioの `Open` で、この `GolfScoreManager` フォルダを選択します。
2. Gradle Syncの完了を待ちます。SDKの確認が出た場合はAPI 36を選択します。
3. Galaxy S25で「開発者向けオプション」と「USBデバッグ」を有効にし、USB接続後に端末側の接続許可を承認します。
4. Android Studio上部の実行対象でGalaxy S25を選択し、Run（▶）を押します。

生成済みデバッグAPKは `app/build/outputs/apk/debug/app-debug.apk` です。

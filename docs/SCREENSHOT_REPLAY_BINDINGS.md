# スクショ保存音・リプレイ表示再現・複数入力

上流 ED #250、beatoraja #685/#794を参考にしたArena本体の変更。Issue #495。
ソース実装と自動検証が対象で、配布・実機受入は別工程。

## スクリーンショット保存音

起動前設定の「その他」に「スクショ保存音」と「スクショ保存音量」を追加。
Classic/Sidebar共通、初期OFF、音量初期50%。音量はシステム音量に対する割合。
画像の保存成功後に一度だけ鳴り、保存失敗時やSNS書出しでは鳴らない。
音量0、システム音量0では消音する。

サウンドテーマの `screenshot.wav`（対応するogg/mp3等も既存音声探索の対象）を
優先し、なければ既存の設定変更音 `o-change.wav` を使う。新しい第三者音源は
同梱しない。通知音は描画スレッドへ渡して再生する。
JPGの初回保存でも保存先ディレクトリを作り、JPEG writerの失敗を成功扱いしない。

## リプレイ表示設定

起動前設定の「リプレイの表示設定を再現」は初期ON。
開始時の表示設定と曲中の変更を記録・再生する。変更履歴は操作キーではなく
実際の表示状態を譜面時計のマイクロ秒で保存し、再生速度の変更にも追従する。

対象はHS、duration、HI-SPEED FIX、SUD+、LIFT、HIDDENの値/有効状態、CONSTANTの
表示/fade、LR2固定HSの有効状態・基準BPM・基準スクロール値。
判定方式、判定調整、譜面変形、キー入力ログ、スコアIDは表示履歴から復元しない。

- 旧リプレイで開始configがある場合は開始表示だけ復元し、存在しない履歴を推測しない。
- configがない、または不正な場合は現在の設定で表示する。
- 未対応の履歴versionや不正なeventは表示履歴として除外し、通常keylogを壊さない。
- 同一譜面時刻の複数変更は最後の状態を保存。履歴は最大100,000変更。
- 自動再現中のHS/カバー操作は抑制する。停止、Esc、NUM1〜4の再生速度変更は使える。
- 再生中の表示状態をプレイヤーの保存設定へ書き戻さない。
- OFFでは従来どおり現在の表示設定と手動操作を使える。
- コースはステージ別の開始設定/履歴。retryは新しいplay sessionとして記録する。
- NO_SPEED制約があるコースでは復元で制約を回避しない。
- リプレイのHSだけを借りて通常プレイする既存操作は、元履歴を自動再生しない。

## 複数キーバインド

ゲーム内Key Configの各キー数モードで設定する。初期OFF。
対象はプレイ鍵盤/皿方向、START、SELECT。キーボード、登録済みコントローラー、
MIDIのNOTE/PITCH入力に、既存primaryに加えて最大7個の追加入力を登録できる。
MIDIは既存の共有NOTE/PITCH名前空間を使用し、ポート別割当は追加しない。
マウス皿は従来のprimary割当を使う。追加登録中のマウス移動では既存割当を変更せず、
キーボード・コントローラー・MIDIの入力待ちを継続する。

| 操作 | 内容 |
|---|---|
| `[4]` | このモードの複数入力ON/OFF |
| `[5]` | 選択行に追加入力を登録。複数入力をONにする |
| `[6]` | 選択行の追加入力をまとめて削除 |
| `[0]` | 選択行の追加入力一覧に表示するデバイスを切り替える |
| `Enter` | 選択行の割当を置き換える |
| `Delete` | 選択行の割当と追加入力を削除 |
| `Esc` | 登録中ならキャンセル、通常時は保存して選曲へ戻る |
| `[7]/[8]/[9]` | 対応デバイスの既存初期割当へ戻し、追加入力も解除 |

列の `+N` は追加入力数。右側の一覧で具体的な入力を確認できる。
F5/Esc/方向キー等の固定操作用予約キーは従来どおり割当対象外。
上流#794に含まれる予約キーの全面自由化やメニューhotkey再設計は今回含まない。

どれか一つの入力が押されていれば論理キーは押下中とする。
複数の入力を押しても判定/keylogへ余分な押下を送り直さず、最後の入力を離した時に
解除する。このため、別の割当キーを離しても押下中のLNを切らない。
START/SELECTも同じOR方式で、キーボード捕捉やcontroller切断では他sourceの押下を保つ。

同じ物理入力を複数レーンへ割り当てない。追加割当の重複は検証で除外し、実効入力も
一つの物理入力につき一操作を選ぶ。Controllerのアナログ値はprimaryを優先し、
primaryがない場合は先頭の追加入力を使用する。

OFF時のデバイス優先順位は従来のKeyboard→Controller→MIDI。
優先順位を実効設定のコピーへ適用し、保存済みのprimaryや追加入力は消さない。
key profileのALL/CURRENT_MODE/KEYS_ONLY適用にも複数入力の設定を含める。

## 自動検証とoperator受入

自動テストは別キーの重なり/解除、Keyboard/Controller/MIDIの実入力処理、sourceの
解除、START/SELECT、JSON設定保持、重複入力、replayの表示snapshot/event、旧形式、
単曲/コースの実brd保存読込を確認する。全core suiteと対象macOSビルドも実行する。

配布候補でのoperator確認:

1. PNG/JPG保存成功、書込失敗、音ON/OFF/0/テーマ音/fallback音、画像コピーを確認。
2. 曲中にHS・カバー・LIFT/HIDDENを変え、通常/倍速/コースのリプレイで一致を確認。
3. 旧リプレイ、NO_SPEED、HS借用の通常プレイ、停止後の普段の設定を確認。
4. 複数キーでLNを保持して一つずつ離す。controller切断と再接続、START/SELECTを確認。
5. 追加一覧、登録/取消/解除、OFF→ON、再起動、key profile、overlay編集中の入力を確認。

CodexによるGUI操作・ゲーム本体/ランチャー起動は行わない。

## English

Screenshot feedback is optional (off by default), plays once after a successful local save,
and uses a theme screenshot sound with the option-change sound as a fallback.
Replay lane restoration is on by default, restores recorded display state on the chart clock,
and never writes replay settings back to the player configuration. Old replays with a config
restore their initial display state; replays without one retain the current-settings fallback.
Multiple play inputs are opt-in per mode. Add inputs in Key Config with `[5]`, toggle with `[4]`,
clear added inputs with `[6]`, and inspect another device with `[0]`. Logical keys remain held
until the last assigned input releases. Fixed recovery/navigation keys stay reserved.

# 上流監査で残った9件の不具合修正 (#497)

便利機能PR #496を保持し、利用者が追加で指定したバグ群を一つの修正セットで扱う。
ソース・自動検証が対象。配布、実機受入、productionのデータ修復は別工程。

| 対象 | 修正 |
|---|---|
| ED #242 IR設定消失 | 保存時の構造検証からmodule可用性を分離。未検出IRも設定画面の候補/保存対象へ残し、認証情報・送信/受信設定を保持。実行時は接続不能moduleをskip |
| ED #216 正規化音量 | LUFS gainをユーザーKey/BG音量へそれぞれ適用。設定0を保持し、解析失敗/非有限LUFSは設定値へfallback。変更された音量も各経路で参照 |
| ED #271 ゲージ復元 | DECIDE分岐の前で元ゲージを保存。省略ON/OFFの両経路を通す。リザルト/コースの復元機構は保持 |
| ED #87 Judge Trainer | 折り畳みでBeginがfalseでも対応するEndを呼ぶ |
| beatoraja #871 点滅alpha | 元描画色の値を退避し、描画後/例外時に復元。Colorへの参照退避による漏れを防ぐ |
| beatoraja #616 BMSON layer | 標準 `{y,id}` を通常layerへ格納し、次イベントまで保持。静止画は黒色抜きを使わずPNG alphaを保持 |
| rian #31 BMP00 | time0の既定MISSへdense BMP00 IDを設定。明示06と後続の動的MISSを優先 |
| rian #34 旧BMS空白構文 | 元byteを変換せずpayload開始位置を正しく解析。小節長も同じ規則。通常コロン/BASE/RANDOMは維持 |
| ED #193 孤立LNOBJ | 直前が完成済みLN終端のケースだけ、対応不能LNOBJを通常ノーツとして保持。正常LN変換/未完LN接続/前noteなしは既存規則 |

BMSONの未確定な独自PLAY/ALWAYS/sequence拡張の全対応は含めない。
区切りを一切持たないBMS構文の新しい解釈も導入しない。
パーサーの対応PRを本体PRへリンクし、固定gitlinkを更新する。

## 解析互換性とデータ保護

空白構文の実添付例は旧解析466/864ノーツ、正しい構文位置の解析では230/387ノーツ。
同じSHAのPBを更新するだけでは、古い高EX/ランプを新notesへ付け替えてしまう。
このため解析変更で影響する譜面を単位として、ローカル保存キーを `bmsir-parser-v2:` の
SHA-256から派生させる。**モデルの公開MD5/SHAは変えない**。

- 単曲/コースPB・playlog・削除・local cacheは同じローカル識別を使う。
- 旧score.dbの元行、履歴、元brdを削除・移動・新notesへ再解釈しない。
- 新しいlive playのlocal SHAを使い、BMS-IR pluginのPB/log参照も新slotにそろえる。
  公開送信はIRChartDataの元MD5/SHA。IRScoreData.sha256はlocal lookup key。
- 空白構文は未選択RANDOM分岐も調べ、全分岐の保存先を統一する。条件分岐を持つ
  LNOBJ譜面は未選択分岐で影響する可能性があるため保守的に同じ新キーへ分離する。
  分岐の全組合せ列挙やランダムな保存先の切替は行わない。
- それ以外の通常コロン構文・正常LNOBJ・BMP00/BMSON表示修正だけの譜面は従来の保存キー。
- IRのmemory/persistent cacheとnormalization cacheも影響する解析を分ける。
- 非LN譜面の旧尺度のrival cacheはnotes不一致なら表示しない。LN譜面では
  authored CN/HCNとForce LNのnotesが異なるため、catalog notesの一律比較をしない。
  ローカルPBは派生キーと既存のForce LN/selector分離で保護する。
- 楽曲DBに `parsercompatibility` メタデータを保存する。更新前のDBでは次の通常
  楽曲更新時に、ファイル日時が同じでも解析を刷新する。自動更新をOFFにしている
  環境では更新後にF2の楽曲更新を行う。無関係なPBは同じキーのまま。

既存のstandalone score.db importは派生SHAのraw chartへの逆引きを持たないため、
該当する新slotは従来のsongdata照合ではskipされる。live uploadと混同しない。
原SHAの旧行も新notesと不一致ならimport対象にしない。勝手な行移動/metadata捏造は行わない。
原SHAを直接joinする独自SQLは旧行を参照する。利用者のSQLを自動で書き換えない。

## 旧リプレイ

新replayは解析revision 2を保存。項目がない旧fileはrevision 1として旧解析を選ぶ。
不明revisionは安全に再生を断り、ファイルを削除しない。

current slotがなければparser成分を含まない旧slotを先に検索し、Force LN/MANIACの
識別を保つ。コースにも同じfallbackを使う。
影響するreplayは、判定用モデル・resourceの表示notes・音声/BGAを記録時の解析に
そろえる。先行media loaderの完了を後続の背景threadで待ち、描画threadをjoinしない。
thumbnail GL textureはこの再bindで作り直さない。
current slotの削除からfallback先の旧brdを巻き込まない。

## 検証

自作最小BMS/BMSON、元添付の構文比較、非GUI検証を使う。元配布の楽曲/音源/画像は
repoへ追加しない。
key/BG音量比と消音、missing IR保存/再読込、旧PBの高EX非混入、単曲/コースbrdと
旧revision再解析、楽曲日時不変のメタデータ刷新、BASE/RANDOM/正常LN、BMP00の
dense ID/明示06/後続保持、標準BMSON layer/poor同pulseを検証する。
Judge Trainerはplatform backend/OS window/rendererを持たないCPU ImGui frameで
折り畳み/展開のBegin/Endを確認する。

入力・保存・解析が跨るため、最終の独立読み取りレビューを1回行う。
最終core suite、macOS build、feature parity、repository CIで検証する。

operator確認:

1. IR moduleを退避→起動/設定保存→復帰し、設定/認証/primaryが保持されること。
2. 正規化ONでKey/BGを別音量、0、音量変更、解析失敗、対象ASIO/OpenALで確認。
3. DECIDE ON/OFF、GAS、retry、コース、replayで元ゲージが正しく戻ること。
4. Judge Trainerの折り畳み/展開/終了、点滅ゲージの後続粒のalphaを確認。
5. BMSON静止画/動画layer、BMP00と明示06、seek/retry/MISS終了復帰を確認。
6. F2のDB更新後に旧空白構文と孤立LNOBJのnotes、保存されていたPB/brd、
   新旧replay、通常IR取得/送信、LR2 ghost、RANDOM branchを確認。

Codexによるゲーム/launcher起動、GUI操作、productionデータ修復は行わない。

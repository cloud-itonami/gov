# gov — 公的機関の**公開**参照レイヤ（agency / official / municipality）

`gov` という名前は機能を示さないので、まず名乗る。

**この repo は、政府機関・公務員・自治体という「公開されている行政の台帳」を
AT PDS レコードとして登録・検索・集計するための参照実装である。**
市民個人の相談内容（医療・福祉・教育の相談履歴＝ PII）は**ここには無いし、
入れない** — それは consent-capability を持つ etzhayyim 側基盤に残る
（`kotoba/src/types.ts` の SPLIT NOTE、ADR-2605172400 の custody 軸）。

つまり `gov` が扱うのは「厚生労働省という組織が在る」であって
「誰が何を相談した」ではない。

- 動く実装と手順: [`docs/operator-quickstart.md`](docs/operator-quickstart.md)
- 正準メタデータ: `README.edn`（`:canonical-metadata :edn`）。この README は
  人間向けの入口であって、機械可読な正本ではない。

## この repo に入っている 4 つの木

`git ls-files` は 24 ファイル。うち**実際にビルド・テストできるのは
`kotoba/` だけ**である。

| 木 | 中身 | 状態 |
|---|---|---|
| `kotoba/` | TS package `@etzhayyim/gov-kotoba`。src 461 行 + test 55 行、vitest | **動く**（手順は quickstart） |
| `appview/gov-mcp-component/` | Cloudflare Worker + SvelteKit（`gov.etzhayyim.com`） | 部分移植。`SUBSTRATE-PORT-PENDING.md` 参照 |
| `scaffold/actor-manifest.jsonld` | 別 actor「Gov Coverage Orchestrator」の宣言（Cypher pipeline + cron） | 宣言のみ。実行体はこの repo に無い |
| `bpmn/gov-services.bpmn` | BPMN プロセス定義（zeebe task type） | 宣言のみ。エンジンはこの repo に無い |

**ルート直下に `src/` も `test/` も無い。** 探しているものは `kotoba/src/` と
`kotoba/test/` に在る（この repo を機械で走査するものを書くときの注意）。

## `kotoba/` が公開している API

`@etzhayyim/sdk` の `Etzhayyim` インスタンスを第 1 引数に取る 8 関数
（`kotoba/src/index.ts`）:

```
registerAgency  getAgency  listAgencies          — 政府機関（COFOG コード / 親子 FK）
recordOfficial  listOfficials                    — 公務員（FK → agency）
registerMunicipality  listMunicipalities         — 自治体（JIS コード / 人口）
coverage                                         — 3 コレクションの集計
```

コレクション名は `com.etzhayyim.apps.gov.{agency,official,municipality}`、
DID は `did:web:gov.etzhayyim.com:{agency,official,muni}:{id}`
（`kotoba/src/types.ts`）。

検証は「拒否できること」まで実装されている — `level` が語彙外なら
`rejected`、親 agency が居なければ `parentNotFound`、JIS コードが 5 桁でなければ
`rejected`、人口が負なら `rejected`。テストはこの拒否側も固定している。

## 読む前に知っておくべきこと — 識別子が 3 通りある

**この repo の中で、同じ「gov」が 3 つの異なる identity を名乗っている。**
どれが現行かはこの repo の中身だけからは決まらないので、参照する前に確認すること。

| | nanoid | ACTOR_DID | コレクション名前空間 |
|---|---|---|---|
| `kotoba/` | — | `did:web:gov.etzhayyim.com:…` | `com.etzhayyim.apps.gov.*` |
| `appview/…/kotodama.jsonld` + `src/app.ts` | `gv7ps2m1` | `did:web:etzhayyim.com:gov` | `com.etzhayyim.gov.*` |
| `scaffold/actor-manifest.jsonld` | `g0v9a1cy` | `did:web:gov.etzhayyim.com` | `com.etzhayyim.apps.gov.*` |

repo 自体の名前も 3 通りある: west/GitHub は `cloud-itonami/gov`、
`README.edn` は `com-etzhayyim-app-gov`、`migration.edn` の宛先は
`etzhayyim/com-etzhayyim-app-gov`。**git remote が実在を持つ**（前者）。

### 「未完了」と書いてある項目のうち 3 つは、矢印の両辺が既に同じ文字列

一括置換が矢印の両側を書き換えた結果と読める。**残作業として読むと、
何をすればよいか決まらない**ので、そのまま着手しないこと:

- `SUBSTRATE-PORT-PENDING.md` §3 — `@etzhayyim/kotodama-gv7ps2m1` → `@etzhayyim/kotodama-gv7ps2m1`
- `CLAUDE.md` WIT 節 / 同 §「Deliberately preserved」 — `etzhayyim:gov/public-service@1.0.0` → 同左
- `appview/gov-mcp-component/src/app.ts` 冒頭 — `@etzhayyim/kotodama-host-sdk` → 同左

同様に、既存ドキュメントが指す `00-contracts/lexicons/…`・`wit/gov/package.wit`・
`90-docs/rules/compliance/…` は**この repo には無い**（抽出元の
`etzhayyim/root` 側のパス）。`migration.edn` の `:source` が抽出元と revision を持つ。

## 境界 — 隣の repo との関係

- **市民の相談 PII**（`submitConsult` / consult コレクション）は etzhayyim 側基盤。
  ここには型も関数も置かない。
- **`@etzhayyim/sdk`（AT PDS 読み書き）** は git 依存として外から入る。
  この repo は SDK を実装しない。
- `appview/` の XRPC は `mcp.etzhayyim.com` へ転送する（`+server.ts`）。
  MCP ルータはこの repo に無い。

## 状態

履歴は 1 commit（`0d2e24d chore: extract gov app from root`、`etzhayyim/root` の
`60-apps/etzhayyim-project-gov` から 22 ファイル / 68,350 バイトを抽出）。
substrate 移植は **PARTIAL（2026-05-24 時点）** — 残りは
`SUBSTRATE-PORT-PENDING.md`。

ライセンス: `kotoba/package.json` が Apache-2.0。

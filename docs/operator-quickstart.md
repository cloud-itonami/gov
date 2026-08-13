# Operator Quickstart — gov

clone から**緑のテスト**までの最短経路。

この repo で実際にビルド・テストできるのは **`kotoba/` だけ**である
（`appview/` は部分移植、`scaffold/` と `bpmn/` は宣言のみ。README.md の表）。
以下は `kotoba/` の手順。

**実測環境**（2026-08-13 にこの手順を通したときの値。他の環境での所要時間は違う）:
macOS / darwin 25.3.0 (arm64) · Node **v26.3.0** · npm **11.16.0**

## 前提

- Node 20 以上（実測は v26.3.0）
- **GitHub `etzhayyim` org への SSH 読み取り権限。**
  依存 2 本（`@etzhayyim/sdk` / `@etzhayyim/sdk-mock`）は `package.json` に
  `git+https://…` と書かれているが、npm は `ssh://git@github.com/etzhayyim/…` に
  解決する。SSH 鍵が通らないとここで止まる。
- Java / Clojure は不要（この木は TypeScript）。

## 1. clone

```bash
git clone git@github.com:cloud-itonami/gov.git
cd gov/kotoba
```

## 2. 依存を入れる

```bash
npm install
```

**実測: 5 分 2 秒 / node_modules 311 MB / `found 0 vulnerabilities`。**
長いのは git 依存 2 本を nested install して `tsc` でビルドするため
（`@etzhayyim/sdk` は `atproto-client` `base-l2` `checkpointer` `ipfs` `pqh`
`witness-quorum` を芋づるで引く）。初回だけ。

### ⚠ `~/.npmrc` に `allow-scripts[]=` があると必ず失敗する

このワークスペースの標準的な `~/.npmrc` は
`allow-scripts[]=@anthropic-ai/claude-code` を持っている。npm 11 は git 依存を
準備するとき入れ子の `npm install` を **project スコープ**で走らせるので、
user 設定の `allow-scripts[]` がそこに継承されて拒否される:

```
npm error code EALLOWSCRIPTS
npm error --allow-scripts is not allowed in project-scoped installs.
```

**これは repo の欠陥ではなく手元の設定**である。回避は、その行だけ落とした
npmrc を渡す（`~/.npmrc` を書き換えない）:

```bash
grep -v '^allow-scripts' ~/.npmrc > /tmp/npmrc-gov
npm install --userconfig /tmp/npmrc-gov
```

インストール自体は成功する。`allow-scripts` 未承認の警告が 8 件出るが、
git 依存の `prepare: tsc` は準備段階で既に走っているのでテストは通る。

## 3. テスト

```bash
npm test          # vitest run
```

**期待する出力: `Test Files 1 passed (1)` / `Tests 3 passed (3)`**（実測 217 ms）。

3 本が固定しているのは agency の親子 FK・語彙外 `level` の拒否・
official → agency の FK・自治体の JIS コードと人口の検証・`coverage` の集計。
`MockEtzhayyim`（`@etzhayyim/sdk-mock`）を使うので **PDS もネットワークも要らない**。

## 4. 型検査

```bash
npm run typecheck # tsc --noEmit
```

無出力・exit 0 が期待値（実測どおり）。`src/**` のみが対象で `test/` は入らない
（`tsconfig.json` の `include`）。

## 5. この検査が本当に効いていることを確かめる

緑を信じる前に、**赤くできることを一度見る**。`registry.ts` の親 agency 実在
チェックを無効化する:

```bash
cp src/registry.ts /tmp/registry.ts.orig
perl -0pi -e 's/\Qif (input.parentAgencyId && !(await exists(e, AGENCY_COLLECTION, agencyRkey(input.parentAgencyId)))) {\E/if (false) {/' src/registry.ts
npm test        # → Tests 1 failed | 2 passed
cp /tmp/registry.ts.orig src/registry.ts   # 必ず戻す
npm test        # → Tests 3 passed (3)
```

赤くなったとき、報告は**壊した場所を名指しする**はずである（実測）:

```
AssertionError: expected 'registered' to be 'parentNotFound'
 ❯ test/gov.test.ts:25
```

`test/gov.test.ts:25` は `parentAgencyId: "GHOST"` の行。ここが一致しない赤は
別の何かを壊している。

## この quickstart が扱わないもの

**`appview/gov-mcp-component/` のビルドとデプロイは、ここでは通していない。**
着手する前に、少なくとも次の 2 つが要ることを見ておくこと:

- `wrangler.jsonc` の `main` は `svelte/.svelte-kit/cloudflare/_worker.js` を指す。
  これは commit されていない生成物なので、先に `svelte/` 側で
  `npm install && npm run build`（SvelteKit + `@sveltejs/adapter-cloudflare`）が要る。
- `npm run deploy` は `wrangler` ではなく **`etzhayyim` CLI** を呼ぶ。
  これはこの repo に無く、実測でも PATH に無かった（`wrangler` 自体は在った）。

`scaffold/actor-manifest.jsonld` と `bpmn/gov-services.bpmn` は宣言であって、
それを実行するエンジン（k8s-langserver / zeebe）はこの repo に無い。

## 制約

- 市民の相談 PII をこの repo のコレクションに入れない（README.md の境界節）。
- force-push しない。
- 秘密情報を repo に置かない。`@etzhayyim/sdk` の署名鍵・PDS URL は
  実行時 env（`appview/…/src/app.ts` の `env.SIGNER` / `env.PDS_URL`）。

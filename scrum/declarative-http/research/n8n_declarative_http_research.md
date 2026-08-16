# Research — n8n's Declarative HTTP System & Real-World Usage

> Purpose: study how n8n builds HTTP integrations **declaratively** (no imperative request code in each node), so we can extract the _user requirements_ a Java core library must satisfy to recreate the same capability for the Java ecosystem.
>
> Sources: `packages/workflow/src/interfaces.ts` (`IHttpRequestOptions`, `DeclarativeRestApiSettings`, `INodePropertyRouting`, `PostReceiveAction`, …), `packages/core/src/execution-engine/routing-node.ts` (runtime engine), `packages/core/src/execution-engine/node-execution-context/utils/request-helpers/*` (credential + OAuth orchestration), `packages/@n8n/backend-network/src/http/**` (HTTP transport), and real nodes in `packages/nodes-base/nodes/**`.

---

## 1. The Big Idea

n8n lets a node author declare **"what an API call looks like"** — method, URL, headers, query params, body mapping, response shaping, pagination, auth — and a generic **RoutingNode** engine executes it. The node author writes **zero imperative HTTP code**. Instead they write a _description_ (metadata), and the engine:

1. walks the node's parameter tree to discover which routing fragments apply (based on user-selected resource/operation/fields),
2. **merges** all applicable fragments into one request plan,
3. resolves **expressions** (per input item) inside every value,
4. runs a **pipeline**: `preSend → (auth) → HTTP transport → postReceive`,
5. returns a list of **items** (`{ json, binary, pairedItem }`) for downstream nodes.

This is the "declarative HTTP" model we want to port to Java as a **reusable core library** — the same request-planning engine, minus the workflow/editor coupling.

```
┌─────────────────────────────┐      ┌─────────────────────────────────────────────┐
│  Node description (static)  │      │  RoutingNode (runtime, per input item)      │
│  requestDefaults            │      │                                             │
│  requestOperations          │      │  walk params ──► merge fragments            │
│  properties[].routing       │      │  resolve expressions (item/run context)     │
│  credentials[].authenticate │ ───► │  ResultOptions { options, preSend,          │
└─────────────────────────────┘      │                   postReceive, paginate,   │
                                     │                   maxResults,               │
                                     │                   requestOperations }       │
                                     │         │                                   │
                                     │         ▼                                   │
                                     │  makeRequest: preSend[] ─► (paginate?)      │
                                     │    ─► httpRequest[WithAuthentication]       │
                                     │    ─► postProcessResponseData: postReceive[]│
                                     │    ─► INodeExecutionData[]                  │
                                     └─────────────────────────────────────────────┘
```

---

## 2. The Two Configuration Layers

### 2.1 `IHttpRequestOptions` — the _canonical HTTP request_ (transport contract)

```ts
export interface IHttpRequestOptions {
	url: string;
	baseURL?: string;
	headers?: IDataObject;
	method?: 'DELETE' | 'GET' | 'HEAD' | 'PATCH' | 'POST' | 'PUT';
	body?: FormData | GenericValue | GenericValue[] | Buffer | URLSearchParams;
	qs?: IDataObject;                       // query params
	arrayFormat?: 'indices' | 'brackets' | 'repeat' | 'comma';
	auth?: { username: string; password: string; sendImmediately?: boolean };
	disableFollowRedirect?: boolean;
	maxRedirects?: number;                  // ignored whens disableFollowRedirect=true
	encoding?: 'arraybuffer' | 'blob' | 'document' | 'json' | 'text' | 'stream';
	skipSslCertificateValidation?: boolean;
	returnFullResponse?: boolean;
	ignoreHttpStatusErrors?: boolean | IgnoreStatusErrorConfig; // {ignore, except[]}
	proxy?: { host: string; port: number; auth?: {...}; protocol?: string };
	timeout?: number;
	json?: boolean;                         // sets Accept: application/json
	abortSignal?: GenericAbortSignal;       // cancellation
	sendCredentialsOnCrossOriginRedirect?: boolean; // default true (bc)
	allowedDomains?: string;                // SSRF guard, comma-separated
	agentOptions?: Omit<AgentOptions, 'socket' | 'lookup'>; // TLS/agent tuning
}
```

**What it covers (from `backend-network/src/http/axios/request.ts`):**

| Concern         | Behavior                                                                                                                                                                                        |
| --------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| URL composition | `buildTargetUrl(url, baseURL)` — `baseURL` + `url` joined                                                                                                                                       |
| Query string    | `qs` → axios `params`; `arrayFormat` custom serializer (repeat/brackets/indices/comma)                                                                                                          |
| Body            | FormData (auto content-type via `getHeaders()`), `URLSearchParams` (`application/x-www-form-urlencoded`), plain object/string/array; **empty body removed on GET/HEAD/OPTIONS**                 |
| Headers         | user headers kept; content-type only set if absent; `json` flag adds `Accept: application/json` if not already set; default outbound User-Agent applied                                         |
| Auth            | basic via `auth` (+ `sendImmediately` for preemptive), digest computed from 401 `WWW-Authenticate` challenge                                                                                    |
| Redirects       | axios `maxRedirects`; when SSRF-guarded, **manual redirect following** with per-hop DNS validation and **credential stripping on cross-origin** (unless `sendCredentialsOnCrossOriginRedirect`) |
| SSL             | `skipSslCertificateValidation` → TLS agent options                                                                                                                                              |
| Timeout         | `timeout` (RoutingNode defaults to **300 000 ms**)                                                                                                                                              |
| Cancellation    | `abortSignal` → axios `signal` (execution cancel)                                                                                                                                               |
| Status handling | `ignoreHttpStatusErrors` (all, or `except` list)                                                                                                                                                |
| SSRF/egress     | `validateUrlSsrf`, DNS secure lookup, `throwIfDomainNotAllowed(allowedDomains)`                                                                                                                 |
| Full response   | `returnFullResponse` → `IN8nHttpFullResponse { body, headers, statusCode, statusMessage }`                                                                                                      |

### 2.2 `DeclarativeRestApiSettings.ResultOptions` — the _request plan_ (engine contract)

```ts
export namespace DeclarativeRestApiSettings {
  export type HttpRequestOptions = Override<
    IHttpRequestOptions,
    { skipSslCertificateValidation?: string | boolean; url?: string }
  >;
  export type ResultOptions = {
    maxResults?: number | string; // cap returned items
    options: HttpRequestOptions; // merged request
    paginate?: boolean | string; // enable pagination
    preSend: PreSendAction[]; // (requestOptions) => requestOptions
    postReceive: Array<{
      data: { parameterValue };
      actions: PostReceiveAction[];
    }>;
    requestOperations?: IN8nRequestOperations; // pagination config
  };
}
```

The `RoutingNode` accumulates one `ResultOptions` per input item by **walking the node's parameter tree** (`getRequestOptionsFromParameters`) and merging every fragment that is active given the current parameter values (`mergeOptions`). Fragments come from:

- `nodeType.description.requestDefaults` (node-level, applied first)
- each parameter's `routing` (`request`, `send`, `output`, `operations`)
- per-operation options (collection / fixedCollection / options) discovered while walking

---

## 3. The Runtime Engine — `routing-node.ts`

`RoutingNode.runNode()` per input item:

1. `prepareCredentials()` — resolves which credential description applies (single, or by `authentication` parameter `displayOptions`) and decrypts it.
2. Reads per-node `requestOptions` params: `batching.batch.{batchSize, batchInterval}`, `proxy`, `timeout`, `allowUnauthorizedCerts`.
3. Starts with `nodeType.description.requestDefaults`, resolving each value as an expression.
4. Walks `nodeType.description.properties` → `getRequestOptionsFromParameters` → merge into the plan.
5. `makeRequest()`:
   - run every `preSend` action (`(requestOptions) => requestOptions`) — **can rewrite the request per item**;
   - if `paginate` and `requestOperations.pagination` → paginate (function, `generic`, or `offset`);
   - else single `rawRoutingRequest` = `httpRequestWithAuthentication(credentialType, options)` or `httpRequest(options)`;
   - `postProcessResponseData` → run `postReceive` actions, or default mapping (object → 1 item; array → items).
6. Collects all item results; on errors, wraps in `NodeApiError` (with httpCode, itemIndex/runIndex), honors `continueOnFail`, adds a 429 hint.
7. Applies `maxResults` cap.

### The pipeline, precisely

```
plan.options ──► [preSend₁ … preSendₙ]  ──►  authenticated transport  ──► IN8nHttpFullResponse
                                                       │
                                                       ▼
                        postProcessResponseData:  [postReceive₁ … postReceiveₙ] ──► INodeExecutionData[]
                        (or default: body-object → one item / body-array → items)
```

### `getRequestOptionsFromParameters` — the merge rules (critical semantics)

- **`displayOptions`** gate: a property is skipped entirely unless it should be displayed for the current parameter values (`NodeHelpers.displayParameter`). → _routing is conditional on state_.
- **`routing.request`** — each key merged into `options` (url, method, headers, qs, body, json…), values expression-resolved.
- **`routing.send`** — maps the parameter's _value_ into the request:
  - `{ type: 'body'|default(query), property, propertyInDotNotation, value }`;
  - `value` override lets a constant/expression be sent instead of the param value;
  - `propertyInDotNotation === false` → set literally; else dot-notation set (`set(obj, 'a.b', v)`).
  - `preSend` on send are appended to the plan's preSend list.
  - `paginate` on send toggles pagination.
- **`routing.output`** — `maxResults`, and `postReceive` (with `enabled` expression filter).
- **`routing.operations`** — pagination strategy (merged shallowly).
- Child traversal: `options`, `collection`, `fixedCollection` (with `multipleValues` index path `path[i]` and `$index`/`$parent` context keys).

### Expression context keys observed in the wild

`$parameter`, `$credentials`, `$response`, `$responseItem`, `$value`, `$version`, `$request`, `$index`, `$parent`, `$env`, `$json`, `$item`, `$now`, `$today`, … — every string starting with `=` or every object value is run through the expression engine per item.

---

## 4. Real-World Usage Catalog (evidence from actual nodes)

### 4.1 `requestDefaults` — node-level defaults

| Node           | Declared                                                                                                         |
| -------------- | ---------------------------------------------------------------------------------------------------------------- |
| `Brevo`        | `baseURL: 'https://api.brevo.com'`                                                                               |
| `Npm`          | `baseURL: '={{ $credentials.registryUrl }}'` _(expression from credential)_                                      |
| `Gong`         | `baseURL: '={{ $credentials.baseUrl.replace(...) }}'` _(expression w/ transform)_                                |
| `HighLevel v2` | `baseURL: 'https://services.leadconnectorhq.com'` + `headers: { Accept, 'Content-Type', Version: '2021-07-28' }` |
| `Adalo`        | `baseURL: '=https://api.adalo.com/v0/apps/{{$credentials.appId}}'`                                               |
| `Pipedrive v1` | `baseURL: 'https://api.pipedrive.com/v1', url: ''`                                                               |

### 4.2 `routing.request` — per-operation request shape

- `Brevo` email ops: `{ method: 'POST', url: '/v3/smtp/email' }`, `/v3/smtp/emailTemplate`.
- `Adalo` create: `{ method: 'POST', url: '=/collections/{{$parameter["collectionId"]}}' }` — **URL templated from a parameter**.
- `Brevo` contact ops: `{ method: 'POST', url: '/v3/contacts' }`, `upsert` uses `url: '=/v3/contacts'` (expression form).

### 4.3 `routing.send` — parameter value → request mapping

- `Brevo` email fields: `subject`, `textContent`, `htmlContent`, `sender`, … each `{ routing: { send: { property, type: 'body' } } }`.
- Resource locator / query params default to **query** when `type` is not `'body'`.
- `propertyInDotNotation: false` for literal key names.

### 4.4 `routing.output` + `postReceive` — response shaping

- `Brevo` `getAll`: `postReceive: [ { type: 'rootProperty', properties: { property: 'contacts' } } ]` — unwrap a collection root.
- `Brevo` `update`: `postReceive: [ { type: 'setKeyValue', properties: { … } } ]` — reshape each item.
- `Brevo` loadOptions: `setKeyValue { name, value }` → `sort { key: 'name' }` — **turn API rows into dropdown options**.
- `routing.output.maxResults` — cap.
- Full `PostReceiveAction` type set: `rootProperty`, `filter`, `limit`, `set`, `sort`, `setKeyValue`, `binaryData`, **or an arbitrary function** `(items, response) => items`.

### 4.5 Pagination

- **offset (node-level)** — `Adalo`: `{ type: 'offset', properties: { limitParameter: 'limit', offsetParameter: 'offset', pageSize: 100, type: 'query' } }`.
- **offset (operation-level)** — `Brevo` getAll: `pageSize: 1000, type: 'query'` (+ optional `rootProperty`).
- **custom function** — `HighLevel` `highLevelApiPagination`: reads node params (`returnAll`, `resource`), uses `this.makeRoutingRequest(requestData)` to loop, advances a **cursor** (`startAfterId`, `startAfter`) from `meta`, stops when `total <= collected`; respects `returnAll=false` (single page).
- **generic** — routing-node: `continue` expression (with `$request`/`$response` keys) + per-request `request` override; used for cursor/link-based APIs.

### 4.6 Credentials / authentication

- `BrevoApi.credentials.ts`: `authenticate: { type: 'generic', properties: { headers: { 'api-key': '={{$credentials.apiKey}}' } } }` + `test: { request: { baseURL, url: '/account' } }`.
- `HttpHeaderAuth.credentials.ts`: dynamic header name/value from credential fields; `genericAuth = true`.
- `httpRequestWithAuthentication` orchestrates: get credential → `preAuthentication` (pre-auth / refresh-on-401) → `authenticate` → transport. OAuth1/OAuth2 are separate branches.
- **`allowedDomains`** derived from the credential (`getCredentialAllowedDomains`) → SSRF guard.

### 4.7 Design-time (metadata) HTTP — `loadOptions` / `listSearch` / resource locators

The _same_ routing machinery powers fetching dropdown options **before execution**: `typeOptions.loadOptions.routing.request` + `output.postReceive` (e.g. Brevo contact attributes: `rootProperty` → `setKeyValue` → `sort`), and resource-locator `entryTypes.data.request`. → The core must expose a **"describe/query" mode** reusing the same planner.

### 4.8 Transport / security behaviors worth porting

- default timeout 300 s; batching throttle (`batchSize`/`batchInterval`) between items;
- proxy from node param or credential; `skipSslCertificateValidation`;
- manual redirect following with DNS re-validation + **credential stripping cross-origin**;
- `ignoreHttpStatusErrors` (all | except);
- body empty-removal on GET/HEAD/OPTIONS;
- `returnFullResponse` for binary/postReceive pipelines;
- cancellation via abort signal;
- SSRF URL + DNS guards, `allowedDomains` allow-list.

---

## 5. Extracted User Requirements (the contract a Java core must meet)

> Each requirement is traced to n8n evidence. These become the acceptance criteria for the Java core library.

### R1 — Declarative request description (no imperative HTTP code)

A developer must be able to describe an API call as data — base URL, per-operation method/URL, headers, query params, body, response shape — without writing request logic. _(§2, §4.1–4.4)_

### R2 — Request defaults merged with per-operation overrides

A base spec (`baseURL`, default `headers`) is merged with operation-level fragments; later fragments override earlier ones (deep merge for objects, append for pipelines). _(§2.2, §4.1)_

### R3 — Conditional routing driven by state

Routing fragments apply only when their preconditions hold (e.g. selected resource/operation/field). The engine must evaluate preconditions before merging. _(§3 `displayOptions`, §4)_

### R4 — Parameter → request mapping (`send`)

Map a field's value into body (literal or dot-notation path) or query; support a value override and per-send hooks. _(§3 `routing.send`, §4.3)_

### R5 — Per-request expression evaluation

Every string value may be an expression resolved against a runtime context (`credentials`, `parameter`, `response`, `value`, env, item data). Must be **pluggable** in Java (no single language mandated). _(§3 context keys, §4.1/4.2)_

### R6 — Pre-send pipeline (request transformation hooks)

Ordered functions that receive the request and may rewrite it per item (e.g. build body from mapped input). _(§3 `preSend`, §4.5 Adalo `presendCreateUpdate`)_

### R7 — Post-receive pipeline (response shaping)

Ordered actions that transform the raw response into output records: `rootProperty`, `filter`, `limit`, `set`, `sort`, `setKeyValue`, `binaryData`, plus custom functions. _(§3, §4.4)_

### R8 — Output capping

A `maxResults` cap applied to the final item list. _(§2.2, §3)_

### R9 — Pagination strategies

Built-in **offset** (pageSize, limit/offset param, query|body, rootProperty) and **cursor/generic** (continue-expression + per-page request override), plus a **custom strategy hook** with access to the request loop and node context. _(§4.5)_

### R10 — Credential abstraction & auth injection

Credentials decoupled from request spec: generic header/query/basic injection from credential fields, plus pluggable OAuth1/OAuth2 flows with token refresh on 401. _(§4.6)_

### R11 — Rich transport contract (parity with `IHttpRequestOptions`)

URL + baseURL composition, headers, query with `arrayFormat`, body types (form, urlencoded, JSON, raw, binary), basic/digest auth, proxy, TLS skip, redirect policy, timeouts, cancellation, status-error policy, full-response mode, JSON Accept header. _(§2.1)_

### R12 — Security-first defaults

SSRF guards (URL + DNS), `allowedDomains` allow-list, credential stripping on cross-origin redirect, default `sendCredentialsOnCrossOriginRedirect` behavior, secrets never logged. _(§2.1, §4.8)_

### R13 — Error model

Structured API errors carrying HTTP status, node/operation context, per-item index; friendly 429 handling; option to continue-on-error per item. _(§3 step 6)_

### R14 — Cancellation & lifecycle

Abort signal propagation from an execution/request scope; support for streaming responses and binary payloads. _(§2.1, §4.8)_

### R15 — Metadata/design-time mode

Reuse the same planner to fetch dropdown/search options before execution (loadOptions/listSearch), so the library can also power autocomplete UIs. _(§4.7)_

### R16 — Deterministic, testable engine

The engine must be a pure function of (spec, context, transport) so unit tests can mock the transport and assert the exact request plan + output records. _(observed test structure: `routing-node.test.ts`, `HighLevelApiPagination.test.ts`)_

# k6 Load Tests

Issue scope:

- #31 and #32 under parent issue #6 for time-sale and coupon concurrency load verification.
- #95 for product search v1/v2 load comparison.

- `timesale-order-load.js`: `POST /api/v1/timesales/{timeSaleId}/orders`
- `coupon-issue-load.js`: `POST /api/v1/coupons/{couponPolicyId}/issue`
- `product-search-v1-load.js`: `GET /api/v1/products`
- `product-search-v2-load.js`: `GET /api/v2/products`

The scripts assume that seed data, members, login, active time sale, and active coupon policy are prepared before the run. They do not create application data because setup ownership belongs to other issues and local environments may use different seed strategies.

Product search scripts also assume that comparable product data is already loaded. For issue #95, load at least 50,000 products and run v1 and v2 with the same keyword, category, page, size, stage profile, and load generator host.

## Product Search v1/v2

The product search scripts use the same common scenario and differ only by endpoint version:

- v1 measures `GET /api/v1/products`, which uses the non-cache product search path.
- v2 measures `GET /api/v2/products`, which uses the configured product search cache path. Set `PRODUCT_SEARCH_CACHE_MODE=REMOTE` on the application process when measuring Redis Remote Cache.

Default ramp profile:

```text
30s:20,1m:50,1m:100,30s:0
```

Override it with:

```powershell
$env:STAGES = "30s:50,1m:100,1m:200,30s:0"
```

Common inputs:

```powershell
$env:BASE_URL = "http://localhost:8080"
$env:SEARCH_KEYWORD = "cache-target"
$env:CATEGORY_ID = ""
$env:PAGE = "0"
$env:SIZE = "20"
$env:STAGES = "30s:20,1m:50,1m:100,30s:0"
```

Run v1:

```powershell
$env:SUMMARY_DIR = "docs/performance/k6/results/product-search-v1"
k6 run docs/performance/k6/product-search-v1-load.js
```

Run v2 with Redis Remote Cache enabled on the application:

```powershell
$env:PRODUCT_SEARCH_CACHE_MODE = "REMOTE"
$env:WARMUP_REQUESTS = "20"
$env:SUMMARY_DIR = "docs/performance/k6/results/product-search-v2"
k6 run docs/performance/k6/product-search-v2-load.js
```

Collected product search metrics:

- `product_search_successes`
- `product_search_failures`
- `product_search_error_rate`
- `product_search_response_time`
- k6 built-ins including `http_reqs`, `http_req_duration`, `http_req_failed`, `dropped_iterations`, and `vus_max`

The generated Markdown summary reports average response time, p95, p99, throughput, failure rate, and dropped iterations. Use dropped iterations, rising failure rate, and sharply increasing p95/p99 as saturation signals.

The comparison report for issue #95 is maintained in `.agents/wiki-work/ProductSearchK6Comparison.md`.

## Token Input

Pass one buyer token:

```powershell
$env:AUTH_TOKEN = "<buyer-access-token>"
```

Or pass a JSON file containing buyer tokens:

```json
{
  "tokens": [
    "<buyer-1-access-token>",
    "<buyer-2-access-token>"
  ]
}
```

Then set:

```powershell
$env:AUTH_TOKENS_FILE = "D:\path\to\tokens.json"
```

For realistic 500 to 1,000 concurrent requests, prepare enough distinct buyer tokens. The current domain policy allows one time-sale order and one coupon issue per member, so reusing a single token intentionally produces duplicate-member rejections.

## Time Sale Order

```powershell
$env:BASE_URL = "http://localhost:8080"
$env:TIME_SALE_ID = "1"
$env:EXPECTED_STOCK = "1000"
$env:VUS = "500"
$env:REQUESTS = "500"
$env:RUN_ID = "mvp3-trackb-timesale-001"
$env:RUN_DUPLICATE_PROBE = "true"
$env:PROBE_AUTH_TOKEN = "<dedicated-buyer-token-not-used-by-main-load>"
k6 run docs/performance/k6/timesale-order-load.js
```

Collected metrics:

- `timesale_successful_orders`
- `timesale_failed_orders`
- `timesale_lock_failures`
- `timesale_response_time`
- `timesale_duplicate_successes`
- `timesale_duplicate_probe_failures`

Automatic guards:

- successful orders must not exceed `EXPECTED_STOCK`
- successful order responses must contain `data.id`
- duplicate `Request-ID` probe success count must remain `0` when enabled
- duplicate probe first request must succeed when enabled
- Redis outage or lock acquisition failures are recorded as `timesale_lock_failures` when the API returns `503`
- response time is collected but not thresholded because the policy is still undecided

## Coupon Issue

```powershell
$env:BASE_URL = "http://localhost:8080"
$env:COUPON_POLICY_ID = "1"
$env:EXPECTED_COUPON_QUANTITY = "1000"
$env:VUS = "500"
$env:REQUESTS = "500"
$env:RUN_ID = "mvp3-trackb-coupon-001"
$env:RUN_DUPLICATE_PROBE = "true"
$env:PROBE_AUTH_TOKEN = "<dedicated-buyer-token-not-used-by-main-load>"
k6 run docs/performance/k6/coupon-issue-load.js
```

Collected metrics:

- `coupon_successful_issues`
- `coupon_failed_issues`
- `coupon_lock_failures`
- `coupon_response_time`
- `coupon_duplicate_successes`
- `coupon_duplicate_probe_failures`

Automatic guards:

- successful issues must not exceed `EXPECTED_COUPON_QUANTITY`
- successful issue responses must contain `data.id`
- duplicate `Request-ID` probe success count must remain `0` when enabled
- duplicate probe first request must succeed when enabled
- Redis outage or lock acquisition failures are recorded as `coupon_lock_failures` when the API returns `503`
- response time is collected but not thresholded because the policy is still undecided

## Summary Output

By default, each run writes k6 summaries under:

```text
docs/performance/k6/results
```

Override the output directory with:

```powershell
$env:SUMMARY_DIR = "docs/performance/k6/results/local-run-001"
```

Result files are generated by k6 at runtime and are not required to be committed for this implementation issue.

## Scope Notes

- Issue #32 adds a coupon facade-level test (`CouponIssueFacadeTest`) that verifies Request-ID duplicate rejection **before** lock acquisition, matching the "block the Request-ID in Redis first" contract, plus Redis/lock Fail-Closed behavior.
- The time-sale facade (`TimeSaleOrderFacade`) checks the Request-ID **after** acquiring the lock (`tryLock()` then `checkAndSet()`), so its duplicate-Request-ID rejection differs from the coupon pre-lock contract and is not newly verified against that contract in this PR. Treat time-sale Request-ID blocking as covered only in its current post-lock form by the pre-existing `TimeSaleOrderFacadeTest`.
- The k6 duplicate probe reuses the same buyer token and the same Request-ID for both requests, so an externally observed second-request failure cannot distinguish a Request-ID reuse rejection from the existing one-issue/order-per-member policy rejection. Use the facade-level tests for the precise Request-ID guarantee; the probe only confirms that a repeated request does not succeed twice.
- The k6 scripts record externally observable `503` lock failure responses. Redis outage and lock-expiration conditions must be injected by the runtime environment while the scripts run.
- Lock expiration release safety is covered at the `RedissonLockManager` boundary by verifying that unlock is skipped when the current thread no longer owns the lock.

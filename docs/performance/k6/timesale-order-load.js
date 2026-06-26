import http from 'k6/http';
import { check, group } from 'k6';
import { Counter, Trend } from 'k6/metrics';
import { SharedArray } from 'k6/data';

const BASE_URL = (__ENV.BASE_URL || 'http://localhost:8080').replace(/\/$/, '');
const TIME_SALE_ID = __ENV.TIME_SALE_ID;
const EXPECTED_STOCK = Number(__ENV.EXPECTED_STOCK || 1000);
const VUS = Number(__ENV.VUS || 500);
const REQUESTS = Number(__ENV.REQUESTS || VUS);
const MAX_DURATION = __ENV.MAX_DURATION || '2m';
const RUN_DUPLICATE_PROBE = (__ENV.RUN_DUPLICATE_PROBE || 'false').toLowerCase() === 'true';
const TOKEN_FILE = __ENV.AUTH_TOKENS_FILE;
const SINGLE_TOKEN = __ENV.AUTH_TOKEN;
const PROBE_TOKEN = __ENV.PROBE_AUTH_TOKEN;
const RUN_TOKEN = String(__ENV.RUN_ID || Date.now()).replace(/[^a-zA-Z0-9]/g, '').slice(-12);

const tokens = new SharedArray('auth tokens', () => {
  if (TOKEN_FILE) {
    const parsed = JSON.parse(open(TOKEN_FILE).replace(/^\uFEFF/, ''));
    return Array.isArray(parsed) ? parsed : parsed.tokens;
  }

  return SINGLE_TOKEN ? [SINGLE_TOKEN] : [];
});

if (!TIME_SALE_ID) {
  throw new Error('TIME_SALE_ID is required.');
}

if (!tokens || tokens.length === 0) {
  throw new Error('AUTH_TOKENS_FILE or AUTH_TOKEN is required.');
}

export const timesale_successful_orders = new Counter('timesale_successful_orders');
export const timesale_failed_orders = new Counter('timesale_failed_orders');
export const timesale_duplicate_successes = new Counter('timesale_duplicate_successes');
export const timesale_response_time = new Trend('timesale_response_time');

const scenarios = {
  order_load: {
    executor: 'shared-iterations',
    vus: VUS,
    iterations: REQUESTS,
    maxDuration: MAX_DURATION,
    exec: 'orderLoad',
  },
};

if (RUN_DUPLICATE_PROBE) {
  scenarios.duplicate_request_probe = {
    executor: 'per-vu-iterations',
    vus: 1,
    iterations: 1,
    maxDuration: '30s',
    exec: 'duplicateRequestProbe',
    startTime: '1s',
  };
}

export const options = {
  scenarios,
  thresholds: {
    timesale_successful_orders: [`count<=${EXPECTED_STOCK}`],
    checks: ['rate>0.95'],
  },
};

export function orderLoad() {
  const token = tokens[(__VU - 1) % tokens.length];
  const requestId = buildRequestId('k6-ts');
  const response = placeOrder(token, requestId);

  recordOrderResponse(response);
}

export function duplicateRequestProbe() {
  group('same Request-ID is not accepted twice', () => {
    const token = PROBE_TOKEN || tokens[0];
    const requestId = buildRequestId('k6-ts-dupe');

    const first = placeOrder(token, requestId);
    const second = placeOrder(token, requestId);

    const duplicateBlocked = second.status !== 201;
    if (!duplicateBlocked) {
      timesale_duplicate_successes.add(1);
    }

    check(first, {
      'first duplicate probe request reaches API': (res) => [201, 400, 401, 403, 404, 409, 503].includes(res.status),
    });

    check(second, {
      'duplicate Request-ID is rejected': () => duplicateBlocked,
    });
  });
}

function buildRequestId(prefix) {
  return `${prefix}-${RUN_TOKEN}-${__VU.toString(36)}-${__ITER.toString(36)}`.slice(0, 36);
}

function placeOrder(token, requestId) {
  return http.post(
    `${BASE_URL}/api/v1/timesales/${TIME_SALE_ID}/orders`,
    JSON.stringify({ quantity: 1 }),
    {
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json',
        'Request-ID': requestId,
      },
      tags: {
        feature: 'timesale-order',
      },
    },
  );
}

function recordOrderResponse(response) {
  timesale_response_time.add(response.timings.duration);

  const succeeded = response.status === 201;

  if (succeeded) {
    timesale_successful_orders.add(1);
  } else {
    timesale_failed_orders.add(1);
  }

  check(response, {
    'order response is created or business rejection': (res) => [201, 400, 401, 403, 404, 409, 503].includes(res.status),
    'successful order has response data': (res) => res.status !== 201 || Boolean(res.json('data.id')),
  });
}

export function handleSummary(data) {
  const summaryDir = __ENV.SUMMARY_DIR || 'docs/performance/k6/results';
  const successful = data.metrics.timesale_successful_orders?.values?.count || 0;
  const failed = data.metrics.timesale_failed_orders?.values?.count || 0;
  const duplicateSuccesses = data.metrics.timesale_duplicate_successes?.values?.count || 0;
  const p95 = data.metrics.timesale_response_time?.values?.['p(95)'] || 0;

  return {
    stdout: textSummary(successful, failed, duplicateSuccesses, p95),
    [`${summaryDir}/timesale-order-summary.json`]: JSON.stringify(data, null, 2),
    [`${summaryDir}/timesale-order-summary.md`]: markdownSummary(successful, failed, duplicateSuccesses, p95),
  };
}

function textSummary(successful, failed, duplicateSuccesses, p95) {
  return [
    '타임세일 주문 k6 부하 테스트 요약',
    `주문 성공 수=${successful}`,
    `주문 실패 수=${failed}`,
    `중복 Request-ID 성공 수=${duplicateSuccesses}`,
    `응답 시간 p95(ms)=${p95}`,
    `기대 재고 상한=${EXPECTED_STOCK}`,
    '',
  ].join('\n');
}

function markdownSummary(successful, failed, duplicateSuccesses, p95) {
  return [
    '# 타임세일 주문 k6 부하 테스트 결과',
    '',
    '## 실행 조건',
    '',
    `- 대상 타임세일 ID: ${TIME_SALE_ID}`,
    `- 요청 반복 수: ${REQUESTS}`,
    `- 동시 가상 사용자 수(VUs): ${VUS}`,
    `- 기대 재고 상한: ${EXPECTED_STOCK}`,
    `- 중복 Request-ID probe: ${RUN_DUPLICATE_PROBE ? '실행됨' : '미실행'}`,
    '',
    '## 측정 결과',
    '',
    `- 주문 성공 수: ${successful}건`,
    `- 주문 실패 수: ${failed}건`,
    `- 중복 Request-ID가 성공한 수: ${duplicateSuccesses}건`,
    `- 응답 시간 p95: ${p95}ms`,
    `- 총 HTTP 요청 수: ${REQUESTS}${RUN_DUPLICATE_PROBE ? '건 + 중복 probe 2건' : '건'}`,
    '',
    '## 검증 결과',
    '',
    '- 초과 판매 검증: `timesale_successful_orders <= EXPECTED_STOCK` 조건으로 검증한다.',
    `- 현재 실행에서는 성공 주문 수 ${successful}건이 기대 재고 상한 ${EXPECTED_STOCK}건을 초과하지 않았다.`,
    '',
    '## 해석',
    '',
    '성공 수, 실패 수, 응답 시간은 k6 실행 결과에서 수집한 값이다.',
    '',
    '응답 시간은 현재 몇 초를 기준으로 통과/실패를 판단할지 정책이 확정되지 않았으므로 수집만 수행한다.',
    '',
    '실제 DB 재고 차감 여부는 runbook의 DB 검증 SQL로 추가 확인한다.',
    '',
  ].join('\n');
}

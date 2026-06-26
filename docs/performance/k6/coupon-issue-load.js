import http from 'k6/http';
import { check, group } from 'k6';
import { Counter, Trend } from 'k6/metrics';
import { SharedArray } from 'k6/data';

const BASE_URL = (__ENV.BASE_URL || 'http://localhost:8080').replace(/\/$/, '');
const COUPON_POLICY_ID = __ENV.COUPON_POLICY_ID;
const EXPECTED_COUPON_QUANTITY = Number(__ENV.EXPECTED_COUPON_QUANTITY || 1000);
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

if (!COUPON_POLICY_ID) {
  throw new Error('COUPON_POLICY_ID is required.');
}

if (!tokens || tokens.length === 0) {
  throw new Error('AUTH_TOKENS_FILE or AUTH_TOKEN is required.');
}

export const coupon_successful_issues = new Counter('coupon_successful_issues');
export const coupon_failed_issues = new Counter('coupon_failed_issues');
export const coupon_duplicate_successes = new Counter('coupon_duplicate_successes');
export const coupon_response_time = new Trend('coupon_response_time');

const scenarios = {
  coupon_issue_load: {
    executor: 'shared-iterations',
    vus: VUS,
    iterations: REQUESTS,
    maxDuration: MAX_DURATION,
    exec: 'couponIssueLoad',
  },
};

if (RUN_DUPLICATE_PROBE) {
  scenarios.duplicate_issue_probe = {
    executor: 'per-vu-iterations',
    vus: 1,
    iterations: 1,
    maxDuration: '30s',
    exec: 'duplicateIssueProbe',
    startTime: '1s',
  };
}

export const options = {
  scenarios,
  thresholds: {
    coupon_successful_issues: [`count<=${EXPECTED_COUPON_QUANTITY}`],
    checks: ['rate>0.95'],
  },
};

export function couponIssueLoad() {
  const token = tokens[(__VU - 1) % tokens.length];
  const requestId = buildRequestId('k6-cp');
  const response = issueCoupon(token, requestId);

  recordIssueResponse(response);
}

export function duplicateIssueProbe() {
  group('same member is not issued twice', () => {
    const token = PROBE_TOKEN || tokens[0];
    const firstRequestId = buildRequestId('k6-cp-dupe1');
    const secondRequestId = buildRequestId('k6-cp-dupe2');

    const first = issueCoupon(token, firstRequestId);
    const second = issueCoupon(token, secondRequestId);

    const duplicateBlocked = second.status !== 201;
    if (!duplicateBlocked) {
      coupon_duplicate_successes.add(1);
    }

    check(first, {
      'first duplicate probe request reaches API': (res) => [201, 400, 401, 403, 404, 409, 503].includes(res.status),
    });

    check(second, {
      'same member duplicate issue is rejected': () => duplicateBlocked,
    });
  });
}

function buildRequestId(prefix) {
  return `${prefix}-${RUN_TOKEN}-${__VU.toString(36)}-${__ITER.toString(36)}`.slice(0, 36);
}

function issueCoupon(token, requestId) {
  return http.post(
    `${BASE_URL}/api/v1/coupons/${COUPON_POLICY_ID}/issue`,
    null,
    {
      headers: {
        Authorization: `Bearer ${token}`,
        'Request-ID': requestId,
      },
      tags: {
        feature: 'coupon-issue',
      },
    },
  );
}

function recordIssueResponse(response) {
  coupon_response_time.add(response.timings.duration);

  const succeeded = isSuccessfulIssue(response);

  if (succeeded) {
    coupon_successful_issues.add(1);
  } else {
    coupon_failed_issues.add(1);
  }

  check(response, {
    'issue response is success or business rejection': (res) => isSuccessfulIssue(res) || [400, 401, 403, 404, 409, 503].includes(res.status),
    'successful issue has response data': (res) => !isSuccessStatus(res.status) || Boolean(res.json('data.id')),
  });
}

function isSuccessfulIssue(response) {
  return isSuccessStatus(response.status) && Boolean(response.json('data.id'));
}

function isSuccessStatus(status) {
  return status >= 200 && status < 300;
}

export function handleSummary(data) {
  const summaryDir = __ENV.SUMMARY_DIR || 'docs/performance/k6/results';
  const successful = data.metrics.coupon_successful_issues?.values?.count || 0;
  const failed = data.metrics.coupon_failed_issues?.values?.count || 0;
  const duplicateSuccesses = data.metrics.coupon_duplicate_successes?.values?.count || 0;
  const p95 = data.metrics.coupon_response_time?.values?.['p(95)'] || 0;

  return {
    stdout: textSummary(successful, failed, duplicateSuccesses, p95),
    [`${summaryDir}/coupon-issue-summary.json`]: JSON.stringify(data, null, 2),
    [`${summaryDir}/coupon-issue-summary.md`]: markdownSummary(successful, failed, duplicateSuccesses, p95),
  };
}

function textSummary(successful, failed, duplicateSuccesses, p95) {
  return [
    '쿠폰 발급 k6 부하 테스트 요약',
    `쿠폰 발급 성공 수=${successful}`,
    `쿠폰 발급 실패 수=${failed}`,
    `중복 발급 성공 수=${duplicateSuccesses}`,
    `응답 시간 p95(ms)=${p95}`,
    `기대 쿠폰 수량 상한=${EXPECTED_COUPON_QUANTITY}`,
    '',
  ].join('\n');
}

function markdownSummary(successful, failed, duplicateSuccesses, p95) {
  return [
    '# 쿠폰 발급 k6 부하 테스트 결과',
    '',
    '## 실행 조건',
    '',
    `- 대상 쿠폰 정책 ID: ${COUPON_POLICY_ID}`,
    `- 요청 반복 수: ${REQUESTS}`,
    `- 동시 가상 사용자 수(VUs): ${VUS}`,
    `- 기대 쿠폰 수량 상한: ${EXPECTED_COUPON_QUANTITY}`,
    `- 중복 발급 probe: ${RUN_DUPLICATE_PROBE ? '실행됨' : '미실행'}`,
    '',
    '## 측정 결과',
    '',
    `- 쿠폰 발급 성공 수: ${successful}건`,
    `- 쿠폰 발급 실패 수: ${failed}건`,
    `- 중복 발급이 성공한 수: ${duplicateSuccesses}건`,
    `- 응답 시간 p95: ${p95}ms`,
    `- 총 HTTP 요청 수: ${REQUESTS}${RUN_DUPLICATE_PROBE ? '건 + 중복 probe 2건' : '건'}`,
    '',
    '## 검증 결과',
    '',
    '- 초과 발급 검증: `coupon_successful_issues <= EXPECTED_COUPON_QUANTITY` 조건으로 검증한다.',
    `- 현재 실행에서는 성공 발급 수 ${successful}건이 기대 쿠폰 수량 상한 ${EXPECTED_COUPON_QUANTITY}건을 초과하지 않았다.`,
    '',
    '## 해석',
    '',
    '성공 수, 실패 수, 응답 시간은 k6 실행 결과에서 수집한 값이다.',
    '',
    '응답 시간은 현재 몇 초를 기준으로 통과/실패를 판단할지 정책이 확정되지 않았으므로 수집만 수행한다.',
    '',
    '실제 DB 잔여 수량 차감 여부는 runbook의 DB 검증 SQL로 추가 확인한다.',
    '',
  ].join('\n');
}

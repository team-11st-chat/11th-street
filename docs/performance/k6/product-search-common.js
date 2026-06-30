import http from 'k6/http';
import { check } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

const BASE_URL = (__ENV.BASE_URL || 'http://localhost:8080').replace(/\/$/, '');
const SEARCH_KEYWORD = __ENV.SEARCH_KEYWORD || 'cache-target';
const CATEGORY_ID = __ENV.CATEGORY_ID || '';
const PAGE = Number(__ENV.PAGE || 0);
const SIZE = Number(__ENV.SIZE || 20);
const GUEST_ID_PREFIX = __ENV.GUEST_ID_PREFIX || 'k6-product-search';
const STAGES = parseStages(__ENV.STAGES || '30s:20,1m:50,1m:100,30s:0');
const MIN_CHECK_RATE = Number(__ENV.MIN_CHECK_RATE || 0.95);
const MAX_ERROR_RATE = Number(__ENV.MAX_ERROR_RATE || 0.01);
const RUN_ID = String(__ENV.RUN_ID || Date.now()).replace(/[^a-zA-Z0-9-]/g, '').slice(-32);

export const product_search_successes = new Counter('product_search_successes');
export const product_search_failures = new Counter('product_search_failures');
export const product_search_error_rate = new Rate('product_search_error_rate');
export const product_search_response_time = new Trend('product_search_response_time');

export function buildOptions(apiVersion) {
  return {
    scenarios: {
      product_search_ramp: {
        executor: 'ramping-vus',
        stages: STAGES,
        gracefulRampDown: __ENV.GRACEFUL_RAMP_DOWN || '10s',
        exec: 'searchLoad',
        tags: {
          endpoint: `/api/${apiVersion}/products`,
          apiVersion,
        },
      },
    },
    thresholds: {
      checks: [`rate>=${MIN_CHECK_RATE}`],
      product_search_error_rate: [`rate<=${MAX_ERROR_RATE}`],
    },
    summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
  };
}

export function warmUp(apiVersion, defaultWarmupRequests) {
  const warmupRequests = Number(__ENV.WARMUP_REQUESTS || defaultWarmupRequests || 0);

  for (let i = 0; i < warmupRequests; i += 1) {
    http.get(buildSearchUrl(apiVersion), {
      headers: buildHeaders(i),
      tags: {
        endpoint: `/api/${apiVersion}/products`,
        apiVersion,
        phase: 'warmup',
      },
    });
  }

  return {
    apiVersion,
    warmupRequests,
    searchUrl: buildSearchUrl(apiVersion),
  };
}

export function runSearch(apiVersion) {
  const response = http.get(buildSearchUrl(apiVersion), {
    headers: buildHeaders(__ITER),
    tags: {
      endpoint: `/api/${apiVersion}/products`,
      apiVersion,
      phase: 'load',
    },
  });

  product_search_response_time.add(response.timings.duration);

  const succeeded = isSuccessfulSearchResponse(response);
  product_search_error_rate.add(!succeeded);

  if (succeeded) {
    product_search_successes.add(1);
  } else {
    product_search_failures.add(1);
  }

  check(response, {
    'search response is HTTP 200': (res) => res.status === 200,
    'search response has page data': (res) => isSuccessfulSearchResponse(res),
  });
}

export function summarize(apiVersion, data) {
  const summaryDir = __ENV.SUMMARY_DIR || 'docs/performance/k6/results';
  const baseName = `product-search-${apiVersion}-summary`;
  const metrics = extractMetrics(data);

  return {
    stdout: textSummary(apiVersion, metrics),
    [`${summaryDir}/${baseName}.json`]: JSON.stringify(data, null, 2),
    [`${summaryDir}/${baseName}.md`]: markdownSummary(apiVersion, metrics),
  };
}

function buildSearchUrl(apiVersion) {
  const params = [
    ['keyword', SEARCH_KEYWORD],
    ['page', PAGE],
    ['size', SIZE],
  ];

  if (CATEGORY_ID) {
    params.push(['categoryId', CATEGORY_ID]);
  }

  const query = params
    .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(value)}`)
    .join('&');

  return `${BASE_URL}/api/${apiVersion}/products?${query}`;
}

function buildHeaders(iteration) {
  const vu = typeof __VU === 'undefined' ? 0 : __VU;

  return {
    'Request-Guest-ID': `${GUEST_ID_PREFIX}-${RUN_ID}-${vu}-${iteration}`,
  };
}

function isSuccessfulSearchResponse(response) {
  if (response.status !== 200) {
    return false;
  }

  try {
    return Array.isArray(response.json('data.content'));
  } catch (error) {
    return false;
  }
}

function parseStages(rawStages) {
  return rawStages.split(',')
    .map((stage) => stage.trim())
    .filter(Boolean)
    .map((stage) => {
      const [duration, target] = stage.split(':');
      if (!duration || target === undefined || Number.isNaN(Number(target))) {
        throw new Error(`Invalid STAGES entry: ${stage}. Use duration:target, for example 30s:20.`);
      }
      return {
        duration: duration.trim(),
        target: Number(target),
      };
    });
}

function extractMetrics(data) {
  return {
    successes: count(data, 'product_search_successes'),
    failures: count(data, 'product_search_failures'),
    errorRate: rate(data, 'product_search_error_rate'),
    requests: count(data, 'http_reqs'),
    requestRate: value(data, 'http_reqs', 'rate'),
    avgMs: value(data, 'product_search_response_time', 'avg'),
    p95Ms: value(data, 'product_search_response_time', 'p(95)'),
    p99Ms: value(data, 'product_search_response_time', 'p(99)'),
    droppedIterations: count(data, 'dropped_iterations'),
    maxVus: value(data, 'vus_max', 'value'),
  };
}

function count(data, metricName) {
  return value(data, metricName, 'count');
}

function rate(data, metricName) {
  return value(data, metricName, 'rate');
}

function value(data, metricName, key) {
  return data.metrics[metricName]?.values?.[key] || 0;
}

function formatNumber(number, digits = 2) {
  return Number(number || 0).toFixed(digits);
}

function textSummary(apiVersion, metrics) {
  return [
    `Product search ${apiVersion} k6 summary`,
    `successes=${metrics.successes}`,
    `failures=${metrics.failures}`,
    `error_rate=${formatNumber(metrics.errorRate * 100)}%`,
    `avg_ms=${formatNumber(metrics.avgMs)}`,
    `p95_ms=${formatNumber(metrics.p95Ms)}`,
    `p99_ms=${formatNumber(metrics.p99Ms)}`,
    `throughput_req_s=${formatNumber(metrics.requestRate)}`,
    `dropped_iterations=${metrics.droppedIterations}`,
    '',
  ].join('\n');
}

function markdownSummary(apiVersion, metrics) {
  return [
    `# Product Search ${apiVersion} k6 Load Test Result`,
    '',
    '## Run Conditions',
    '',
    `- Endpoint: /api/${apiVersion}/products`,
    `- Base URL: ${BASE_URL}`,
    `- Keyword: ${SEARCH_KEYWORD}`,
    `- Category ID: ${CATEGORY_ID || 'not set'}`,
    `- Page: ${PAGE}`,
    `- Size: ${SIZE}`,
    `- Stages: ${STAGES.map((stage) => `${stage.duration}:${stage.target}`).join(',')}`,
    `- Run ID: ${RUN_ID}`,
    '',
    '## Metrics',
    '',
    `- Successful responses: ${metrics.successes}`,
    `- Failed responses: ${metrics.failures}`,
    `- Failure rate: ${formatNumber(metrics.errorRate * 100)}%`,
    `- Average response time: ${formatNumber(metrics.avgMs)} ms`,
    `- p95 response time: ${formatNumber(metrics.p95Ms)} ms`,
    `- p99 response time: ${formatNumber(metrics.p99Ms)} ms`,
    `- Throughput: ${formatNumber(metrics.requestRate)} req/s`,
    `- Dropped iterations: ${metrics.droppedIterations}`,
    `- Max observed VUs: ${metrics.maxVus}`,
    '',
    '## Saturation Notes',
    '',
    '- Treat rising p95/p99 or increasing failure rate at higher VU stages as saturation signals. (Note: dropped iterations is normally 0 for ramping-vus closed model)',
    '- Compare v1 and v2 only when they use the same keyword, category, page, size, data volume, stage profile, and load generator host.',
    '',
  ].join('\n');
}

import { buildOptions, runSearch, summarize, warmUp } from './product-search-common.js';

const API_VERSION = 'v1';

export const options = buildOptions(API_VERSION);

export function setup() {
  return warmUp(API_VERSION, 0);
}

export function searchLoad() {
  runSearch(API_VERSION);
}

export function handleSummary(data) {
  return summarize(API_VERSION, data);
}

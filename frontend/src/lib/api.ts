export type ApiResponse<T> = {
  message: string;
  data: T;
  code?: string;
};

export type SaleStatus = "ON_SALE" | "SOLD_OUT" | "SUSPENDED";

export type Product = {
  id: number;
  name: string;
  price: number;
  saleStatus: SaleStatus;
  categoryId: number;
};

export type ProductDetail = Product & {
  sellerId: number;
  sellerName?: string | null;
  stockQuantity: number;
};

export type ProductPage = {
  content: Product[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type ProductCreatePayload = {
  name: string;
  categoryId: number;
  price: number;
  stockQuantity: number;
};

export type ProductUpdatePayload = {
  name?: string;
  categoryId?: number;
  price?: number;
  stockQuantity?: number;
  saleStatus?: SaleStatus;
};

export type TimeSale = {
  id: number;
  productId: number;
  originalPrice: number;
  salePrice: number;
  startedAt: string;
  endedAt: string;
  status: "SCHEDULED" | "ONGOING" | "ENDED";
  remainingQuantity: number;
};

export type TimeSaleCreatePayload = {
  productId: number;
  salePrice: number;
  startedAt: string;
  endedAt: string;
  initialQuantity: number;
};

export type TimeSaleUpdatePayload = {
  salePrice?: number;
  startedAt?: string;
  endedAt?: string;
  initialQuantity?: number;
};

export type TimeSaleOrder = {
  id: number;
  timeSaleId: number;
  productId: number;
  quantity: number;
  salePriceSnapshot: number;
  status: "COMPLETED" | "FAILED";
  orderedAt: string;
};

export type PopularKeyword = {
  keyword: string;
  searchCount: number;
};

export type LoginPayload = {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
};

export type IssuedCoupon = {
  id: number;
  couponPolicyId: number;
  memberId: number;
  status: "ISSUED";
  issuedAt: string;
};

export type CouponPolicy = {
  id: number;
  name: string;
  discountType: "PERCENTAGE" | "FIXED_AMOUNT";
  discountValue: number;
  maxDiscountAmount: number | null;
  remainingQuantity: number;
  status: "SCHEDULED" | "ACTIVE" | "ENDED";
};

export type DiscountType = "PERCENTAGE" | "FIXED_AMOUNT";

export type CouponPolicyCreatePayload = {
  name: string;
  discountType: DiscountType;
  discountValue: number;
  maxDiscountAmount: number | null;
  issueStartsAt: string;
  issueEndsAt: string;
  totalQuantity: number;
};

export type ChatRoom = {
  id: number;
  roomType: "PRODUCT" | "CS";
  sellerId: number | null;
  sellerName: string | null;
  createdByMemberId: number;
  csStatus: "WAITING" | "IN_PROGRESS" | "COMPLETED" | null;
  closedAt: string | null;
  createdAt: string;
  updatedAt: string;
};

export type ChatMessage = {
  id: number;
  chatRoomId: number;
  senderId: number | null;
  content: string;
  clientMessageId: string;
  messageType: "TEXT" | "PRODUCT_REFERENCE" | "SYSTEM";
  product?: {
    id: number;
    name: string;
    price: number;
  } | null;
  sentAt: string;
};

export type ChatMessageHistory = {
  content: ChatMessage[];
  nextCursor: number | null;
  hasNext: boolean;
};

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

let accessToken: string | null = null;

const errorMessageByCode: Record<string, string> = {
  TIME_SALE_001: "타임세일 판매 시간이 아닙니다. 시작 전이거나 이미 종료된 상품입니다.",
  TIME_SALE_002: "타임세일 수량이 모두 소진되었습니다.",
  TIME_SALE_003: "이미 구매했거나 같은 주문이 처리 중입니다.",
  INVALID_DISCOUNT_RATE: "타임세일 할인율은 정상가 대비 최소 5% 이상, 100% 미만이어야 합니다.",
  INVALID_SALE_PRICE: "타임세일 특가는 정상가보다 낮고 100원 이상이어야 합니다.",
  INVALID_SALE_PERIOD: "타임세일 종료 시각은 시작 시각보다 이후여야 합니다.",
  INVALID_QUANTITY: "타임세일 한정 판매 수량은 1개 이상이어야 합니다.",
  MODIFICATION_NOT_ALLOWED: "이 타임세일은 현재 상태에서 수정할 수 없습니다.",
  EXTENSION_ONLY_ALLOWED: "진행 중인 타임세일은 종료 시각을 뒤로 연장하는 수정만 가능합니다.",
  TIME_SALE_NOT_FOUND: "타임세일 정보를 찾을 수 없습니다.",
  UNAUTHORIZED_OWNER: "본인 소유 상품 또는 타임세일만 수정할 수 있습니다.",
  COUPON_001: "쿠폰 발급 기간이 아니거나 발급 가능한 상태가 아닙니다.",
  COUPON_002: "선착순 쿠폰 수량이 모두 소진되었습니다.",
  COUPON_003: "이미 발급받았거나 같은 쿠폰 발급 요청이 처리 중입니다.",
  INVALID_COUPON_NAME: "쿠폰명을 확인하세요.",
  INVALID_DISCOUNT_TYPE: "쿠폰 할인 방식을 확인하세요.",
  INVALID_DISCOUNT_VALUE: "쿠폰 할인 값은 정책에 맞는 금액 또는 비율이어야 합니다.",
  INVALID_MAX_DISCOUNT_AMOUNT: "최대 할인 금액을 확인하세요.",
  INVALID_ISSUE_PERIOD: "쿠폰 발급 종료 시각은 시작 시각보다 이후여야 합니다.",
  INVALID_TOTAL_QUANTITY: "선착순 쿠폰 총 수량은 1개 이상이어야 합니다.",
  COUPON_POLICY_NOT_FOUND: "쿠폰 정책을 찾을 수 없습니다.",
  UNAUTHORIZED_ADMIN: "쿠폰 정책은 SUPER_ADMIN 권한으로만 만들 수 있습니다."
};

const errorMessageByServerMessage: Record<string, string> = {
  "판매 기간 외 요청입니다.": errorMessageByCode.TIME_SALE_001,
  "타임세일 한정 판매 수량이 모두 소진되었습니다.": errorMessageByCode.TIME_SALE_002,
  "이미 구매했거나 중복 주문 요청입니다.": errorMessageByCode.TIME_SALE_003,
  "타임세일 할인율은 정상가 대비 최소 5% 이상, 100% 미만이어야 합니다.": errorMessageByCode.INVALID_DISCOUNT_RATE,
  "타임세일 특가는 정상가보다 낮아야 하며, 최소 100원 이상이어야 합니다.": errorMessageByCode.INVALID_SALE_PRICE,
  "종료 시각은 시작 시각보다 이후여야 합니다.": errorMessageByCode.INVALID_SALE_PERIOD,
  "한정 판매 수량은 1개 이상이어야 합니다.": errorMessageByCode.INVALID_QUANTITY,
  "판매 시작 이후에는 수정할 수 없습니다.": errorMessageByCode.MODIFICATION_NOT_ALLOWED,
  "종료 시각 변경은 연장만 가능합니다.": errorMessageByCode.EXTENSION_ONLY_ALLOWED,
  "타임세일 정보를 찾을 수 없습니다.": errorMessageByCode.TIME_SALE_NOT_FOUND,
  "해당 타임세일의 소유자가 아닙니다.": errorMessageByCode.UNAUTHORIZED_OWNER,
  "쿠폰을 발급할 수 있는 상태가 아닙니다.": errorMessageByCode.COUPON_001,
  "선착순 수량이 모두 소진되었습니다.": errorMessageByCode.COUPON_002,
  "이미 발급받았거나 중복 발급 요청입니다.": errorMessageByCode.COUPON_003,
  "쿠폰명이 올바르지 않습니다.": errorMessageByCode.INVALID_COUPON_NAME,
  "할인 방식이 올바르지 않습니다.": errorMessageByCode.INVALID_DISCOUNT_TYPE,
  "할인 값이 올바르지 않습니다.": errorMessageByCode.INVALID_DISCOUNT_VALUE,
  "최대 할인 금액이 올바르지 않습니다.": errorMessageByCode.INVALID_MAX_DISCOUNT_AMOUNT,
  "발급 종료 시각은 시작 시각보다 이후여야 합니다.": errorMessageByCode.INVALID_ISSUE_PERIOD,
  "선착순 총 수량은 1개 이상이어야 합니다.": errorMessageByCode.INVALID_TOTAL_QUANTITY,
  "쿠폰 정책을 찾을 수 없습니다.": errorMessageByCode.COUPON_POLICY_NOT_FOUND,
  "쿠폰 정책은 SUPER_ADMIN 만 등록할 수 있습니다.": errorMessageByCode.UNAUTHORIZED_ADMIN
};

export function setAccessToken(token: string | null) {
  accessToken = token;
}

export function getAccessToken() {
  return accessToken;
}

export function getApiBaseUrl() {
  return API_BASE_URL;
}

function resolveErrorMessage(body: ApiResponse<unknown> | null, status: number) {
  const codeMessage = body?.code ? errorMessageByCode[body.code] : undefined;
  const serverMessage = body?.message;
  const mappedServerMessage = serverMessage ? errorMessageByServerMessage[serverMessage] : undefined;

  return codeMessage ?? mappedServerMessage ?? serverMessage ?? `요청 실패 (${status})`;
}

export async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  if (!headers.has("Content-Type") && init.body) {
    headers.set("Content-Type", "application/json");
  }
  if (accessToken) {
    headers.set("Authorization", `Bearer ${accessToken}`);
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers,
    credentials: "include"
  });
  const body = (await response.json().catch(() => null)) as ApiResponse<T> | null;

  if (!response.ok) {
    throw new Error(resolveErrorMessage(body, response.status));
  }
  return body?.data as T;
}

export async function login(email: string, password: string) {
  const data = await request<LoginPayload>("/api/v1/auth/login", {
    method: "POST",
    body: JSON.stringify({ email, password })
  });
  setAccessToken(data.accessToken);
  return data;
}

export async function registerMember(name: string, email: string, password: string) {
  return request<{ id: number; email: string; name: string }>("/api/v1/members", {
    method: "POST",
    body: JSON.stringify({ name, email, password })
  });
}

export async function refreshToken() {
  const data = await request<LoginPayload>("/api/v1/auth/refresh", { method: "POST" });
  setAccessToken(data.accessToken);
  return data;
}

export async function logout() {
  try {
    await request<void>("/api/v1/auth/logout", { method: "POST" });
  } finally {
    setAccessToken(null);
  }
}

export async function searchProducts(keyword: string, categoryId?: number, useCache = true) {
  const params = new URLSearchParams({ page: "0", size: "60" });
  if (keyword.trim()) params.set("keyword", keyword.trim());
  if (categoryId) params.set("categoryId", String(categoryId));
  return request<ProductPage>(`/api/${useCache ? "v2" : "v1"}/products?${params}`, {
    headers: { "Request-Guest-ID": getGuestId() }
  });
}

export async function getProduct(productId: number) {
  return request<ProductDetail>(`/api/v1/products/${productId}`);
}

export async function createProduct(payload: ProductCreatePayload) {
  return request<ProductDetail>("/api/v1/products", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export async function updateProduct(productId: number, payload: ProductUpdatePayload) {
  return request<ProductDetail>(`/api/v1/products/${productId}`, {
    method: "PATCH",
    body: JSON.stringify(payload)
  });
}

export async function getPopularKeywords() {
  return request<PopularKeyword[]>("/api/v1/popular-keywords");
}

export async function getTimeSales() {
  const data = await request<TimeSale[] | { content: TimeSale[] }>("/api/v1/timesales?page=0&size=20");
  return Array.isArray(data) ? data : data.content;
}

export async function getTimeSale(timeSaleId: number) {
  return request<TimeSale>(`/api/v1/timesales/${timeSaleId}`);
}

export async function orderTimeSale(timeSaleId: number) {
  return request<TimeSaleOrder>(`/api/v1/timesales/${timeSaleId}/orders`, {
    method: "POST",
    headers: { "Request-ID": crypto.randomUUID() },
    body: JSON.stringify({ quantity: 1 })
  });
}

export async function createTimeSale(payload: TimeSaleCreatePayload) {
  return request<TimeSale>("/api/v1/timesales", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export async function updateTimeSale(timeSaleId: number, payload: TimeSaleUpdatePayload) {
  return request<TimeSale>(`/api/v1/timesales/${timeSaleId}`, {
    method: "PATCH",
    body: JSON.stringify(payload)
  });
}

export async function issueCoupon(couponPolicyId: number) {
  return request<IssuedCoupon>(`/api/v1/coupons/${couponPolicyId}/issue`, {
    method: "POST",
    headers: { "Request-ID": crypto.randomUUID() }
  });
}

export async function getCouponPolicy(couponPolicyId: number) {
  return request<CouponPolicy>(`/api/v1/coupons/${couponPolicyId}`);
}

export async function getCouponPolicies() {
  return request<CouponPolicy[]>("/api/v1/coupons");
}

export async function createCouponPolicy(payload: CouponPolicyCreatePayload) {
  return request<CouponPolicy>("/api/v1/coupons", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export async function createProductChatRoom(productId: number) {
  return request<ChatRoom>("/api/v1/chatrooms/products", {
    method: "POST",
    body: JSON.stringify({ productId })
  });
}

export async function getProductChatRooms() {
  return request<ChatRoom[]>("/api/v1/chatrooms/products");
}

export async function getChatMessages(chatRoomId: number) {
  return request<ChatMessageHistory>(`/api/v1/chatrooms/${chatRoomId}/messages?size=20`);
}

export async function createCsRoom() {
  return request<ChatRoom>("/api/v1/chatrooms/cs", { method: "POST" });
}

export async function getCsRooms() {
  return request<ChatRoom[]>("/api/v1/chatrooms/cs");
}

export async function acceptCsRoom(chatRoomId: number) {
  return request<ChatRoom>(`/api/v1/chatrooms/cs/${chatRoomId}/accept`, { method: "POST" });
}

export async function completeCsRoom(chatRoomId: number) {
  return request<ChatRoom>(`/api/v1/chatrooms/cs/${chatRoomId}/complete`, { method: "POST" });
}

function getGuestId() {
  const key = "commerceGuestId";
  const saved = localStorage.getItem(key);
  if (saved) return saved;
  const next = crypto.randomUUID();
  localStorage.setItem(key, next);
  return next;
}

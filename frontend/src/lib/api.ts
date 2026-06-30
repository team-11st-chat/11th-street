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

export function setAccessToken(token: string | null) {
  accessToken = token;
}

export function getAccessToken() {
  return accessToken;
}

export function getApiBaseUrl() {
  return API_BASE_URL;
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
    const message = body?.message ?? `요청 실패 (${response.status})`;
    throw new Error(body?.code ? `${body.code}: ${message}` : message);
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

export async function getPopularKeywords() {
  return request<PopularKeyword[]>("/api/v1/popular-keywords");
}

export async function getTimeSales() {
  const data = await request<TimeSale[] | { content: TimeSale[] }>("/api/v1/timesales?page=0&size=20");
  return Array.isArray(data) ? data : data.content;
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

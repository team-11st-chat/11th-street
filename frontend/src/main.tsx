import React, { useEffect, useMemo, useRef, useState } from "react";
import { createRoot } from "react-dom/client";
import {
  Gift,
  Headphones,
  Home,
  LogIn,
  LogOut,
  MessageCircle,
  PackageSearch,
  Search,
  ShoppingCart,
  Sparkles,
  Timer,
  UserRound,
  Zap
} from "lucide-react";
import {
  acceptCsRoom,
  completeCsRoom,
  createCsRoom,
  createCouponPolicy,
  createProduct,
  createProductChatRoom,
  createTimeSale,
  getAccessToken,
  getApiBaseUrl,
  getChatMessages,
  getCouponPolicies,
  getCouponPolicy,
  getCsRooms,
  getPopularKeywords,
  getProduct,
  getProductChatRooms,
  getTimeSale,
  getTimeSales,
  issueCoupon,
  login,
  logout,
  orderTimeSale,
  registerMember,
  refreshToken,
  searchProducts,
  updateProduct,
  updateTimeSale
} from "./lib/api";
import type {
  ChatMessage,
  ChatRoom,
  CouponPolicy,
  DiscountType,
  PopularKeyword,
  Product,
  ProductDetail,
  ProductUpdatePayload,
  SaleStatus,
  TimeSale,
  TimeSaleUpdatePayload
} from "./lib/api";
import { categories } from "./lib/mock";
import "./styles.css";

type View = "home" | "search" | "timesale" | "coupon" | "support" | "backoffice" | "login";
type MemberRole = "BUYER" | "SELLER" | "CS_ADMIN" | "SUPER_ADMIN";
type Session = {
  memberId: number | null;
  role: MemberRole | null;
};

const money = new Intl.NumberFormat("ko-KR");
const kst = new Intl.DateTimeFormat("ko-KR", {
  timeZone: "Asia/Seoul",
  month: "2-digit",
  day: "2-digit",
  hour: "2-digit",
  minute: "2-digit"
});

function getCurrentSession(): Session {
  const token = getAccessToken();
  if (!token) return { memberId: null, role: null };

  try {
    const [, payload] = token.split(".");
    if (!payload) return { memberId: null, role: null };
    const normalizedPayload = payload.replace(/-/g, "+").replace(/_/g, "/");
    const decoded = JSON.parse(atob(normalizedPayload)) as { sub?: string; role?: MemberRole };
    return {
      memberId: decoded.sub ? Number(decoded.sub) : null,
      role: decoded.role ?? null
    };
  } catch {
    return { memberId: null, role: null };
  }
}

function toDatetimeLocal(date: Date) {
  const offsetMs = date.getTimezoneOffset() * 60000;
  return new Date(date.getTime() - offsetMs).toISOString().slice(0, 16);
}

function getRemainingTimePercent(startedAt: string, endedAt: string, nowMs: number) {
  const startMs = new Date(startedAt).getTime();
  const endMs = new Date(endedAt).getTime();
  const durationMs = endMs - startMs;

  if (!Number.isFinite(durationMs) || durationMs <= 0) return 0;
  if (nowMs < startMs) return 100;
  if (nowMs >= endMs) return 0;

  return Math.max(0, Math.min(100, ((endMs - nowMs) / durationMs) * 100));
}

function formatRemainingTime(endedAt: string, nowMs: number) {
  const remainingMs = new Date(endedAt).getTime() - nowMs;
  if (!Number.isFinite(remainingMs) || remainingMs <= 0) return "종료됨";

  const totalMinutes = Math.ceil(remainingMs / 60000);
  const days = Math.floor(totalMinutes / 1440);
  const hours = Math.floor((totalMinutes % 1440) / 60);
  const minutes = totalMinutes % 60;

  if (days > 0) return `${days}일 ${hours}시간 남음`;
  if (hours > 0) return `${hours}시간 ${minutes}분 남음`;
  return `${minutes}분 남음`;
}

type StompFrame = {
  command: string;
  headers: Record<string, string>;
  body: string;
};

type ChatConnectionStatus = "idle" | "connecting" | "connected" | "closed" | "error";

function getWebSocketUrl() {
  const baseUrl = new URL(getApiBaseUrl());
  baseUrl.protocol = baseUrl.protocol === "https:" ? "wss:" : "ws:";
  baseUrl.pathname = "/ws";
  baseUrl.search = "";
  baseUrl.hash = "";
  return baseUrl.toString();
}

function buildStompFrame(command: string, headers: Record<string, string> = {}, body = "") {
  const headerLines = Object.entries(headers).map(([key, value]) => `${key}:${value}`);
  return [command, ...headerLines, "", body].join("\n") + "\0";
}

function parseStompFrames(rawData: string) {
  return rawData
    .split("\0")
    .map((rawFrame) => rawFrame.trim())
    .filter(Boolean)
    .map((rawFrame) => {
      const [head, ...bodyParts] = rawFrame.split("\n\n");
      const [command, ...headerLines] = head.split("\n");
      const headers = Object.fromEntries(
        headerLines
          .map((line) => {
            const separatorIndex = line.indexOf(":");
            return separatorIndex === -1
              ? null
              : [line.slice(0, separatorIndex), line.slice(separatorIndex + 1)];
          })
          .filter((entry): entry is [string, string] => entry !== null)
      );

      return { command, headers, body: bodyParts.join("\n\n") } satisfies StompFrame;
    });
}

function sortMessagesByOldest(messages: ChatMessage[]) {
  return [...messages].sort((left, right) => {
    const sentAtDiff = new Date(left.sentAt).getTime() - new Date(right.sentAt).getTime();
    if (sentAtDiff !== 0) return sentAtDiff;
    return left.id - right.id;
  });
}

function appendUniqueMessage(messages: ChatMessage[], nextMessage: ChatMessage) {
  const exists = messages.some((message) => {
    if (nextMessage.id && message.id === nextMessage.id) return true;
    return message.clientMessageId === nextMessage.clientMessageId;
  });
  return exists ? sortMessagesByOldest(messages) : sortMessagesByOldest([...messages, nextMessage]);
}

function formatRoomSeller(room: ChatRoom) {
  if (!room.sellerId) return "판매자 -";
  return room.sellerName ? `판매자 ${room.sellerName}` : `판매자 #${room.sellerId}`;
}

function formatProductSeller(product: ProductDetail) {
  const sellerName = product.sellerName?.trim();
  return sellerName ? `${sellerName} #${product.sellerId}` : `#${product.sellerId}`;
}

function formatProductSellerTarget(product: ProductDetail) {
  const sellerName = product.sellerName?.trim();
  return sellerName ? `${sellerName} 판매자` : `판매자 #${product.sellerId}`;
}

function App() {
  const [view, setView] = useState<View>("home");
  const [keyword, setKeyword] = useState("");
  const [selectedCategory, setSelectedCategory] = useState<number | undefined>();
  const [products, setProducts] = useState<Product[]>([]);
  const [keywords, setKeywords] = useState<PopularKeyword[]>([]);
  const [timeSales, setTimeSales] = useState<TimeSale[]>([]);
  const [timeSaleProducts, setTimeSaleProducts] = useState<Record<number, ProductDetail>>({});
  const [selectedProduct, setSelectedProduct] = useState<ProductDetail | null>(null);
  const [session, setSession] = useState<Session>(() => getCurrentSession());
  const [status, setStatus] = useState("백엔드 연결 대기 중");
  const [notice, setNotice] = useState("Access Token은 메모리에만 보관합니다.");
  const commerceRequestSeq = useRef(0);
  const timeSaleRequestSeq = useRef(0);

  useEffect(() => {
    void loadCommerceData();
  }, []);

  useEffect(() => {
    void restoreSession();
  }, []);

  useEffect(() => {
    if (view === "timesale") {
      void loadTimeSaleData();
    }
  }, [view]);

  async function restoreSession() {
    if (getAccessToken()) {
      setSession(getCurrentSession());
      return;
    }

    try {
      await refreshToken();
      setSession(getCurrentSession());
      setNotice("Refresh Token으로 로그인 상태를 복구했습니다.");
    } catch {
      setSession({ memberId: null, role: null });
    }
  }

  async function loadCommerceData(nextKeyword = keyword, nextCategory?: number) {
    const requestSeq = commerceRequestSeq.current + 1;
    commerceRequestSeq.current = requestSeq;
    const categoryForRequest = arguments.length >= 2 ? nextCategory : selectedCategory;
    setStatus("DB 상품 조회 중...");

    const productPage = await searchProducts(nextKeyword, categoryForRequest)
      .then((value) => ({ status: "fulfilled" as const, value }))
      .catch((reason) => ({ status: "rejected" as const, reason }));

    const popular = await getPopularKeywords()
      .then((value) => ({ status: "fulfilled" as const, value }))
      .catch((reason) => ({ status: "rejected" as const, reason }));

    if (requestSeq !== commerceRequestSeq.current) {
      return;
    }

    const messages: string[] = [];

    if (productPage.status === "fulfilled") {
      setProducts(productPage.value.content);
    } else {
      setProducts([]);
      messages.push(`상품: ${productPage.reason.message}`);
    }

    if (popular.status === "fulfilled") {
      setKeywords(popular.value);
    } else {
      setKeywords([]);
      messages.push(`인기검색어: ${popular.reason.message}`);
    }

    setStatus(messages.length ? `백엔드 조회 실패 - ${messages.join(" / ")}` : `백엔드 연결됨: ${getApiBaseUrl()}`);
  }

  async function loadTimeSaleData() {
    const requestSeq = timeSaleRequestSeq.current + 1;
    timeSaleRequestSeq.current = requestSeq;
    setStatus("DB 타임세일 조회 중...");

    try {
      const sales = await getTimeSales();
      const detailResults = await Promise.allSettled(sales.map((sale) => getProduct(sale.productId)));

      if (requestSeq !== timeSaleRequestSeq.current) {
        return;
      }

      const detailMap = detailResults.reduce<Record<number, ProductDetail>>((acc, result) => {
        if (result.status === "fulfilled") {
          acc[result.value.id] = result.value;
        }
        return acc;
      }, {});

      setTimeSales(sales);
      setTimeSaleProducts(detailMap);
      setStatus(`백엔드 연결됨: ${getApiBaseUrl()} · 타임세일 ${sales.length}건 조회`);
    } catch (error) {
      if (requestSeq !== timeSaleRequestSeq.current) {
        return;
      }
      setTimeSales([]);
      setTimeSaleProducts({});
      setStatus(`백엔드 타임세일 조회 실패 - ${(error as Error).message}`);
    }
  }

  async function inspectProduct(productId: number) {
    try {
      const product = await getProduct(productId);
      setSelectedProduct(product);
      setNotice(`상품 상세 조회 완료: #${product.id}`);
    } catch (error) {
      setNotice((error as Error).message);
    }
  }

  async function openProductChat(productId: number) {
    try {
      const product = await getProduct(productId);
      const room = await createProductChatRoom(productId);
      setView("support");
      setNotice(`${formatProductSellerTarget(product)}에게 문의방 연결됨: #${room.id}`);
    } catch (error) {
      setNotice((error as Error).message);
    }
  }

  function submitSearch(event: React.FormEvent) {
    event.preventDefault();
    setView("search");
    void loadCommerceData(keyword, selectedCategory);
  }

  async function submitLogout() {
    try {
      await logout();
      setSession({ memberId: null, role: null });
      setSelectedProduct(null);
      setNotice("로그아웃되었습니다.");
      setView("home");
    } catch (error) {
      setSession({ memberId: null, role: null });
      setNotice((error as Error).message);
    }
  }

  const visibleProducts = useMemo(
    () => products.filter((product) => product.saleStatus !== "SUSPENDED"),
    [products]
  );
  const isAuthenticated = Boolean(session.role);

  return (
    <>
      <header className="topbar">
        <button className="brand" onClick={() => setView("home")}>11번길</button>
        <form className="searchBar" onSubmit={submitSearch}>
          <Search size={18} />
          <input
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            placeholder="상품 검색"
          />
          <button type="submit">검색</button>
        </form>
        <nav className="topnav">
          <button onClick={() => setView("timesale")}>타임세일</button>
          <button onClick={() => setView("coupon")}>쿠폰</button>
          <button onClick={() => setView("support")}>문의</button>
          <button onClick={() => setView("backoffice")}>백오피스</button>
          {isAuthenticated ? (
            <button onClick={() => void submitLogout()}>로그아웃</button>
          ) : (
            <button onClick={() => setView("login")}>로그인</button>
          )}
        </nav>
        <button className="iconButton" aria-label="장바구니">
          <ShoppingCart size={20} />
        </button>
      </header>

      <aside className="sidebar">
        <div className="profilePanel">
          <div className="avatar"><UserRound size={22} /></div>
          <div>
            <strong>한국 런칭 MVP</strong>
            <span>원화 표시, KST 시간</span>
          </div>
        </div>
        <button onClick={() => setView("home")} className={view === "home" ? "active" : ""}><Home size={18} /> 홈</button>
        <button onClick={() => setView("search")} className={view === "search" ? "active" : ""}><PackageSearch size={18} /> 상품 검색</button>
        <button onClick={() => setView("timesale")} className={view === "timesale" ? "active" : ""}><Zap size={18} /> 타임세일</button>
        <button onClick={() => setView("coupon")} className={view === "coupon" ? "active" : ""}><Gift size={18} /> 선착순 쿠폰</button>
        <button onClick={() => setView("support")} className={view === "support" ? "active" : ""}><MessageCircle size={18} /> 상품/CS 문의</button>
        <button onClick={() => setView("backoffice")} className={view === "backoffice" ? "active" : ""}><UserRound size={18} /> 백오피스</button>
        {isAuthenticated ? (
          <button onClick={() => void submitLogout()}><LogOut size={18} /> 로그아웃</button>
        ) : (
          <button onClick={() => setView("login")} className={view === "login" ? "active" : ""}><LogIn size={18} /> 로그인</button>
        )}
      </aside>

      <main className="shell">
        <div className="statusLine">{status}</div>
        <div className="noticeLine">{notice}</div>
        {selectedProduct && (
          <ProductDetailPanel
            product={selectedProduct}
            onClose={() => setSelectedProduct(null)}
            onChat={() => void openProductChat(selectedProduct.id)}
          />
        )}
        {view === "home" && (
          <HomeView
            products={visibleProducts}
            keywords={keywords}
            onMove={setView}
            onKeyword={(next) => {
              setKeyword(next);
              setView("search");
              void loadCommerceData(next, selectedCategory);
            }}
            onInspect={inspectProduct}
            onChat={openProductChat}
          />
        )}
        {view === "search" && (
          <SearchView
            keyword={keyword}
            products={visibleProducts}
            selectedCategory={selectedCategory}
            onCategory={(categoryId) => {
              setSelectedCategory(categoryId);
              void loadCommerceData(keyword, categoryId);
            }}
            onInspect={inspectProduct}
            onChat={openProductChat}
          />
        )}
        {view === "timesale" && (
          <TimeSaleView
            sales={timeSales}
            productDetails={timeSaleProducts}
            onRefresh={loadTimeSaleData}
            setNotice={setNotice}
          />
        )}
        {view === "coupon" && <CouponView setNotice={setNotice} />}
        {view === "support" && <SupportView setNotice={setNotice} />}
        {view === "backoffice" && <BackOfficeView session={session} setNotice={setNotice} />}
        {view === "login" && (
          <LoginView
            notice={notice}
            setNotice={setNotice}
            onAuthenticated={() => {
              setSession(getCurrentSession());
              setView("home");
            }}
          />
        )}
      </main>
    </>
  );
}

function HomeView({
  products,
  keywords,
  onMove,
  onKeyword,
  onInspect,
  onChat
}: {
  products: Product[];
  keywords: PopularKeyword[];
  onMove: (view: View) => void;
  onKeyword: (keyword: string) => void;
  onInspect: (productId: number) => void;
  onChat: (productId: number) => void;
}) {
  return (
    <div className="pageStack">
      <section className="hero">
        <div>
          <span className="eyebrow"><Sparkles size={16} /> 오늘 11시 선착순</span>
          <h1>검색, 타임세일, 쿠폰, 문의 API를 한 화면에서 확인</h1>
          <p>백엔드 API 명세와 정책 기준으로 상품 검색 v2 캐시, 상세 조회, 선착순 주문, 쿠폰 발급, 채팅방 REST 흐름을 연결했습니다.</p>
          <div className="heroActions">
            <button className="primary" onClick={() => onMove("timesale")}><Timer size={18} /> 타임세일 보기</button>
            <button className="secondary" onClick={() => onMove("support")}><MessageCircle size={18} /> 문의 확인</button>
          </div>
        </div>
        <div className="heroVisual">
          <div className="dealTicket">
            <span>LIVE</span>
            <strong>최대 80%</strong>
            <small>KST 기준 진행</small>
          </div>
        </div>
      </section>

      <section className="sectionHead">
        <div>
          <h2>인기 검색어</h2>
          <p>`GET /api/v1/popular-keywords`와 연결됩니다.</p>
        </div>
        <div className="keywordRail">
          {keywords.length === 0 && <span className="empty">DB에 집계된 인기 검색어가 없습니다.</span>}
          {keywords.map((item, index) => (
            <button key={item.keyword} onClick={() => onKeyword(item.keyword)}>
              {index + 1}. {item.keyword}
            </button>
          ))}
        </div>
      </section>

      <ProductGrid title="오늘의 추천 상품" products={products.slice(0, 4)} onInspect={onInspect} onChat={onChat} />
    </div>
  );
}

function SearchView({
  keyword,
  products,
  selectedCategory,
  onCategory,
  onInspect,
  onChat
}: {
  keyword: string;
  products: Product[];
  selectedCategory?: number;
  onCategory: (categoryId?: number) => void;
  onInspect: (productId: number) => void;
  onChat: (productId: number) => void;
}) {
  return (
    <div className="contentGrid">
      <aside className="filters">
        <h2>필터</h2>
        <button type="button" className={!selectedCategory ? "chip active" : "chip"} onClick={() => onCategory(undefined)}>전체</button>
        {categories.map((category) => (
          <button
            type="button"
            key={category.id}
            className={selectedCategory === category.id ? "chip active" : "chip"}
            onClick={() => onCategory(category.id)}
          >
            {category.name}
            <span>{category.parent}</span>
          </button>
        ))}
      </aside>
      <ProductGrid
        title={`"${keyword || "전체"}" 검색 결과`}
        products={products}
        note="검색은 /api/v2/products, 상세는 /api/v1/products/{id}로 조회합니다."
        onInspect={onInspect}
        onChat={onChat}
      />
    </div>
  );
}

function TimeSaleView({
  sales,
  productDetails,
  onRefresh,
  setNotice
}: {
  sales: TimeSale[];
  productDetails: Record<number, ProductDetail>;
  onRefresh: () => void;
  setNotice: (notice: string) => void;
}) {
  const [nowMs, setNowMs] = useState(() => Date.now());

  useEffect(() => {
    const timerId = window.setInterval(() => setNowMs(Date.now()), 1000);
    return () => window.clearInterval(timerId);
  }, []);

  async function buy(id: number) {
    try {
      const order = await orderTimeSale(id);
      setNotice(`주문 완료: 주문 #${order.id}, 상태 ${order.status}`);
    } catch (error) {
      setNotice((error as Error).message);
    }
  }

  return (
    <div className="pageStack">
      <section className="saleBanner">
        <span className="eyebrow"><Timer size={16} /> KST 기준 판매 중</span>
        <h1>DB 타임세일</h1>
        <p>`GET /api/v1/timesales` 조회 결과만 표시합니다.</p>
        <button className="secondary" type="button" onClick={() => onRefresh()}>목록 새로고침</button>
      </section>
      <div className="saleGrid">
        {sales.length === 0 && <p className="empty">DB에 조회 가능한 타임세일이 없습니다.</p>}
        {sales.map((sale) => {
          const product = productDetails[sale.productId];
          const canOrder = sale.status === "ONGOING" && sale.remainingQuantity > 0;
          const remainingPercent = getRemainingTimePercent(sale.startedAt, sale.endedAt, nowMs);
          const remainingTime = formatRemainingTime(sale.endedAt, nowMs);

          return (
            <article className="saleCard" key={sale.id}>
              <div className="imageBox"><Headphones size={56} /></div>
              <div className="saleBody">
                <span className="badge">{sale.status}</span>
                <h3>{product?.name ?? `상품 #${sale.productId}`}</h3>
                <div className="priceLine">
                  <strong>{money.format(Number(sale.salePrice))}원</strong>
                  <span>{money.format(Number(sale.originalPrice))}원</span>
                </div>
                <p>{kst.format(new Date(sale.startedAt))} - {kst.format(new Date(sale.endedAt))}</p>
                <p>타임세일 #{sale.id} · 남은 수량 {money.format(sale.remainingQuantity)}개</p>
                <div className="timeGaugeMeta">
                  <span>남은 시간</span>
                  <strong>{remainingTime}</strong>
                </div>
                <div className="timeGauge" aria-label={`남은 시간 ${remainingTime}`}>
                  <span style={{ width: `${remainingPercent}%` }} />
                </div>
                <button className="primary full" disabled={!canOrder} onClick={() => void buy(sale.id)}>
                  {canOrder ? "선착순 주문" : "주문 불가"}
                </button>
              </div>
            </article>
          );
        })}
      </div>
    </div>
  );
}

function CouponView({ setNotice }: { setNotice: (notice: string) => void }) {
  const [policyId, setPolicyId] = useState("");
  const [couponPolicies, setCouponPolicies] = useState<CouponPolicy[]>([]);
  const [issuedCoupon, setIssuedCoupon] = useState<{ id: number; issuedAt: string } | null>(null);
  const [issuedPolicy, setIssuedPolicy] = useState<CouponPolicy | null>(null);

  useEffect(() => {
    void loadCouponPolicies();
  }, []);

  async function loadCouponPolicies() {
    try {
      const policies = await getCouponPolicies();
      const issuablePolicies = policies.filter((policy) => policy.status === "ACTIVE" && policy.remainingQuantity > 0);
      setCouponPolicies(issuablePolicies);
      setPolicyId((current) => current || String(issuablePolicies[0]?.id ?? ""));
    } catch (error) {
      setCouponPolicies([]);
      setNotice((error as Error).message);
    }
  }

  async function claim() {
    try {
      const selectedPolicyId = Number(policyId);
      if (!selectedPolicyId) {
        setNotice("발급 가능한 쿠폰 정책이 없습니다.");
        return;
      }

      setIssuedCoupon(null);
      setIssuedPolicy(null);
      const coupon = await issueCoupon(selectedPolicyId);
      const policy = await getCouponPolicy(coupon.couponPolicyId);
      setIssuedCoupon({ id: coupon.id, issuedAt: coupon.issuedAt });
      setIssuedPolicy(policy);
      setCouponPolicies((current) => current.map((item) => item.id === policy.id ? policy : item));
      setNotice(`쿠폰 발급 완료: ${formatCouponBenefit(policy)} 쿠폰 #${coupon.id}`);
    } catch (error) {
      setNotice((error as Error).message);
    }
  }

  return (
    <section className="couponPanel">
      <span className="eyebrow"><Gift size={16} /> 1인 1장 선착순</span>
      <h1>오늘의 쿠폰</h1>
      <strong>{issuedPolicy ? formatCouponBenefit(issuedPolicy) : "혜택 확인"}</strong>
      <p>쿠폰 정책을 선택하고 발급합니다.</p>
      <label>
        쿠폰 정책
        <select value={policyId} onChange={(event) => setPolicyId(event.target.value)}>
          {couponPolicies.length === 0 && <option value="">발급 가능한 쿠폰 없음</option>}
          {couponPolicies.map((policy) => (
            <option key={policy.id} value={policy.id}>
              #{policy.id} {formatCouponPolicyName(policy)} - {formatCouponBenefit(policy)} - 잔여 {money.format(policy.remainingQuantity)}장
            </option>
          ))}
        </select>
      </label>
      <button className="primary full" disabled={!policyId} onClick={() => void claim()}>쿠폰 받기</button>
      {issuedPolicy && issuedCoupon && (
        <div className="issuedCouponResult" role="status" aria-live="polite">
          <span>{issuedPolicy.discountType === "PERCENTAGE" ? "정률 할인 쿠폰" : "정액 할인 쿠폰"}</span>
          <strong>{formatCouponBenefit(issuedPolicy)}</strong>
          <p>{formatCouponPolicyName(issuedPolicy)}</p>
          <dl>
            <div>
              <dt>발급 번호</dt>
              <dd>#{issuedCoupon.id}</dd>
            </div>
            <div>
              <dt>정책 ID</dt>
              <dd>#{issuedPolicy.id}</dd>
            </div>
            <div>
              <dt>잔여 수량</dt>
              <dd>{money.format(issuedPolicy.remainingQuantity)}장</dd>
            </div>
          </dl>
        </div>
      )}
    </section>
  );
}

function formatCouponBenefit(policy: CouponPolicy) {
  if (policy.discountType === "PERCENTAGE") {
    const limit = policy.maxDiscountAmount ? `, 최대 ${money.format(policy.maxDiscountAmount)}원` : "";
    return `${money.format(policy.discountValue)}% 할인${limit}`;
  }

  return `${money.format(policy.discountValue)}원 할인`;
}

function formatCouponPolicyName(policy: CouponPolicy) {
  return policy.name.includes("??") ? `쿠폰 정책 #${policy.id}` : policy.name;
}
function SupportView({ setNotice }: { setNotice: (notice: string) => void }) {
  const [rooms, setRooms] = useState<ChatRoom[]>([]);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [selectedRoom, setSelectedRoom] = useState<ChatRoom | null>(null);
  const [connectionStatus, setConnectionStatus] = useState<ChatConnectionStatus>("idle");
  const [messageText, setMessageText] = useState("");
  const wsRef = useRef<WebSocket | null>(null);
  const selectedRoomIdRef = useRef<number | null>(null);
  const messageListRef = useRef<HTMLDivElement | null>(null);

  async function loadRooms() {
    try {
      const [productRooms, csRooms] = await Promise.all([getProductChatRooms(), getCsRooms()]);
      setRooms([...productRooms, ...csRooms]);
      setNotice(`채팅방 ${productRooms.length + csRooms.length}개 조회 완료`);
    } catch (error) {
      setNotice((error as Error).message);
    }
  }

  async function startCsRoom() {
    try {
      const room = await createCsRoom();
      setNotice(`CS 문의방 연결됨: #${room.id}`);
      await loadRooms();
      await selectRoom(room);
    } catch (error) {
      setNotice((error as Error).message);
    }
  }

  async function loadMessages(roomId: number) {
    try {
      const history = await getChatMessages(roomId);
      setMessages(sortMessagesByOldest(history.content));
      setNotice(`메시지 ${history.content.length}건 조회 완료`);
    } catch (error) {
      setNotice((error as Error).message);
    }
  }

  async function selectRoom(room: ChatRoom) {
    selectedRoomIdRef.current = room.id;
    setSelectedRoom(room);
    setMessageText("");
    await loadMessages(room.id);
  }

  async function sendMessage(event: React.FormEvent) {
    event.preventDefault();
    const content = messageText.trim();
    if (!content || !selectedRoom) return;

    const socket = wsRef.current;
    if (!socket || socket.readyState !== WebSocket.OPEN || connectionStatus !== "connected") {
      setNotice("채팅 서버에 연결된 뒤 메시지를 보낼 수 있습니다.");
      return;
    }

    socket.send(buildStompFrame(
      "SEND",
      {
        destination: `/app/chatrooms/${selectedRoom.id}/messages`,
        "content-type": "application/json"
      },
      JSON.stringify({
        content,
        clientMessageId: crypto.randomUUID(),
        messageType: "TEXT"
      })
    ));
    setMessageText("");
  }

  useEffect(() => {
    void loadRooms();
  }, []);

  useEffect(() => {
    const messageList = messageListRef.current;
    if (!messageList) return;
    messageList.scrollTop = messageList.scrollHeight;
  }, [messages, selectedRoom?.id]);

  useEffect(() => {
    if (!selectedRoom) {
      setConnectionStatus("idle");
      return;
    }

    const token = getAccessToken();
    if (!token) {
      setConnectionStatus("error");
      setNotice("로그인 후 채팅 연결을 사용할 수 있습니다.");
      return;
    }

    const roomId = selectedRoom.id;
    selectedRoomIdRef.current = roomId;
    const socket = new WebSocket(getWebSocketUrl());
    wsRef.current = socket;
    setConnectionStatus("connecting");

    socket.addEventListener("open", () => {
      socket.send(buildStompFrame("CONNECT", {
        "accept-version": "1.2",
        "heart-beat": "10000,10000",
        Authorization: `Bearer ${token}`
      }));
    });

    socket.addEventListener("message", async (event) => {
      const rawData = typeof event.data === "string" ? event.data : await event.data.text();
      const frames = parseStompFrames(rawData);

      frames.forEach((frame) => {
        if (frame.command === "CONNECTED") {
          setConnectionStatus("connected");
          socket.send(buildStompFrame("SUBSCRIBE", {
            id: `chatroom-${roomId}`,
            destination: `/topic/chatrooms/${roomId}`
          }));
          setNotice(`채팅방 #${roomId} 실시간 연결됨`);
          return;
        }

        if (frame.command === "MESSAGE") {
          const nextMessage = JSON.parse(frame.body) as ChatMessage;
          if (nextMessage.chatRoomId === selectedRoomIdRef.current) {
            setMessages((current) => appendUniqueMessage(current, nextMessage));
          }
          return;
        }

        if (frame.command === "ERROR") {
          setConnectionStatus("error");
          setNotice(frame.body || "채팅 연결 오류가 발생했습니다.");
        }
      });
    });

    socket.addEventListener("close", () => {
      if (selectedRoomIdRef.current === roomId) {
        setConnectionStatus((current) => current === "error" ? current : "closed");
      }
    });

    socket.addEventListener("error", () => {
      if (selectedRoomIdRef.current === roomId) {
        setConnectionStatus("error");
        setNotice("채팅 서버 연결에 실패했습니다.");
      }
    });

    return () => {
      if (wsRef.current === socket) {
        wsRef.current = null;
      }
      socket.close();
    };
  }, [selectedRoom?.id]);

  return (
    <div className="supportGrid">
      <section className="supportPanel">
        <div className="sectionTitle">
          <h2>상품/CS 문의방</h2>
          <p>`GET /api/v1/chatrooms/products`, `/cs`</p>
        </div>
        <div className="heroActions">
          <button className="primary" onClick={() => void loadRooms()}>목록 새로고침</button>
          <button className="secondary" onClick={() => void startCsRoom()}>CS 문의 시작</button>
        </div>
        <div className="roomList">
          {rooms.length === 0 && <p className="empty">아직 조회된 문의방이 없습니다.</p>}
          {rooms.map((room) => (
            <button
              key={`${room.roomType}-${room.id}`}
              className={selectedRoom?.id === room.id ? "active" : ""}
              onClick={() => void selectRoom(room)}
            >
              <strong>#{room.id} {room.roomType}</strong>
              <span>{room.csStatus ?? "상품 문의"} · {formatRoomSeller(room)}</span>
            </button>
          ))}
        </div>
      </section>
      <section className="supportPanel">
        <div className="sectionTitle">
          <h2>{selectedRoom ? `채팅방 #${selectedRoom.id}` : "메시지 내역"}</h2>
          <p>
            {connectionStatus === "connected" && "STOMP 연결됨"}
            {connectionStatus === "connecting" && "STOMP 연결 중"}
            {connectionStatus === "closed" && "STOMP 연결 종료"}
            {connectionStatus === "error" && "STOMP 연결 필요"}
            {connectionStatus === "idle" && "방을 선택하면 실시간 연결됩니다."}
          </p>
        </div>
        <div className="messageList" ref={messageListRef}>
          {messages.length === 0 && <p className="empty">채팅방을 선택하면 메시지 내역이 표시됩니다.</p>}
          {messages.map((message) => (
            <article key={message.id || message.clientMessageId} className={message.senderId ? "" : "system"}>
              <strong>{message.messageType}{message.senderId ? ` · 회원 #${message.senderId}` : ""}</strong>
              <p>{message.content}</p>
              {message.product && <span>{message.product.name} · {money.format(message.product.price)}원</span>}
              <span>{kst.format(new Date(message.sentAt))}</span>
            </article>
          ))}
        </div>
        <form className="chatComposer" onSubmit={sendMessage}>
          <textarea
            value={messageText}
            onChange={(event) => setMessageText(event.target.value)}
            placeholder={selectedRoom ? "메시지를 입력하세요" : "먼저 문의방을 선택하세요"}
            maxLength={1000}
            disabled={!selectedRoom || connectionStatus !== "connected"}
          />
          <button className="primary" type="submit" disabled={!messageText.trim() || connectionStatus !== "connected"}>
            전송
          </button>
        </form>
      </section>
    </div>
  );
}

function BackOfficeView({ session, setNotice }: { session: Session; setNotice: (notice: string) => void }) {
  const isSeller = session.role === "SELLER";
  const isSuperAdmin = session.role === "SUPER_ADMIN";
  const isCsAdmin = session.role === "CS_ADMIN";
  const canManageCs = isCsAdmin || isSuperAdmin;

  if (!getAccessToken()) {
    return (
      <section className="authPanel">
        <span className="eyebrow"><UserRound size={16} /> 백오피스</span>
        <h1>로그인이 필요합니다</h1>
        <p>SELLER, CS_ADMIN 또는 SUPER_ADMIN 권한의 계정으로 로그인하면 운영 기능을 사용할 수 있습니다.</p>
      </section>
    );
  }

  if (!isSeller && !canManageCs) {
    return (
      <section className="authPanel">
        <span className="eyebrow"><UserRound size={16} /> 백오피스</span>
        <h1>권한이 없습니다</h1>
        <p>현재 역할은 {session.role ?? "UNKNOWN"}입니다. 상품/타임세일 운영은 SELLER, 쿠폰 운영은 SUPER_ADMIN, CS 운영은 CS_ADMIN 또는 SUPER_ADMIN만 사용할 수 있습니다.</p>
      </section>
    );
  }

  return (
    <div className="pageStack">
      <section className="backofficeHeader">
        <div>
          <span className="eyebrow"><UserRound size={16} /> 11번길 운영 콘솔</span>
          <h1>커머스 백오피스</h1>
          <p>현재 로그인: 회원 #{session.memberId ?? "-"} · {session.role}</p>
        </div>
      </section>

      {isSeller && (
        <div className="backofficeGrid">
          <ProductCreatePanel setNotice={setNotice} />
          <ProductUpdatePanel setNotice={setNotice} />
          <TimeSaleCreatePanel setNotice={setNotice} />
          <TimeSaleUpdatePanel setNotice={setNotice} />
          <BackOfficeChatPanel
            title="내 상품 문의"
            description="판매자로 참여 중인 상품 문의방을 조회하고 응답합니다."
            loadRooms={getProductChatRooms}
            emptyText="아직 내 상품으로 들어온 문의가 없습니다."
            setNotice={setNotice}
          />
        </div>
      )}

      {canManageCs && (
        <>
          {isSuperAdmin && (
            <div className="backofficeGrid">
              <CouponPolicyCreatePanel setNotice={setNotice} />
            </div>
          )}
          <BackOfficeChatPanel
            title="CS 문의"
            description="대기 중인 CS 문의를 접수하고 STOMP 채팅으로 응답합니다."
            loadRooms={getCsRooms}
            emptyText="현재 접수 가능한 CS 문의가 없습니다."
            setNotice={setNotice}
            csMode
          />
        </>
      )}
    </div>
  );
}

function ProductCreatePanel({ setNotice }: { setNotice: (notice: string) => void }) {
  const [name, setName] = useState("11번길 신규 상품");
  const [categoryId, setCategoryId] = useState(String(categories[0]?.id ?? 11));
  const [price, setPrice] = useState("10000");
  const [stockQuantity, setStockQuantity] = useState("100");
  const [createdProduct, setCreatedProduct] = useState<ProductDetail | null>(null);

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    try {
      const product = await createProduct({
        name: name.trim(),
        categoryId: Number(categoryId),
        price: Number(price),
        stockQuantity: Number(stockQuantity)
      });
      setCreatedProduct(product);
      setNotice(`상품 등록 완료: #${product.id} ${product.name}`);
    } catch (error) {
      setNotice((error as Error).message);
    }
  }

  return (
    <section className="backofficePanel">
      <div className="sectionTitle">
        <div>
          <h2>상품 등록</h2>
          <p>SELLER 권한으로 `/api/v1/products`에 등록합니다.</p>
        </div>
      </div>
      <form className="officeForm" onSubmit={submit}>
        <label>상품명<input value={name} onChange={(event) => setName(event.target.value)} maxLength={100} required /></label>
        <label>
          카테고리
          <select value={categoryId} onChange={(event) => setCategoryId(event.target.value)}>
            {categories.map((category) => (
              <option key={category.id} value={category.id}>{category.parent} · {category.name}</option>
            ))}
          </select>
        </label>
        <label>가격<input value={price} onChange={(event) => setPrice(event.target.value)} inputMode="numeric" required /></label>
        <label>재고<input value={stockQuantity} onChange={(event) => setStockQuantity(event.target.value)} inputMode="numeric" required /></label>
        <button className="primary full" type="submit"><PackageSearch size={18} /> 상품 등록</button>
      </form>
      {createdProduct && (
        <div className="officeResult">
          <strong>#{createdProduct.id} {createdProduct.name}</strong>
          <span>카테고리 #{createdProduct.categoryId} · 재고 {money.format(createdProduct.stockQuantity)}개 · {money.format(Number(createdProduct.price))}원</span>
        </div>
      )}
    </section>
  );
}

function ProductUpdatePanel({ setNotice }: { setNotice: (notice: string) => void }) {
  const [productId, setProductId] = useState("");
  const [name, setName] = useState("");
  const [categoryId, setCategoryId] = useState(String(categories[0]?.id ?? 11));
  const [price, setPrice] = useState("");
  const [stockQuantity, setStockQuantity] = useState("");
  const [saleStatus, setSaleStatus] = useState<SaleStatus>("ON_SALE");
  const [loadedProduct, setLoadedProduct] = useState<ProductDetail | null>(null);
  const [updatedProduct, setUpdatedProduct] = useState<ProductDetail | null>(null);

  async function loadProduct(event: React.FormEvent) {
    event.preventDefault();
    try {
      const product = await getProduct(Number(productId));
      setLoadedProduct(product);
      setUpdatedProduct(null);
      setName(product.name);
      setCategoryId(String(product.categoryId));
      setPrice(String(Number(product.price)));
      setStockQuantity(String(product.stockQuantity));
      setSaleStatus(product.saleStatus);
      setNotice(`상품 #${product.id} 조회 완료: ${product.name}`);
    } catch (error) {
      setLoadedProduct(null);
      setUpdatedProduct(null);
      setNotice((error as Error).message);
    }
  }

  function validateUpdate() {
    if (!loadedProduct) return "먼저 상품을 조회하세요.";
    const nextName = name.trim();
    const nextPrice = Number(price);
    const nextStockQuantity = Number(stockQuantity);
    if (!nextName || nextName.length > 100) return "상품명은 1자 이상 100자 이하로 입력하세요.";
    if (!Number.isFinite(nextPrice) || nextPrice <= 0) return "상품 가격은 0보다 커야 합니다.";
    if (!Number.isInteger(nextStockQuantity) || nextStockQuantity < 0) return "재고 수량은 0 이상 정수여야 합니다.";
    if (saleStatus === "ON_SALE" && nextStockQuantity === 0) return "재고가 0개인 상품은 판매중으로 변경할 수 없습니다.";
    return null;
  }

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    const validationMessage = validateUpdate();
    if (validationMessage) {
      setNotice(validationMessage);
      return;
    }

    if (!loadedProduct) return;

    const payload: ProductUpdatePayload = {
      name: name.trim(),
      categoryId: Number(categoryId),
      price: Number(price),
      stockQuantity: Number(stockQuantity),
      saleStatus
    };

    try {
      const product = await updateProduct(loadedProduct.id, payload);
      setLoadedProduct(product);
      setUpdatedProduct(product);
      setName(product.name);
      setCategoryId(String(product.categoryId));
      setPrice(String(Number(product.price)));
      setStockQuantity(String(product.stockQuantity));
      setSaleStatus(product.saleStatus);
      setNotice(`상품 #${product.id} 수정 완료: ${product.saleStatus}`);
    } catch (error) {
      setNotice((error as Error).message);
    }
  }

  return (
    <section className="backofficePanel">
      <div className="sectionTitle">
        <div>
          <h2>상품 수정</h2>
          <p>SELLER 권한으로 상품 정보, 재고, 판매 상태를 수정합니다.</p>
        </div>
      </div>
      <form className="officeForm" onSubmit={loadProduct}>
        <label>상품 ID<input value={productId} onChange={(event) => setProductId(event.target.value)} inputMode="numeric" required /></label>
        <button className="secondary full" type="submit">상품 불러오기</button>
      </form>
      {loadedProduct && (
        <form className="officeForm" onSubmit={submit}>
          <label>상품명<input value={name} onChange={(event) => setName(event.target.value)} maxLength={100} required /></label>
          <label>
            카테고리
            <select value={categoryId} onChange={(event) => setCategoryId(event.target.value)}>
              {categories.map((category) => (
                <option key={category.id} value={category.id}>{category.parent} · {category.name}</option>
              ))}
            </select>
          </label>
          <label>가격<input value={price} onChange={(event) => setPrice(event.target.value)} inputMode="numeric" required /></label>
          <label>재고<input value={stockQuantity} onChange={(event) => setStockQuantity(event.target.value)} inputMode="numeric" required /></label>
          <label>
            판매 상태
            <select value={saleStatus} onChange={(event) => setSaleStatus(event.target.value as SaleStatus)}>
              <option value="ON_SALE">판매중</option>
              <option value="SOLD_OUT">품절</option>
              <option value="SUSPENDED">판매중지</option>
            </select>
          </label>
          <button className="primary full" type="submit"><PackageSearch size={18} /> 상품 수정</button>
        </form>
      )}
      {updatedProduct && (
        <div className="officeResult">
          <strong>#{updatedProduct.id} {updatedProduct.name}</strong>
          <span>카테고리 #{updatedProduct.categoryId} · 재고 {money.format(updatedProduct.stockQuantity)}개 · {updatedProduct.saleStatus}</span>
        </div>
      )}
    </section>
  );
}

function TimeSaleCreatePanel({ setNotice }: { setNotice: (notice: string) => void }) {
  const now = useMemo(() => new Date(), []);
  const defaultEnd = useMemo(() => new Date(now.getTime() + 30 * 24 * 60 * 60 * 1000), [now]);
  const [productId, setProductId] = useState("");
  const [salePrice, setSalePrice] = useState("9000");
  const [startedAt, setStartedAt] = useState(toDatetimeLocal(now));
  const [endedAt, setEndedAt] = useState(toDatetimeLocal(defaultEnd));
  const [initialQuantity, setInitialQuantity] = useState("100");
  const [createdSale, setCreatedSale] = useState<TimeSale | null>(null);

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    try {
      const sale = await createTimeSale({
        productId: Number(productId),
        salePrice: Number(salePrice),
        startedAt,
        endedAt,
        initialQuantity: Number(initialQuantity)
      });
      setCreatedSale(sale);
      setNotice(`타임세일 생성 완료: #${sale.id}, 상품 #${sale.productId}`);
    } catch (error) {
      setNotice((error as Error).message);
    }
  }

  return (
    <section className="backofficePanel">
      <div className="sectionTitle">
        <div>
          <h2>타임세일 생성</h2>
          <p>SELLER 권한으로 본인 상품에 타임세일을 생성합니다.</p>
        </div>
      </div>
      <form className="officeForm" onSubmit={submit}>
        <label>상품 ID<input value={productId} onChange={(event) => setProductId(event.target.value)} inputMode="numeric" required /></label>
        <label>타임세일가<input value={salePrice} onChange={(event) => setSalePrice(event.target.value)} inputMode="numeric" required /></label>
        <label>시작 시각<input type="datetime-local" value={startedAt} onChange={(event) => setStartedAt(event.target.value)} required /></label>
        <label>종료 시각<input type="datetime-local" value={endedAt} onChange={(event) => setEndedAt(event.target.value)} required /></label>
        <label>선착순 수량<input value={initialQuantity} onChange={(event) => setInitialQuantity(event.target.value)} inputMode="numeric" required /></label>
        <button className="primary full" type="submit"><Timer size={18} /> 타임세일 생성</button>
      </form>
      {createdSale && (
        <div className="officeResult">
          <strong>타임세일 #{createdSale.id}</strong>
          <span>상품 #{createdSale.productId} · {money.format(Number(createdSale.salePrice))}원 · {createdSale.status}</span>
        </div>
      )}
    </section>
  );
}

function TimeSaleUpdatePanel({ setNotice }: { setNotice: (notice: string) => void }) {
  const [timeSaleId, setTimeSaleId] = useState("");
  const [salePrice, setSalePrice] = useState("");
  const [startedAt, setStartedAt] = useState("");
  const [endedAt, setEndedAt] = useState("");
  const [initialQuantity, setInitialQuantity] = useState("");
  const [loadedSale, setLoadedSale] = useState<TimeSale | null>(null);
  const [updatedSale, setUpdatedSale] = useState<TimeSale | null>(null);

  const isScheduled = loadedSale?.status === "SCHEDULED";
  const isOngoing = loadedSale?.status === "ONGOING";
  const isEnded = loadedSale?.status === "ENDED";
  const canSubmit = Boolean(loadedSale && !isEnded);

  async function loadSale(event: React.FormEvent) {
    event.preventDefault();
    try {
      const sale = await getTimeSale(Number(timeSaleId));
      setLoadedSale(sale);
      setUpdatedSale(null);
      setSalePrice(String(Number(sale.salePrice)));
      setStartedAt(toDatetimeLocal(new Date(sale.startedAt)));
      setEndedAt(toDatetimeLocal(new Date(sale.endedAt)));
      setInitialQuantity(String(sale.remainingQuantity));
      setNotice(`타임세일 #${sale.id} 조회 완료: ${sale.status}`);
    } catch (error) {
      setLoadedSale(null);
      setUpdatedSale(null);
      setNotice((error as Error).message);
    }
  }

  function validateUpdate() {
    if (!loadedSale) return "먼저 타임세일을 조회하세요.";
    if (isEnded) return "종료된 타임세일은 수정할 수 없습니다.";

    const nextStartedAt = new Date(startedAt).getTime();
    const nextEndedAt = new Date(endedAt).getTime();
    if (!Number.isFinite(nextEndedAt)) return "종료 시각을 입력하세요.";

    if (isOngoing) {
      if (nextEndedAt <= new Date(loadedSale.endedAt).getTime()) {
        return "진행 중인 타임세일은 기존 종료 시각보다 뒤로만 연장할 수 있습니다.";
      }
      return null;
    }

    const nextSalePrice = Number(salePrice);
    const nextInitialQuantity = Number(initialQuantity);
    const originalPrice = Number(loadedSale.originalPrice);
    if (!Number.isFinite(nextSalePrice) || nextSalePrice < 100) return "특가는 100원 이상이어야 합니다.";
    if (nextSalePrice >= originalPrice) return "특가는 정상가보다 낮아야 합니다.";
    const discountRate = Math.floor(((originalPrice - nextSalePrice) / originalPrice) * 100);
    if (discountRate < 5 || discountRate >= 100) return "할인율은 정상가 대비 최소 5% 이상, 100% 미만이어야 합니다.";
    if (!Number.isFinite(nextStartedAt) || nextEndedAt <= nextStartedAt) return "종료 시각은 시작 시각보다 이후여야 합니다.";
    if (!Number.isInteger(nextInitialQuantity) || nextInitialQuantity < 1) return "한정 판매 수량은 1개 이상 정수여야 합니다.";
    return null;
  }

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    const validationMessage = validateUpdate();
    if (validationMessage) {
      setNotice(validationMessage);
      return;
    }

    if (!loadedSale) return;

    const payload: TimeSaleUpdatePayload = isOngoing
      ? { endedAt }
      : {
          salePrice: Number(salePrice),
          startedAt,
          endedAt,
          initialQuantity: Number(initialQuantity)
        };

    try {
      const sale = await updateTimeSale(loadedSale.id, payload);
      setLoadedSale(sale);
      setUpdatedSale(sale);
      setSalePrice(String(Number(sale.salePrice)));
      setStartedAt(toDatetimeLocal(new Date(sale.startedAt)));
      setEndedAt(toDatetimeLocal(new Date(sale.endedAt)));
      setInitialQuantity(String(sale.remainingQuantity));
      setNotice(`타임세일 #${sale.id} 수정 완료: ${sale.status}`);
    } catch (error) {
      setNotice((error as Error).message);
    }
  }

  return (
    <section className="backofficePanel">
      <div className="sectionTitle">
        <div>
          <h2>타임세일 수정</h2>
          <p>예정 상태는 전체 수정, 진행 중에는 종료 시각 연장만 가능합니다.</p>
        </div>
      </div>
      <form className="officeForm" onSubmit={loadSale}>
        <label>타임세일 ID<input value={timeSaleId} onChange={(event) => setTimeSaleId(event.target.value)} inputMode="numeric" required /></label>
        <button className="secondary full" type="submit">타임세일 불러오기</button>
      </form>
      {loadedSale && (
        <form className="officeForm" onSubmit={submit}>
          <label>특가<input value={salePrice} onChange={(event) => setSalePrice(event.target.value)} inputMode="numeric" disabled={!isScheduled} required /></label>
          <label>시작 시각<input type="datetime-local" value={startedAt} onChange={(event) => setStartedAt(event.target.value)} disabled={!isScheduled} required /></label>
          <label>종료 시각<input type="datetime-local" value={endedAt} onChange={(event) => setEndedAt(event.target.value)} disabled={isEnded} required /></label>
          <label>한정 수량<input value={initialQuantity} onChange={(event) => setInitialQuantity(event.target.value)} inputMode="numeric" disabled={!isScheduled} required /></label>
          <button className="primary full" type="submit" disabled={!canSubmit}><Timer size={18} /> 타임세일 수정</button>
        </form>
      )}
      {loadedSale && (
        <div className="officeResult">
          <strong>타임세일 #{loadedSale.id} · {loadedSale.status}</strong>
          <span>상품 #{loadedSale.productId} · 정상가 {money.format(Number(loadedSale.originalPrice))}원 · 특가 {money.format(Number(loadedSale.salePrice))}원</span>
        </div>
      )}
      {updatedSale && (
        <div className="officeResult">
          <strong>수정 반영 완료</strong>
          <span>{kst.format(new Date(updatedSale.startedAt))} - {kst.format(new Date(updatedSale.endedAt))} · 잔여 {money.format(updatedSale.remainingQuantity)}개</span>
        </div>
      )}
    </section>
  );
}

function CouponPolicyCreatePanel({ setNotice }: { setNotice: (notice: string) => void }) {
  const now = useMemo(() => new Date(), []);
  const defaultEnd = useMemo(() => new Date(now.getTime() + 30 * 24 * 60 * 60 * 1000), [now]);
  const [name, setName] = useState("11번길 선착순 쿠폰");
  const [discountType, setDiscountType] = useState<DiscountType>("PERCENTAGE");
  const [discountValue, setDiscountValue] = useState("10");
  const [maxDiscountAmount, setMaxDiscountAmount] = useState("5000");
  const [issueStartsAt, setIssueStartsAt] = useState(toDatetimeLocal(now));
  const [issueEndsAt, setIssueEndsAt] = useState(toDatetimeLocal(defaultEnd));
  const [totalQuantity, setTotalQuantity] = useState("1000");
  const [createdPolicy, setCreatedPolicy] = useState<CouponPolicy | null>(null);

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    try {
      const policy = await createCouponPolicy({
        name: name.trim(),
        discountType,
        discountValue: Number(discountValue),
        maxDiscountAmount: discountType === "PERCENTAGE" && maxDiscountAmount ? Number(maxDiscountAmount) : null,
        issueStartsAt,
        issueEndsAt,
        totalQuantity: Number(totalQuantity)
      });
      setCreatedPolicy(policy);
      setNotice(`선착순 쿠폰 생성 완료: #${policy.id} ${formatCouponBenefit(policy)}`);
    } catch (error) {
      setNotice((error as Error).message);
    }
  }

  return (
    <section className="backofficePanel">
      <div className="sectionTitle">
        <div>
          <h2>선착순 쿠폰 생성</h2>
          <p>SUPER_ADMIN 권한으로 쿠폰 정책을 만들고 발급 가능 수량을 설정합니다.</p>
        </div>
      </div>
      <form className="officeForm" onSubmit={submit}>
        <label>쿠폰명<input value={name} onChange={(event) => setName(event.target.value)} maxLength={100} required /></label>
        <label>
          할인 방식
          <select value={discountType} onChange={(event) => setDiscountType(event.target.value as DiscountType)}>
            <option value="PERCENTAGE">정률 할인</option>
            <option value="FIXED_AMOUNT">정액 할인</option>
          </select>
        </label>
        <label>할인 값<input value={discountValue} onChange={(event) => setDiscountValue(event.target.value)} inputMode="numeric" required /></label>
        <label>최대 할인액<input value={maxDiscountAmount} onChange={(event) => setMaxDiscountAmount(event.target.value)} inputMode="numeric" disabled={discountType === "FIXED_AMOUNT"} /></label>
        <label>발급 시작<input type="datetime-local" value={issueStartsAt} onChange={(event) => setIssueStartsAt(event.target.value)} required /></label>
        <label>발급 종료<input type="datetime-local" value={issueEndsAt} onChange={(event) => setIssueEndsAt(event.target.value)} required /></label>
        <label>선착순 수량<input value={totalQuantity} onChange={(event) => setTotalQuantity(event.target.value)} inputMode="numeric" required /></label>
        <button className="primary full" type="submit"><Gift size={18} /> 쿠폰 생성</button>
      </form>
      {createdPolicy && (
        <div className="officeResult">
          <strong>쿠폰 정책 #{createdPolicy.id}</strong>
          <span>{formatCouponBenefit(createdPolicy)} · 잔여 {money.format(createdPolicy.remainingQuantity)}장 · {createdPolicy.status}</span>
        </div>
      )}
    </section>
  );
}

function BackOfficeChatPanel({
  title,
  description,
  loadRooms,
  emptyText,
  setNotice,
  csMode = false
}: {
  title: string;
  description: string;
  loadRooms: () => Promise<ChatRoom[]>;
  emptyText: string;
  setNotice: (notice: string) => void;
  csMode?: boolean;
}) {
  const [rooms, setRooms] = useState<ChatRoom[]>([]);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [selectedRoom, setSelectedRoom] = useState<ChatRoom | null>(null);
  const [connectionStatus, setConnectionStatus] = useState<ChatConnectionStatus>("idle");
  const [messageText, setMessageText] = useState("");
  const wsRef = useRef<WebSocket | null>(null);
  const selectedRoomIdRef = useRef<number | null>(null);
  const messageListRef = useRef<HTMLDivElement | null>(null);

  async function refreshRooms() {
    try {
      const nextRooms = await loadRooms();
      setRooms(nextRooms);
      setNotice(`${title} ${nextRooms.length}건 조회 완료`);
    } catch (error) {
      setNotice((error as Error).message);
    }
  }

  async function loadMessages(roomId: number) {
    try {
      const history = await getChatMessages(roomId);
      setMessages(sortMessagesByOldest(history.content));
      setNotice(`채팅방 #${roomId} 메시지 ${history.content.length}건 조회 완료`);
    } catch (error) {
      setNotice((error as Error).message);
    }
  }

  async function selectRoom(room: ChatRoom) {
    selectedRoomIdRef.current = room.id;
    setSelectedRoom(room);
    setMessageText("");
    await loadMessages(room.id);
  }

  async function acceptRoom(room: ChatRoom) {
    try {
      const acceptedRoom = await acceptCsRoom(room.id);
      setRooms((current) => current.map((item) => item.id === acceptedRoom.id ? acceptedRoom : item));
      setNotice(`CS 문의 #${acceptedRoom.id} 접수 완료`);
      await selectRoom(acceptedRoom);
    } catch (error) {
      setNotice((error as Error).message);
    }
  }

  async function completeRoom() {
    if (!selectedRoom) return;
    try {
      const completedRoom = await completeCsRoom(selectedRoom.id);
      setRooms((current) => current.map((item) => item.id === completedRoom.id ? completedRoom : item));
      setSelectedRoom(completedRoom);
      setNotice(`CS 문의 #${completedRoom.id} 완료 처리`);
    } catch (error) {
      setNotice((error as Error).message);
    }
  }

  async function sendMessage(event: React.FormEvent) {
    event.preventDefault();
    const content = messageText.trim();
    if (!content || !selectedRoom) return;

    const socket = wsRef.current;
    if (!socket || socket.readyState !== WebSocket.OPEN || connectionStatus !== "connected") {
      setNotice("채팅 서버에 연결된 뒤 메시지를 보낼 수 있습니다.");
      return;
    }

    socket.send(buildStompFrame(
      "SEND",
      {
        destination: `/app/chatrooms/${selectedRoom.id}/messages`,
        "content-type": "application/json"
      },
      JSON.stringify({
        content,
        clientMessageId: crypto.randomUUID(),
        messageType: "TEXT"
      })
    ));
    setMessageText("");
  }

  useEffect(() => {
    void refreshRooms();
  }, []);

  useEffect(() => {
    const messageList = messageListRef.current;
    if (!messageList) return;
    messageList.scrollTop = messageList.scrollHeight;
  }, [messages, selectedRoom?.id]);

  useEffect(() => {
    if (!selectedRoom) {
      setConnectionStatus("idle");
      return;
    }

    const token = getAccessToken();
    if (!token) {
      setConnectionStatus("error");
      setNotice("로그인해야 채팅 서버에 연결할 수 있습니다.");
      return;
    }

    const roomId = selectedRoom.id;
    selectedRoomIdRef.current = roomId;
    const socket = new WebSocket(getWebSocketUrl());
    wsRef.current = socket;
    setConnectionStatus("connecting");

    socket.addEventListener("open", () => {
      socket.send(buildStompFrame("CONNECT", {
        "accept-version": "1.2",
        "heart-beat": "10000,10000",
        Authorization: `Bearer ${token}`
      }));
    });

    socket.addEventListener("message", async (event) => {
      const rawData = typeof event.data === "string" ? event.data : await event.data.text();
      const frames = parseStompFrames(rawData);

      frames.forEach((frame) => {
        if (frame.command === "CONNECTED") {
          setConnectionStatus("connected");
          socket.send(buildStompFrame("SUBSCRIBE", {
            id: `backoffice-chatroom-${roomId}`,
            destination: `/topic/chatrooms/${roomId}`
          }));
          setNotice(`채팅방 #${roomId} 실시간 연결됨`);
          return;
        }

        if (frame.command === "MESSAGE") {
          const nextMessage = JSON.parse(frame.body) as ChatMessage;
          if (nextMessage.chatRoomId === selectedRoomIdRef.current) {
            setMessages((current) => appendUniqueMessage(current, nextMessage));
          }
          return;
        }

        if (frame.command === "ERROR") {
          setConnectionStatus("error");
          setNotice(frame.body || "채팅 연결 오류가 발생했습니다.");
        }
      });
    });

    socket.addEventListener("close", () => {
      if (selectedRoomIdRef.current === roomId) {
        setConnectionStatus((current) => current === "error" ? current : "closed");
      }
    });

    socket.addEventListener("error", () => {
      if (selectedRoomIdRef.current === roomId) {
        setConnectionStatus("error");
        setNotice("채팅 서버 연결에 실패했습니다.");
      }
    });

    return () => {
      if (wsRef.current === socket) {
        wsRef.current = null;
      }
      socket.close();
    };
  }, [selectedRoom?.id]);

  const canSend = Boolean(selectedRoom && selectedRoom.csStatus !== "COMPLETED" && connectionStatus === "connected");

  return (
    <section className="backofficePanel widePanel">
      <div className="sectionTitle">
        <div>
          <h2>{title}</h2>
          <p>{description}</p>
        </div>
        <button className="secondary" type="button" onClick={() => void refreshRooms()}>새로고침</button>
      </div>
      <div className="supportGrid compact">
        <div>
          <div className="roomList">
            {rooms.length === 0 && <p className="empty">{emptyText}</p>}
            {rooms.map((room) => (
              <button
                key={`${room.roomType}-${room.id}`}
                className={selectedRoom?.id === room.id ? "active" : ""}
                onClick={() => void (csMode && room.csStatus === "WAITING" ? acceptRoom(room) : selectRoom(room))}
              >
                <strong>#{room.id} {room.roomType}</strong>
                <span>{room.csStatus ?? "상품 문의"} · {formatRoomSeller(room)}</span>
                {csMode && room.csStatus === "WAITING" && (
                  <span className="inlineAction">접수하기</span>
                )}
              </button>
            ))}
          </div>
        </div>
        <div>
          <div className="sectionTitle chatTitle">
            <div>
              <h2>{selectedRoom ? `채팅방 #${selectedRoom.id}` : "메시지"}</h2>
              <p>{connectionStatus}</p>
            </div>
            {csMode && selectedRoom && selectedRoom.csStatus !== "COMPLETED" && (
              <button className="secondary" type="button" onClick={() => void completeRoom()}>완료 처리</button>
            )}
          </div>
          <div className="messageList" ref={messageListRef}>
            {messages.length === 0 && <p className="empty">채팅방을 선택하면 오래된 메시지부터 표시합니다.</p>}
            {messages.map((message) => (
              <article key={message.id || message.clientMessageId} className={message.senderId ? "" : "system"}>
                <strong>{message.messageType}{message.senderId ? ` · 회원 #${message.senderId}` : ""}</strong>
                <p>{message.content}</p>
                {message.product && <span>{message.product.name} · {money.format(message.product.price)}원</span>}
                <span>{kst.format(new Date(message.sentAt))}</span>
              </article>
            ))}
          </div>
          <form className="chatComposer" onSubmit={sendMessage}>
            <textarea
              value={messageText}
              onChange={(event) => setMessageText(event.target.value)}
              placeholder={selectedRoom ? "운영자 답변을 입력하세요" : "먼저 채팅방을 선택하세요"}
              maxLength={1000}
              disabled={!canSend}
            />
            <button className="primary" type="submit" disabled={!messageText.trim() || !canSend}>전송</button>
          </form>
        </div>
      </div>
    </section>
  );
}

function LoginView({
  notice,
  setNotice,
  onAuthenticated
}: {
  notice: string;
  setNotice: (notice: string) => void;
  onAuthenticated: () => void;
}) {
  const [mode, setMode] = useState<"login" | "join">("login");
  const [name, setName] = useState("김구매");
  const [email, setEmail] = useState("buyer@11st.test");
  const [password, setPassword] = useState("P@ssw0rd!");

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    try {
      if (mode === "join") {
        await registerMember(name, email, password);
        setNotice("회원가입 완료. 이제 로그인할 수 있습니다.");
        setMode("login");
        return;
      }
      const result = await login(email, password);
      setNotice(`로그인 성공. Access Token ${result.expiresIn}초 유효.`);
      onAuthenticated();
    } catch (error) {
      setNotice((error as Error).message);
    }
  }

  return (
    <section className="authPanel">
      <div className="segmented">
        <button className={mode === "login" ? "active" : ""} onClick={() => setMode("login")}>로그인</button>
        <button className={mode === "join" ? "active" : ""} onClick={() => setMode("join")}>회원가입</button>
      </div>
      <form onSubmit={submit}>
        {mode === "join" && <label>이름<input value={name} onChange={(event) => setName(event.target.value)} /></label>}
        <label>이메일<input value={email} onChange={(event) => setEmail(event.target.value)} /></label>
        <label>비밀번호<input type="password" value={password} onChange={(event) => setPassword(event.target.value)} /></label>
        <button className="primary full" type="submit">{mode === "login" ? "로그인" : "회원가입"}</button>
      </form>
      <p>{notice}</p>
    </section>
  );
}

function ProductDetailPanel({
  product,
  onClose,
  onChat
}: {
  product: ProductDetail;
  onClose: () => void;
  onChat: () => void;
}) {
  const sellerLabel = formatProductSeller(product);
  const sellerTarget = formatProductSellerTarget(product);

  return (
    <section className="detailPanel">
      <div>
        <span className="badge">{product.saleStatus}</span>
        <h2>{product.name}</h2>
        <p>상품 #{product.id} · 판매자 {sellerLabel} · 카테고리 #{product.categoryId}</p>
        <strong>{money.format(Number(product.price))}원</strong>
        <p>재고 {money.format(product.stockQuantity)}개</p>
      </div>
      <div className="heroActions">
        <button className="secondary" onClick={onClose}>닫기</button>
        <button className="primary" onClick={onChat}><MessageCircle size={18} /> {sellerTarget}에게 문의</button>
      </div>
    </section>
  );
}

function ProductGrid({
  title,
  products,
  note,
  onInspect,
  onChat
}: {
  title: string;
  products: Product[];
  note?: string;
  onInspect: (productId: number) => void;
  onChat: (productId: number) => void;
}) {
  return (
    <section>
      <div className="sectionTitle">
        <h2>{title}</h2>
        {note && <p>{note}</p>}
      </div>
      <div className="productGrid">
        {products.length === 0 && <p className="empty">DB에 조회된 상품이 없습니다.</p>}
        {products.map((product) => (
          <article className={product.saleStatus === "SOLD_OUT" ? "productCard soldout" : "productCard"} key={product.id}>
            <div className="imageBox"><PackageSearch size={48} /></div>
            <div className="productBody">
              <span className="badge">{product.saleStatus === "SOLD_OUT" ? "품절" : "판매중"}</span>
              <h3>{product.name}</h3>
              <strong>{money.format(Number(product.price))}원</strong>
              <button className="iconText" onClick={() => onInspect(product.id)}>
                <PackageSearch size={16} /> 상세
              </button>
              <button className="iconText secondaryAction" onClick={() => onChat(product.id)}>
                <MessageCircle size={16} /> 문의
              </button>
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}

createRoot(document.getElementById("root")!).render(<App />);

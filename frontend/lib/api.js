import axios from "axios";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

const api = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true,
  headers: {
    "Content-Type": "application/json",
  },
});

// Request interceptor — attach JWT token
api.interceptors.request.use(
  (config) => {
    if (typeof window !== "undefined") {
      const token = localStorage.getItem("token");
      if (token && token.split(".").length === 3) {
        config.headers.Authorization = `Bearer ${token}`;
      } else if (token) {
        localStorage.removeItem("token");
      }
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor — handle 401 (expired/invalid token)
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const isCurrentUserCheck = error.config?.url?.startsWith("/user/me");
    const isAuthFailure =
      error.response?.status === 401 ||
      error.response?.status === 403 ||
      (error.response?.status === 404 && isCurrentUserCheck);

    if (isAuthFailure && typeof window !== "undefined") {
      localStorage.removeItem("token");
      localStorage.removeItem("authMode");
      // Only redirect if not already on auth pages
      if (!window.location.pathname.startsWith("/auth")) {
        window.location.href = "/auth";
      }
    }
    return Promise.reject(error);
  }
);

// ==================== Auth ====================
export const authAPI = {
  login: (userName, password) =>
    api.post("/public/login", { userName, password }),
  signup: (userName, password, email, sentimentAnalysis = false) =>
    api.post("/public/signup", { userName, password, email, sentimentAnalysis }),
  clearOAuthCookie: () => api.post("/public/logout-cookie"),
  exchangeOAuthCode: (code) => api.post(`/public/oauth/exchange?code=${encodeURIComponent(code)}`),
  googleOAuthUrl: () => `${API_BASE_URL}/oauth2/authorization/google`,
};

// ==================== Journals ====================
export const journalAPI = {
  getAll: () => api.get("/journals"),
  getById: (id) => api.get(`/journals/id/${id}`),
  create: (data) => api.post("/journals", data),
  update: (id, data) => api.put(`/journals/id/${id}`, data),
  delete: (id) => api.delete(`/journals/id/${id}`),
  assignCollection: (entryId, collectionId) =>
    api.put(`/journals/id/${entryId}/collection/${collectionId}`),
  removeCollection: (entryId) =>
    api.put(`/journals/id/${entryId}/collection`),
};

// ==================== Collections ====================
export const collectionAPI = {
  getAll: () => api.get("/collections"),
  getById: (id) => api.get(`/collections/id/${id}`),
  create: (data) => api.post("/collections", data),
  update: (id, data) => api.put(`/collections/id/${id}`, data),
  delete: (id) => api.delete(`/collections/id/${id}`),
  getEntries: (id) => api.get(`/collections/id/${id}/entries`),
};

// ==================== User ====================
export const userAPI = {
  getMe: () => api.get("/user/me"),
  update: (data) => api.put("/user", data),
  updateUsername: (userName) => api.put("/user/username", { userName }),
  delete: () => api.delete("/user"),
  toggleSentimentAnalysis: (enabled) =>
    api.put("/user/sentiment-analysis", { enabled }),
  getReports: () => api.get("/user/reports"),
  getReportById: (id) => api.get(`/user/reports/${id}`),
  getAnalytics: (range = "15d") =>
    api.get(`/user/analytics?range=${range}`),
};

export default api;

import { useCookies } from "@vueuse/integrations/useCookies";

const TokenKey = 'admin-token';

export function getToken() {
  try {
    const val = localStorage.getItem(TokenKey);
    // 防止有人手动存了字符串 "null"
    return val === 'null' ? null : val;
  } catch {
    return null;
  }
}

export function setToken(token) {
  localStorage.setItem(TokenKey, token);
}

export function removeToken() {
  localStorage.removeItem(TokenKey);
}
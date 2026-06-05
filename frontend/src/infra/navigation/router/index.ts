import { createRouter, createWebHistory } from 'vue-router';
import type { RouteRecordRaw } from 'vue-router';

export function setupRouter(routes: RouteRecordRaw[]) {
  return createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes
  });
}

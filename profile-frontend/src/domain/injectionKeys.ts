import type { InjectionKey } from 'vue';

// Define strict keys to provide/inject dependencies
// We don't import Pinia stores directly here, just their interface types if we want to be strict.
// For now, we will use 'any' or their return types if we wanted to extract interfaces.
export const deviceStoreKey = Symbol('deviceStore') as InjectionKey<any>;
export const authStoreKey = Symbol('authStore') as InjectionKey<any>;
export const templateStoreKey = Symbol('templateStore') as InjectionKey<any>;

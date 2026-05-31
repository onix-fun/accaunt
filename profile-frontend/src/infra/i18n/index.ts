import { createI18n } from 'vue-i18n';
import ru from '@/assets/locales/ru.json';

const messages = {
  ru
};

export const i18n = createI18n({
  legacy: false,
  locale: 'ru',
  fallbackLocale: 'ru',
  messages,
});

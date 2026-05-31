import { createApp } from "vue";
import { createPinia } from "pinia";
import App from "./App.vue";
import { setupRouter } from "./infra/navigation/router";

import PrimeVue from "primevue/config";
import { definePreset } from "@primeuix/themes";
import Aura from "@primeuix/themes/aura";
import "primeicons/primeicons.css";
import "@fontsource/roboto/400.css";
import "@fontsource/roboto/500.css";
import "@fontsource/roboto/700.css";
import "./style.css";

const SparrowTheme = definePreset(Aura, {
  semantic: {
    primary: {
      50: "{slate.50}",
      100: "{slate.100}",
      200: "{slate.200}",
      300: "{slate.300}",
      400: "{slate.400}",
      500: "{slate.500}",
      600: "{slate.600}",
      700: "{slate.700}",
      800: "{slate.800}",
      900: "{slate.900}",
      950: "{slate.950}",
    },
  },
});

const router = setupRouter([
  {
    path: "/",
    name: "Settings",
    component: () => import("@/views/Settings.vue"),
  },
]);

const app = createApp(App);

app.use(createPinia());
app.use(router);
app.use(PrimeVue, {
  theme: {
    preset: SparrowTheme,
    options: {
      darkModeSelector: ".dark",
    },
  },
});

app.mount("#app");

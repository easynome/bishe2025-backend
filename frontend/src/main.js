import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './style.css'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import App from './App.vue'
import router from './router'
import store from "~/store";
import * as ElementPlusIconsVue from '@element-plus/icons-vue'


const app = createApp(App)
app.use(store)
app.use(router)
app.use(ElementPlus, {
  locale: zhCn,
})
app.use(ElementPlus)


for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

import 'virtual:windi.css'
import 'nprogress/nprogress.css'
import './permission'
app.mount('#app')

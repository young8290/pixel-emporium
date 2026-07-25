import './assets/main.css'

import {createApp} from 'vue'
import {createPinia} from 'pinia'
import App from './App.vue'
import {router} from './router'

const pinia = createPinia()
const app = createApp(App)

app.use(pinia)
app.use(router)
app.config.errorHandler=(error,instance,info)=>console.error('[render-error]',info,error)
app.mount('#app')

import router from "~/router/index.js";
import {getToken} from "~/composables/auth.js";
import {hideFullLoading, showFullLoading, toast} from "~/composables/util.js";
import store from "~/store/index.js";


const LOGIN_PATH = '/login'
const DEFAULT_PATH = '/'


// 全局前置守卫

router.beforeEach(async( to, from, next) => {
    //显示loading
    showFullLoading()

    const token =getToken()

    //有token 防止重复登录
    if (token && to.path === LOGIN_PATH) {
        toast('请勿重复登录',"error")
        const redirectPath = from.path && from.path !== LOGIN_PATH ? from.path : DEFAULT_PATH;
        return next({path:redirectPath})
    }
    // 无token 判断用户是否登录,没有登录跳转到登录页面
    if (!token && to.path !== LOGIN_PATH && to.path !=='/register') {
        toast('请先登录',"error")
        return next({path:'/login'})
    }
    //只在路由切换时，且存在 token 的情况下才会执行 getUserInfo
    if(token && to.path !== LOGIN_PATH && to.path !=='/register'){
        await store.dispatch('getUserInfo')
    }
    next()

    //设置页面标题
    document.title = (to.meta.title ? to.meta.title :"") +'-个性化学习推荐系统后台'
})

router.afterEach((to, from) => {
    hideFullLoading()
})
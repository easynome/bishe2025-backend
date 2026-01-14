import {ArrowDown} from "@element-plus/icons-vue";
import {ref} from "vue";
import {onBeforeRouteUpdate, useRoute, useRouter} from "vue-router";
import {useCookies} from "@vueuse/integrations/useCookies";

export function useTabList() {
    const route = useRoute()
    const cookie = useCookies()
    const router = useRouter()
    const activeTabs = ref(route.path);
    const tabList = ref([
        {
            title: '后台首页',
            path: '/'
        },
        {
            title: '课程列表',
            path: '/course/list'
        },
        {
            title: '推荐课程',
            path: '/study/recommend'
        },

    ])

//添加标签导航
    function addTab(tab) {
        let noTab = tabList.value.findIndex(t => t.path === tab.path) === -1
        if (noTab) {
            tabList.value.push(tab)
        }
        cookie.set('tagList', tabList.value)
    }

//初始化标签导航列表
    function initTabList() {
        let tbs = cookie.get('tagList')
        if (tbs &&Array.isArray(tbs)) {
            tabList.value = tbs
        }else{
            cookie.set('tagList', tabList.value)
        }
    }

    initTabList()

    onBeforeRouteUpdate((to, from) => {
        activeTabs.value = to.path
        addTab({
            title: to.meta.title,
            path: to.path
        })
    })
    const changeTab = (t) => {
        activeTabs.value = t
        router.push(t)
    }
    const removeTab = (targetPath) => {
        let tabs = tabList.value
        let newActivePath = activeTabs.value
        if (newActivePath === targetPath) {
            tabs.forEach((tab, index) => {
                if (tab.path === targetPath) {
                    const nextTab = tabs[index + 1] || tabs[index - 1]
                    if (nextTab) {
                        newActivePath = nextTab.path
                    }
                }
            })
        }
        activeTabs.value = newActivePath
        tabList.value = tabList.value.filter(tab => tab.path !== targetPath)
        cookie.set('tagList', tabList.value)
        // 手动触发路由跳转
        if (newActivePath !== route.path) {
            router.push(newActivePath)
        }
    }
    const handleClose = (command) => {
        if (command === 'clearAll') {
            //切换回首页
            activeTabs.value = '/'
            //过滤只剩下首页
            tabList.value = [{
                title: '后台首页',
                path: '/'
            }]
        } else if (command === 'closeOther') {
            //过滤只剩下首页和当前激活
            tabList.value = tabList.value.filter(tab => tab.path === activeTabs.value || tab.path === '/')
        }
        cookie.set('tagList', tabList.value)
    }
    return{
        ArrowDown,
        tabList,
        activeTabs,
        changeTab,
        removeTab,
        handleClose
    }
}

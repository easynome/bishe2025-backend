import {reactive, ref} from "vue";
import {useRouter} from "vue-router";
import {useStore} from "vuex";
import {showModel, toast} from "~/composables/util.js";
import {logout, updatePassword} from "~/api/manager.js";
import store from "~/store/index.js";
import router from "~/router/index.js";

export function useRePassword(){
    const router = useRouter()
    const store = useStore()
    const formDrawerRef = ref(null)
    const form = reactive({
        oldpassword:'',
        password: '',
        repassword: ''
    })
    const openRePasswordForm=()=> {
        formDrawerRef.value.open()
    }
    const ruleFormRef = ref(null)
    const handleLogin=()=> {
        ruleFormRef.value?.validate((valid)=>
        {
            if (!valid) return false
            formDrawerRef.value.showLoading()
            updatePassword(form).then(res => {
                if (res.data.code === 200) {
                    toast('修改密码成功')
                    // formDrawerRef.value.close()
                    store.dispatch('logout')
                    router.replace('/login')
                } else {
                    toast(res.data.msg, 'error')
                }
            }).finally(() => {
                formDrawerRef.value.hideLoading()
            })
        })
    }
    return {
        form,
        formDrawerRef,
        ruleFormRef,
        handleLogin,
        openRePasswordForm
    }
}
export function useLogout( ){
    const router = useRouter()
    const store = useStore()
    /**
     * location.reload():
     * 性能开销较大，需要重新建立连接和下载资源
     * 需要完全重置应用状态时
     * 解决深层次的前端状态问题
     * router.go(0):
     * 性能开销较小，只重新渲染 Vue 组
     * 轻量级刷新需求
     * 保持 SPA 特性的场景
     */
// 退出登录
    function handeLogout() {
        showModel().then(() => {
            logout().then(()=>{
                store.dispatch('logout')
                toast('退出成功');
                router.replace('/login')
            }).catch(() => {
                // 即使API调用失败也清除本地状态
                store.dispatch('logout');
                toast('退出成功');
                router.replace('/login');
            });
        });
    }
    return {
        handeLogout
    }
}

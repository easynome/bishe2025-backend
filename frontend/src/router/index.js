import {
    createRouter,
    createWebHistory
} from 'vue-router'

import Index from '~/pages/index.vue'
import About from '~/pages/about.vue'
import NotFound from '~/pages/404.vue'
import Login from '~/pages/login.vue'
import Admin from '~/layouts/admin.vue'
import CourseList from '~/pages/course/list.vue'
import Recommend from "~/pages/study/recommend.vue";
import Register from "~/pages/register.vue";



const routes = [{
    path: '/',
    component:Admin,
    // 子路由
    children:[{
        path: '/',
        component:Index,
        meta: {
            title: '后台首页'
        }
    },{
        path: '/course/list',
        component:CourseList,
        meta: {
            title: '课程列表'
        }
    },{
        path: '/course/detail/:id',

        component: () => import('~/pages/course/detail.vue'),
        meta: { title: '课程详情' }
    },{
        path: '/study/recommend',
        name: 'Recommend',
        component: Recommend,
        meta: { title: '推荐课程' }
    },{
        path: '/study/my-learning',
        name: 'MyLearning',
        component: () => import('~/pages/study/my-learning.vue'),
        meta: { title: '我的学习' }
    },{
        path: '/teach/publish-course',
        name: 'PublishCourse',
        component: () => import('~/pages/teach/publish-course.vue'),
        meta: { title: '发布课程' }
    },{
        path: '/teach/my-course',
        name: 'MyCourse',
        component: () => import('~/pages/teach/my-course.vue'),
        meta: { title: '我的课程' }
    },{
        path: '/admin/users/list',
        name: 'UserManagement',
        component: () => import('~/pages/operation/user.vue'),
        meta: { title: '用户管理' }
    },{
        path: '/admin/courses/list',
        name: 'CourseManagement',
        component: () => import('~/pages/operation/course.vue'),
        meta: { requiresAuth:true, requiresAdmin:true, title: '课程管理' }
    }]
},{
    path: '/about',
    component:About
},{
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: NotFound,
},{
    path: '/login',
    component:Login,
    meta: {
        title: '登录页'
    }
},{
    path: '/register',
    name: 'Register',
    component: Register,
    meta: { title: '注册' }
}
]


const router = createRouter({
    history: createWebHistory(),
    routes,
})

export default router
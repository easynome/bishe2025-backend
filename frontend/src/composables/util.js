import {ElNotification,ElMessageBox} from 'element-plus'
import nprogress from 'nprogress'
//消息提示,类型默认为“success”
export function toast(msg,type="success",dangerouslyUseHTMLString=false){
  ElNotification({
    message:msg,
    type,
    duration: 2000,
    dangerouslyUseHTMLString,
  })
}
//退出
export function showModel(){
  return ElMessageBox.confirm('此操作将退出登录, 是否继续?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  });
}
//显示全屏loading
export function showFullLoading(){
  nprogress.start ()
}
//隐藏全屏loading
export function hideFullLoading(){
  nprogress.done ()
}
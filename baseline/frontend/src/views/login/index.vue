<template>
  <div class="login-wrapper">
    <div class="login-card">
      <div class="login-header">
        <h1>通用物料管理系统</h1>
        <p class="subtitle">Generic Material Management System</p>
      </div>

      <el-form :model="form" :rules="rules" ref="formRef" label-position="top" size="large">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" @keyup.enter="handleLogin">
            <template #prefix><span class="input-icon">&#xe605;</span></template>
          </el-input>
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" @keyup.enter="handleLogin" />
        </el-form-item>

        <el-form-item label="验证码" prop="captchaCode">
          <div class="captcha-row">
            <el-input v-model="form.captchaCode" placeholder="请输入验证码" @keyup.enter="handleLogin" />
            <img :src="captchaImage" class="captcha-img" @click="refreshCaptcha" title="点击刷新验证码" />
          </div>
        </el-form-item>

        <el-form-item>
          <el-checkbox v-model="form.rememberMe" @change="onRememberChange">记住密码</el-checkbox>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="loading" style="width:100%;height:42px" @click="handleLogin">
            登 录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="login-extra">
        <el-divider><span style="color:#999;font-size:12px">其他方式</span></el-divider>
        <div class="oauth-btns">
          <el-button circle size="large" title="钉钉登录" @click="handleDingTalkLogin">
            <span style="font-weight:bold">钉</span>
          </el-button>
        </div>
        <div class="register-link">
          还没有账号？<el-link type="primary" @click="openRegister">立即注册</el-link>
        </div>
      </div>
    </div>

    <!-- 注册弹窗 -->
    <el-dialog v-model="showRegister" title="用户注册" width="520px" :close-on-click-modal="false">
      <el-form :model="regForm" :rules="regRules" ref="regFormRef" label-width="90px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="regForm.username" placeholder="登录用的用户名，全局唯一" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="regForm.password" type="password" show-password placeholder="8~20位，含大小写字母、数字、特殊符号至少3类" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPwd">
          <el-input v-model="regForm.confirmPwd" type="password" show-password placeholder="再次输入密码" />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="regForm.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="regForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="所属部门" prop="dept">
          <el-input v-model="regForm.dept" placeholder="如：硬件部、软件部" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRegister = false">取消</el-button>
        <el-button type="primary" :loading="regLoading" @click="handleRegister">注册</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login, register, getCaptcha, getDingTalkAuthUrl, dingtalkLogin } from '@/api/user'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const showRegister = ref(false)
const regLoading = ref(false)
const formRef = ref(null)
const regFormRef = ref(null)

const form = reactive({ username: '', password: '', captchaCode: '', rememberMe: false })
const captchaImage = ref('')
const captchaKey = ref('')

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

const regForm = reactive({ username: '', password: '', confirmPwd: '', realName: '', phone: '', dept: '' })
const pwdComplexMsg = '密码需包含大写字母、小写字母、数字、特殊符号中的至少3类'
const pwdComplexValidator = (rule, value, callback) => {
  if (!value) return callback(new Error('请输入密码'))
  if (value.length < 8 || value.length > 20) return callback(new Error('密码长度须为8~20位'))
  let kinds = 0
  if (/[A-Z]/.test(value)) kinds++
  if (/[a-z]/.test(value)) kinds++
  if (/[0-9]/.test(value)) kinds++
  if (/[^a-zA-Z0-9]/.test(value)) kinds++
  if (kinds < 3) return callback(new Error(pwdComplexMsg))
  callback()
}
const regRules = {
  username: [{ required: true, message: '请输入用户名' }],
  password: [
    { required: true, validator: pwdComplexValidator, trigger: 'blur' }
  ],
  confirmPwd: [
    { required: true, message: '请确认密码' },
    { validator: (rule, value, callback) => value !== regForm.password ? callback(new Error('两次密码不一致')) : callback(), trigger: 'blur' }
  ],
  realName: [{ required: true, message: '请输入真实姓名' }],
  phone: [
    { required: true, message: '请输入手机号' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ]
}

const refreshCaptcha = async () => {
  try {
    const res = await getCaptcha()
    if (res && res.data) {
      captchaKey.value = res.data.captchaKey
      captchaImage.value = res.data.captchaImage
    }
  } catch { /* ignore */ }
}

const handleLogin = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const res = await login({
      username: form.username,
      password: form.password,
      captchaKey: captchaKey.value,
      captchaCode: form.captchaCode
    })
    if (res.code === 200) {
      localStorage.setItem('token', res.data.token)
      localStorage.setItem('username', res.data.username)
      localStorage.setItem('realName', res.data.realName || res.data.username)
      localStorage.setItem('role', res.data.role)
      if (form.rememberMe) {
        localStorage.setItem('savedUsername', form.username)
      } else {
        localStorage.removeItem('savedUsername')
      }
      ElMessage.success('登录成功')
      router.push('/inbound')
    } else {
      ElMessage.error(res.msg || '登录失败')
      refreshCaptcha()
    }
  } catch {
    ElMessage.error('登录请求失败')
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}

const openRegister = () => {
  Object.assign(regForm, { username: '', password: '', confirmPwd: '', realName: '', phone: '', dept: '' })
  showRegister.value = true
}

const handleRegister = async () => {
  const valid = await regFormRef.value.validate().catch(() => false)
  if (!valid) return
  regLoading.value = true
  try {
    const res = await register(regForm)
    if (res.code === 200) {
      ElMessage.success('注册成功，请登录')
      showRegister.value = false
    } else {
      ElMessage.error(res.msg || '注册失败')
    }
  } catch {
    ElMessage.error('注册请求失败')
  } finally {
    regLoading.value = false
  }
}

const onRememberChange = (val) => {
  if (!val) localStorage.removeItem('savedUsername')
}

const dingtalkLoading = ref(false)

const handleDingTalkLogin = async () => {
  dingtalkLoading.value = true
  try {
    const redirectUri = window.location.origin + '/login'
    const res = await getDingTalkAuthUrl(redirectUri)
    if (res.code === 200 && res.data.authUrl) {
      window.location.href = res.data.authUrl
    } else {
      ElMessage.error('获取钉钉授权链接失败')
    }
  } catch {
    ElMessage.error('请求失败')
  } finally {
    dingtalkLoading.value = false
  }
}

const handleDingTalkCallback = async (code, state) => {
  loading.value = true
  try {
    const res = await dingtalkLogin({ code, state })
    if (res.code === 200) {
      localStorage.setItem('token', res.data.token)
      localStorage.setItem('username', res.data.username)
      localStorage.setItem('realName', res.data.realName || res.data.username)
      localStorage.setItem('role', res.data.role)
      ElMessage.success('钉钉登录成功')
      router.replace('/inbound')
    } else {
      ElMessage.error(res.msg || '钉钉登录失败')
      router.replace('/login')
    }
  } catch {
    ElMessage.error('钉钉登录请求失败')
    router.replace('/login')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  const code = route.query.code
  const state = route.query.state
  if (code && state) {
    handleDingTalkCallback(code, state)
    return
  }

  const savedUsername = localStorage.getItem('savedUsername')
  if (savedUsername) {
    form.username = savedUsername
    form.rememberMe = true
  }
  refreshCaptcha()
})
</script>

<style scoped>
.login-wrapper {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0c1d36 0%, #1a3a5c 40%, #1f4d7a 100%);
  padding: 20px;
}
.login-card {
  width: 420px;
  padding: 40px 36px 28px;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.3);
}
.login-header {
  text-align: center;
  margin-bottom: 32px;
}
.login-header h1 {
  font-size: 22px;
  color: #1a3a5c;
  margin: 0 0 6px;
  font-weight: 700;
}
.login-header .subtitle {
  font-size: 11px;
  color: #999;
  margin: 0;
  letter-spacing: 1px;
}
.captcha-row {
  display: flex;
  gap: 12px;
}
.captcha-img {
  width: 110px;
  height: 40px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  cursor: pointer;
  flex-shrink: 0;
}
.login-extra {
  text-align: center;
  margin-top: 8px;
}
.oauth-btns {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-bottom: 16px;
}
.oauth-btns .el-button {
  width: 44px;
  height: 44px;
  font-size: 18px;
  background: #f0f8ff;
  border: 1px solid #d0ddee;
  color: #1890ff;
}
.register-link {
  font-size: 13px;
  color: #999;
}
</style>

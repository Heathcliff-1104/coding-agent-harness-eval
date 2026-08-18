import { userApi } from './index'

export const getCaptcha = userApi.captcha
export const login = userApi.login
export const register = userApi.register
export const getUserInfo = userApi.info
export const checkUsername = userApi.checkUsername
export const checkPhone = userApi.checkPhone
export const getDingTalkAuthUrl = userApi.dingtalkAuthUrl
export const dingtalkLogin = userApi.dingtalkLogin
